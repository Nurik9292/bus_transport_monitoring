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
import tm.ugur.ugur_v3.infrastructure.external.gps.config.TugdkGpsProperties;
import tm.ugur.ugur_v3.infrastructure.external.gps.dto.TugdkGpsDataDto;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class TugdkGpsAdapter implements GpsDataProvider {

    private final @Qualifier("tugdkWebClient") WebClient webClient;
    private final TugdkGpsProperties properties;

    private final AtomicReference<ProviderHealthStatus> healthStatus =
            new AtomicReference<>(createInitialHealthStatus());
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicReference<Instant> lastSuccessfulCall =
            new AtomicReference<>(Instant.now());
    private final AtomicReference<Instant> lastHealthCheck =
            new AtomicReference<>(Instant.now());

    @Override
    public Flux<GpsLocationData> getVehicleLocations() {
        log.debug("Fetching vehicle locations from TUGDK API");

        return fetchGpsData()
                .doOnNext(this::updateSuccessMetrics)
                .doOnError(this::updateErrorMetrics)
                .retryWhen(createRetrySpec())
                .onErrorResume(this::handleFetchError);
    }

    @Override
    public String getProviderName() {
        return "TUGDK_GPS";
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return Mono.fromSupplier(() -> {
            ProviderHealthStatus status = healthStatus.get();
            return status.isHealthy() &&
                    !isLastCallTooOld() &&
                    getSuccessRate() > 0.5; // 50% success rate threshold
        });
    }

    @Override
    public Mono<ProviderHealthStatus> getHealthStatus() {
        return Mono.fromSupplier(() -> {
            updateHealthStatus();
            return healthStatus.get();
        });
    }

    private Flux<GpsLocationData> fetchGpsData() {
        return webClient.get()
                .uri("/positions")
                .retrieve()
                .bodyToFlux(TugdkGpsDataDto.class)
                .filter(this::isValidGpsData)
                .map(this::convertToGpsLocationData)
                .doOnSubscribe(sub -> {
                    totalRequests.incrementAndGet();
                    log.debug("Starting GPS data fetch from TUGDK API");
                })
                .doOnComplete(() -> log.debug("Completed GPS data fetch from TUGDK API"))
                .timeout(properties.getTimeout())
                .onErrorMap(this::mapException);
    }

    private boolean isValidGpsData(TugdkGpsDataDto data) {
        if (data == null || data.attributes() == null) {
            log.warn("Received null GPS data from TUGDK API");
            return false;
        }

        if (!data.isValidGpsData()) {
            log.debug("Invalid GPS data for vehicle {}: outdated={}, valid={}, sat={}",
                    data.getVehicleIdentifier(), data.outdated(), data.valid(), data.attributes().sat());
            return false;
        }

        if (data.latitude() == 0.0 && data.longitude() == 0.0) {
            log.debug("Invalid coordinates (0,0) for vehicle {}", data.getVehicleIdentifier());
            return false;
        }

        if (!data.isRecentFix()) {
            log.debug("GPS data too old for vehicle {}: fixTime={}",
                    data.getVehicleIdentifier(), data.fixTime());
            return false;
        }

        return true;
    }

    private GpsLocationData convertToGpsLocationData(TugdkGpsDataDto data) {
        return new GpsLocationData(
                data.getVehicleIdentifier(),
                data.latitude(),
                data.longitude(),
                data.accuracy(),
                data.speed() > 0 ? data.speed() : null,
                data.course() > 0 ? data.course() : null,
                parseTimestamp(data.fixTime()),
                createMetadata(data)
        );
    }

    private Instant parseTimestamp(String timeString) {
        try {
            return Instant.parse(timeString);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}", timeString, e);
            return Instant.now();
        }
    }

    private Map<String, Object> createMetadata(TugdkGpsDataDto data) {
        return Map.ofEntries(
                Map.entry("provider", "TUGDK"),
                Map.entry("deviceId", data.deviceId()),
                Map.entry("protocol", data.protocol()),
                Map.entry("satellites", data.attributes().sat()),
                Map.entry("hdop", data.attributes().hdop()),
                Map.entry("signalQuality", data.attributes().getSignalQuality().name()),
                Map.entry("deviceHealth", data.attributes().getDeviceHealth().name()),
                Map.entry("motion", data.attributes().motion()),
                Map.entry("power", data.attributes().power()),
                Map.entry("temperature", data.attributes().deviceTemp()),
                Map.entry("odometer", data.attributes().odometer()),
                Map.entry("accuracyLevel", data.getAccuracyLevel().name())
        );
    }

    private Retry createRetrySpec() {
        return Retry.backoff(properties.getMaxRetries(), properties.getRetryDelay())
                .maxBackoff(Duration.ofSeconds(30))
                .jitter(0.1)
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying TUGDK GPS API call (attempt {}/{}): {}",
                            retrySignal.totalRetries() + 1,
                            properties.getMaxRetries(),
                            retrySignal.failure().getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Exhausted all {} retry attempts for TUGDK GPS API",
                            properties.getMaxRetries(), retrySignal.failure());
                    return new TugdkGpsException("Failed to fetch GPS data after " +
                            properties.getMaxRetries() + " attempts",
                            retrySignal.failure());
                });
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            int statusCode = webEx.getStatusCode().value();
            // Retry on server errors and rate limiting, but not on client errors
            return statusCode >= 500 || statusCode == 429 || statusCode == 408;
        }

        return throwable instanceof SocketTimeoutException ||
                throwable instanceof ConnectException ||
                throwable instanceof TimeoutException ||
                throwable.getMessage().contains("timeout");
    }

    private Flux<GpsLocationData> handleFetchError(Throwable error) {
        log.error("Failed to fetch GPS data from TUGDK API", error);
        updateErrorMetrics(error);

        // Return empty flux on error to prevent downstream failures
        return Flux.empty();
    }

    private Throwable mapException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            return new TugdkGpsException(
                    "TUGDK GPS API error: " + webEx.getStatusCode() + " - " + webEx.getResponseBodyAsString(),
                    throwable
            );
        }

        if (throwable instanceof java.util.concurrent.TimeoutException) {
            return new TugdkGpsException("TUGDK GPS API timeout after " + properties.getTimeout(), throwable);
        }

        return new TugdkGpsException("TUGDK GPS API error: " + throwable.getMessage(), throwable);
    }

    private void updateSuccessMetrics(GpsLocationData data) {
        successfulRequests.incrementAndGet();
        lastSuccessfulCall.set(Instant.now());

        if (log.isDebugEnabled()) {
            log.debug("Successfully processed GPS data for vehicle: {}", data.vehicleIdentifier());
        }
    }

    private void updateErrorMetrics(Throwable error) {
        failedRequests.incrementAndGet();
        log.warn("GPS data fetch failed: {}", error.getMessage());
    }

    private void updateHealthStatus() {
        Instant now = Instant.now();
        lastHealthCheck.set(now);

        boolean isHealthy = determineHealthStatus();
        Duration responseTime = calculateAverageResponseTime();
        double successRate = getSuccessRate();

        ProviderHealthStatus newStatus = new ProviderHealthStatus(
                getProviderName(),
                isHealthy,
                isHealthy ? "HEALTHY" : "UNHEALTHY",
                responseTime,
                (int) successfulRequests.get(),
                (int) failedRequests.get(),
                now
        );

        healthStatus.set(newStatus);
    }

    private boolean determineHealthStatus() {
        if (isLastCallTooOld()) {
            return false;
        }

        double successRate = getSuccessRate();
        if (successRate < 0.8) { // 80% success rate threshold
            return false;
        }

        Duration avgResponseTime = calculateAverageResponseTime();
        if (avgResponseTime.toMillis() > properties.getTimeout().toMillis() * 0.8) {
            return false;
        }

        return true;
    }

    private boolean isLastCallTooOld() {
        Instant lastCall = lastSuccessfulCall.get();
        Duration maxAge = properties.getMaxDataAge();
        return lastCall.isBefore(Instant.now().minus(maxAge));
    }

    private double getSuccessRate() {
        long total = totalRequests.get();
        long successful = successfulRequests.get();

        if (total == 0) {
            return 1.0; // No requests yet, assume healthy
        }

        return (double) successful / total;
    }

    private Duration calculateAverageResponseTime() {
        // For now, return configured timeout / 2 as estimate
        // In production, this would track actual response times
        return properties.getTimeout().dividedBy(2);
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

    public static class TugdkGpsException extends RuntimeException {
        public TugdkGpsException(String message) {
            super(message);
        }

        public TugdkGpsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}