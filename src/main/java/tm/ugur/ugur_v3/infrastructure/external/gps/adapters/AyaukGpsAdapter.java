package tm.ugur.ugur_v3.infrastructure.external.gps.adapters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider;
import tm.ugur.ugur_v3.infrastructure.external.gps.config.AyaukGpsProperties;
import tm.ugur.ugur_v3.infrastructure.external.gps.dto.AyaukGpsDataDto;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class AyaukGpsAdapter implements GpsDataProvider {

    private final @Qualifier("ayaukWebClient") WebClient webClient;
    private final AyaukGpsProperties properties;


    private final Map<String, AyaukGpsDataDto> routeAssignmentCache = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastCacheUpdate = new AtomicReference<>(Instant.now());


    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicReference<ProviderHealthStatus> healthStatus =
            new AtomicReference<>(createInitialHealthStatus());

    @Override
    public Flux<GpsLocationData> getVehicleLocations() {
        log.debug("Fetching vehicle-route assignments from AYAUK API");

        return fetchRouteAssignments()
                .filter(this::isValidAssignment)
                .map(this::convertToGpsLocationData)
                .doOnNext(this::cacheAssignment)
                .doOnComplete(this::updateCacheTimestamp)
                .doOnError(this::updateErrorMetrics)
                .retryWhen(createRetrySpec())
                .onErrorResume(this::handleFetchError);
    }

    @Override
    public String getProviderName() {
        return "AYAUK_ROUTES";
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return Mono.fromSupplier(() -> {
            ProviderHealthStatus status = healthStatus.get();
            return status.isHealthy() &&
                    !isCacheStale() &&
                    getSuccessRate() > 0.7;
        });
    }

    @Override
    public Mono<ProviderHealthStatus> getHealthStatus() {
        return Mono.fromSupplier(() -> {
            updateHealthStatus();
            return healthStatus.get();
        });
    }

    public Mono<AyaukGpsDataDto> getRouteAssignment(String vehicleId) {
        if (vehicleId == null || vehicleId.trim().isEmpty()) {
            return Mono.empty();
        }

        String normalizedId = vehicleId.trim().toUpperCase();
        AyaukGpsDataDto cached = routeAssignmentCache.get(normalizedId);

        if (cached != null && cached.isRecentAssignment()) {
            return Mono.just(cached);
        }


        return refreshRouteAssignments()
                .then(Mono.fromSupplier(() -> routeAssignmentCache.get(normalizedId)))
                .cast(AyaukGpsDataDto.class);
    }

    public Flux<AyaukGpsDataDto> getVehiclesOnRoute(String routeNumber) {
        if (routeNumber == null || routeNumber.trim().isEmpty()) {
            return Flux.empty();
        }

        return Flux.fromIterable(routeAssignmentCache.values())
                .filter(assignment -> routeNumber.equals(assignment.getRouteNumber()))
                .filter(AyaukGpsDataDto::isForToday);
    }

    public Flux<AyaukGpsDataDto> getTodaysAssignments() {
        return Flux.fromIterable(routeAssignmentCache.values())
                .filter(AyaukGpsDataDto::isForToday)
                .filter(AyaukGpsDataDto::isOnActiveShift);
    }

    public Mono<Void> refreshRouteAssignments() {
        return fetchRouteAssignments()
                .doOnNext(this::cacheAssignment)
                .doOnComplete(this::updateCacheTimestamp)
                .then();
    }

    private Flux<AyaukGpsDataDto> fetchRouteAssignments() {
        return webClient.get()
                .uri("/buses/info")
                .retrieve()
                .bodyToFlux(AyaukGpsDataDto.class)
                .doOnSubscribe(sub -> {
                    totalRequests.incrementAndGet();
                    log.debug("Starting route assignment fetch from AYAUK API");
                })
                .doOnNext(assignment -> {
                    successfulRequests.incrementAndGet();
                    log.debug("Received route assignment: {} -> Route {}",
                            assignment.getNormalizedCarNumber(), assignment.getRouteNumber());
                })
                .doOnComplete(() -> log.debug("Completed route assignment fetch from AYAUK API"))
                .timeout(properties.getTimeout())
                .onErrorMap(this::mapException);
    }

    private boolean isValidAssignment(AyaukGpsDataDto assignment) {
        if (!assignment.isValid()) {
            log.debug("Invalid assignment data: {}", assignment);
            return false;
        }


        if (!assignment.isRecentAssignment()) {
            log.debug("Assignment too old: {} from {}",
                    assignment.getNormalizedCarNumber(), assignment.date());
            return false;
        }

        return true;
    }

    private GpsLocationData convertToGpsLocationData(AyaukGpsDataDto assignment) {
        return new GpsLocationData(
                assignment.getNormalizedCarNumber(),
                0.0,
                0.0,
                0.0,
                null,
                null,
                Instant.now(),
                createRouteMetadata(assignment)
        );
    }

    private Map<String, Object> createRouteMetadata(AyaukGpsDataDto assignment) {
        return Map.of(
                "provider", "AYAUK",
                "dataType", "ROUTE_ASSIGNMENT",
                "routeNumber", Objects.requireNonNull(assignment.getRouteNumber()),
                "assignmentDate", assignment.date(),
                "shift", assignment.getShift().name(),
                "shiftNumber", assignment.change(),
                "isToday", assignment.isForToday(),
                "hasGpsCoordinates", false,
                "assignmentKey", assignment.getAssignmentKey()
        );
    }

    private void cacheAssignment(AyaukGpsDataDto assignment) {
        if (assignment.isValid()) {
            routeAssignmentCache.put(assignment.getNormalizedCarNumber(), assignment);
        }
    }

    private void updateCacheTimestamp() {
        lastCacheUpdate.set(Instant.now());
        log.debug("Updated route assignment cache with {} entries", routeAssignmentCache.size());
    }

    private boolean isCacheStale() {
        Instant lastUpdate = lastCacheUpdate.get();
        Duration maxAge = properties.getCacheTtl();
        return lastUpdate.isBefore(Instant.now().minus(maxAge));
    }

    private Retry createRetrySpec() {
        return Retry.backoff(properties.getMaxRetries(), properties.getRetryDelay())
                .maxBackoff(Duration.ofSeconds(30))
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying AYAUK API call (attempt {}/{}): {}",
                            retrySignal.totalRetries() + 1,
                            properties.getMaxRetries(),
                            retrySignal.failure().getMessage());
                });
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            int statusCode = webEx.getStatusCode().value();
            return statusCode >= 500 || statusCode == 401 || statusCode == 403;
        }

        return throwable instanceof java.net.SocketTimeoutException ||
                throwable instanceof java.net.ConnectException ||
                throwable instanceof java.util.concurrent.TimeoutException;
    }

    private Flux<GpsLocationData> handleFetchError(Throwable error) {
        log.error("Failed to fetch route assignments from AYAUK API", error);
        updateErrorMetrics(error);


        if (!routeAssignmentCache.isEmpty()) {
            log.warn("Using cached route assignments due to API error");
            return Flux.fromIterable(routeAssignmentCache.values())
                    .filter(AyaukGpsDataDto::isRecentAssignment)
                    .map(this::convertToGpsLocationData);
        }

        return Flux.empty();
    }

    private Throwable mapException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            return new AyaukGpsException(
                    "AYAUK API error: " + webEx.getStatusCode() + " - " + webEx.getResponseBodyAsString(),
                    throwable
            );
        }

        return new AyaukGpsException("AYAUK API error: " + throwable.getMessage(), throwable);
    }

    private void updateErrorMetrics(Throwable error) {
        failedRequests.incrementAndGet();
        log.warn("Route assignment fetch failed: {}", error.getMessage());
    }

    private void updateHealthStatus() {
        boolean isHealthy = determineHealthStatus();

        ProviderHealthStatus newStatus = new ProviderHealthStatus(
                getProviderName(),
                isHealthy,
                isHealthy ? "HEALTHY" : "UNHEALTHY",
                properties.getTimeout().dividedBy(2),
                (int) successfulRequests.get(),
                (int) failedRequests.get(),
                Instant.now()
        );

        healthStatus.set(newStatus);
    }

    private boolean determineHealthStatus() {
        return getSuccessRate() > 0.7 && !isCacheStale();
    }

    private double getSuccessRate() {
        long total = totalRequests.get();
        long successful = successfulRequests.get();
        return total == 0 ? 1.0 : (double) successful / total;
    }

    private ProviderHealthStatus createInitialHealthStatus() {
        return new ProviderHealthStatus(
                getProviderName(),
                true,
                "INITIALIZING",
                Duration.ZERO,
                0,
                0,
                Instant.now()
        );
    }

    public static class AyaukGpsException extends RuntimeException {
        public AyaukGpsException(String message) {
            super(message);
        }

        public AyaukGpsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}