/**
 * COMPONENT: CombinedGpsScheduler
 * LAYER: Infrastructure/External/GPS/Scheduler
 * PURPOSE: Coordinated scheduling for both TUGDK and AYAUK APIs
 * PERFORMANCE TARGET: < 10ms scheduling overhead, intelligent coordination
 * SCALABILITY: Non-blocking scheduling for both GPS and route data
 */
package tm.ugur.ugur_v3.infrastructure.external.gps.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider;
import tm.ugur.ugur_v3.infrastructure.external.gps.adapters.AyaukGpsAdapter;
import tm.ugur.ugur_v3.infrastructure.external.gps.adapters.TugdkGpsAdapter;
import tm.ugur.ugur_v3.infrastructure.external.gps.config.AyaukGpsProperties;
import tm.ugur.ugur_v3.infrastructure.external.gps.config.TugdkGpsProperties;
import tm.ugur.ugur_v3.infrastructure.external.gps.dto.AyaukGpsDataDto;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gps.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CombinedGpsScheduler {

    private final TugdkGpsAdapter tugdkGpsAdapter;
    private final AyaukGpsAdapter ayaukGpsAdapter;
    private final TugdkGpsProperties tugdkProperties;
    private final AyaukGpsProperties ayaukProperties;

    
    private final AtomicBoolean tugdkRunning = new AtomicBoolean(false);
    private final AtomicBoolean ayaukRunning = new AtomicBoolean(false);
    private final AtomicLong combinedPollCount = new AtomicLong(0);


    private final Map<String, VehicleDataCorrelation> vehicleCorrelations = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastCorrelationUpdate = new AtomicReference<>(Instant.now());

    @Scheduled(fixedDelayString = "#{@tugdkGpsProperties.pollingInterval.toMillis()}")
    public void pollGpsLocations() {
        if (!tugdkRunning.compareAndSet(false, true)) {
            log.debug("TUGDK GPS polling already in progress, skipping");
            return;
        }

        try {
            executeGpsLocationPolling();
        } finally {
            tugdkRunning.set(false);
        }
    }

    @Scheduled(fixedDelayString = "#{@ayaukGpsProperties.pollingInterval.toMillis()}")
    public void pollRouteAssignments() {
        if (!ayaukRunning.compareAndSet(false, true)) {
            log.debug("AYAUK route polling already in progress, skipping");
            return;
        }

        try {
            executeRouteAssignmentPolling();
        } finally {
            ayaukRunning.set(false);
        }
    }

    @Scheduled(fixedRate = 600000)
    public void correlateAndCleanupData() {
        correlateVehicleData();
        cleanupStaleCorrelations();
    }

    @Scheduled(fixedRate = 120000)
    public void monitorCombinedHealth() {
        Mono.zip(
                        tugdkGpsAdapter.getHealthStatus(),
                        ayaukGpsAdapter.getHealthStatus()
                )
                .doOnNext(tuple -> {
                    var tugdkHealth = tuple.getT1();
                    var ayaukHealth = tuple.getT2();
                    logCombinedHealthStatus(tugdkHealth, ayaukHealth);
                })
                .doOnError(error -> log.error("Combined health check failed", error))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void executeGpsLocationPolling() {
        Instant pollStart = Instant.now();
        combinedPollCount.incrementAndGet();

        log.debug("Starting GPS location polling cycle #{}", combinedPollCount.get());

        tugdkGpsAdapter.getVehicleLocations()
                .doOnNext(this::processGpsLocationData)
                .doOnComplete(() -> handleGpsPollingSuccess(pollStart))
                .doOnError(error -> handleGpsPollingError(pollStart, error))
                .onErrorComplete()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void executeRouteAssignmentPolling() {
        Instant pollStart = Instant.now();

        log.debug("Starting route assignment polling");

        ayaukGpsAdapter.refreshRouteAssignments()
                .doOnSuccess(unused -> handleRoutePollingSuccess(pollStart))
                .doOnError(error -> handleRoutePollingError(pollStart, error))
                .onErrorComplete()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void processGpsLocationData(tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider.GpsLocationData gpsData) {
        log.debug("Processing GPS data for vehicle: {} at [{}, {}]",
                gpsData.vehicleIdentifier(), gpsData.latitude(), gpsData.longitude());


        ayaukGpsAdapter.getRouteAssignment(gpsData.vehicleIdentifier())
                .doOnNext(routeAssignment -> {
                    correlateVehicleData(gpsData, routeAssignment);
                })
                .doOnError(error -> log.debug("No route assignment found for vehicle: {}",
                        gpsData.vehicleIdentifier()))
                .onErrorComplete()
                .subscribe();
    }

    private void correlateVehicleData(tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider.GpsLocationData gpsData,
                                      AyaukGpsDataDto routeAssignment) {
        String vehicleId = gpsData.vehicleIdentifier();

        VehicleDataCorrelation correlation = new VehicleDataCorrelation(
                vehicleId,
                gpsData,
                routeAssignment,
                Instant.now()
        );

        vehicleCorrelations.put(vehicleId, correlation);
        lastCorrelationUpdate.set(Instant.now());

        log.debug("Correlated vehicle {} with route {} at [{}, {}]",
                vehicleId, routeAssignment.getRouteNumber(),
                gpsData.latitude(), gpsData.longitude());
    }

    private void correlateVehicleData() {
        log.debug("Starting data correlation process");

        int correlatedCount = 0;
        int totalVehicles = vehicleCorrelations.size();

        for (VehicleDataCorrelation correlation : vehicleCorrelations.values()) {
            if (correlation.isComplete() && correlation.isRecent()) {
                correlatedCount++;
            }
        }

        log.info("Data correlation: {}/{} vehicles have complete data",
                correlatedCount, totalVehicles);
    }

    private void cleanupStaleCorrelations() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));

        vehicleCorrelations.entrySet().removeIf(entry -> {
            VehicleDataCorrelation correlation = entry.getValue();
            return correlation.lastUpdate().isBefore(cutoff);
        });

        log.debug("Cleaned up stale correlations, {} vehicles remaining",
                vehicleCorrelations.size());
    }

    private void handleGpsPollingSuccess(Instant pollStart) {
        Duration pollDuration = Duration.between(pollStart, Instant.now());
        log.info("GPS polling cycle #{} completed in {}ms",
                combinedPollCount.get(), pollDuration.toMillis());
    }

    private void handleGpsPollingError(Instant pollStart, Throwable error) {
        Duration pollDuration = Duration.between(pollStart, Instant.now());
        log.error("GPS polling cycle #{} failed after {}ms: {}",
                combinedPollCount.get(), pollDuration.toMillis(), error.getMessage());
    }

    private void handleRoutePollingSuccess(Instant pollStart) {
        Duration pollDuration = Duration.between(pollStart, Instant.now());
        log.info("Route assignment polling completed in {}ms", pollDuration.toMillis());
    }

    private void handleRoutePollingError(Instant pollStart, Throwable error) {
        Duration pollDuration = Duration.between(pollStart, Instant.now());
        log.error("Route assignment polling failed after {}ms: {}",
                pollDuration.toMillis(), error.getMessage());
    }

    private void logCombinedHealthStatus(
            tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider.ProviderHealthStatus tugdkHealth,
            tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider.ProviderHealthStatus ayaukHealth) {

        boolean combinedHealthy = tugdkHealth.isHealthy() && ayaukHealth.isHealthy();

        if (combinedHealthy) {
            log.debug("Combined GPS health: HEALTHY - TUGDK: {}, AYAUK: {}",
                    tugdkHealth.status(), ayaukHealth.status());
        } else {
            log.warn("Combined GPS health: DEGRADED - TUGDK: {}, AYAUK: {}",
                    tugdkHealth.status(), ayaukHealth.status());
        }
    }

    public CorrelationStatistics getCorrelationStatistics() {
        int totalVehicles = vehicleCorrelations.size();
        int completeCorrelations = 0;
        int recentCorrelations = 0;

        for (VehicleDataCorrelation correlation : vehicleCorrelations.values()) {
            if (correlation.isComplete()) {
                completeCorrelations++;
            }
            if (correlation.isRecent()) {
                recentCorrelations++;
            }
        }

        return new CorrelationStatistics(
                totalVehicles,
                completeCorrelations,
                recentCorrelations,
                lastCorrelationUpdate.get(),
                tugdkRunning.get(),
                ayaukRunning.get()
        );
    }

    public record VehicleDataCorrelation(
            String vehicleId,
            GpsDataProvider.GpsLocationData gpsData,
            AyaukGpsDataDto routeAssignment,
            Instant lastUpdate
    ) {

        public boolean isComplete() {
            return gpsData != null && routeAssignment != null;
        }

        public boolean isRecent() {
            return lastUpdate.isAfter(Instant.now().minus(Duration.ofMinutes(30)));
        }

        public boolean hasValidGpsCoordinates() {
            return gpsData != null &&
                    gpsData.latitude() != 0.0 &&
                    gpsData.longitude() != 0.0;
        }

        public String getRouteNumber() {
            return routeAssignment != null ? routeAssignment.getRouteNumber() : "UNKNOWN";
        }
    }

    public record CorrelationStatistics(
            int totalVehicles,
            int completeCorrelations,
            int recentCorrelations,
            Instant lastUpdate,
            boolean tugdkPollingActive,
            boolean ayaukPollingActive
    ) {

        public double getCompletionRate() {
            return totalVehicles == 0 ? 0.0 : (double) completeCorrelations / totalVehicles;
        }

        public double getRecentRate() {
            return totalVehicles == 0 ? 0.0 : (double) recentCorrelations / totalVehicles;
        }

        public boolean isHealthy() {
            return getCompletionRate() > 0.7 &&
                    getRecentRate() > 0.8 &&
                    Duration.between(lastUpdate, Instant.now()).toMinutes() < 10;
        }
    }
}