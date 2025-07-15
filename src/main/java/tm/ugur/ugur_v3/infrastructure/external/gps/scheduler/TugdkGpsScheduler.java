package tm.ugur.ugur_v3.infrastructure.external.gps.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;
import tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider;
import tm.ugur.ugur_v3.infrastructure.external.gps.adapters.TugdkGpsAdapter;
import tm.ugur.ugur_v3.infrastructure.external.gps.config.TugdkGpsProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gps.tugdk.polling.enabled", havingValue = "true", matchIfMissing = true)
public class TugdkGpsScheduler {

    private final TugdkGpsAdapter tugdkGpsAdapter;
    private final TugdkGpsProperties properties;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong pollCount = new AtomicLong(0);
    private final AtomicReference<Instant> lastPoll = new AtomicReference<>(Instant.now());
    private final AtomicReference<Instant> lastSuccessfulPoll = new AtomicReference<>(Instant.now());
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    @Scheduled(fixedDelayString = "#{@tugdkGpsProperties.pollingInterval.toMillis()}")
    public void pollGpsData() {
        if (!isRunning.compareAndSet(false, true)) {
            log.debug("GPS polling already in progress, skipping this cycle");
            return;
        }

        try {
            executePolling();
        } finally {
            isRunning.set(false);
        }
    }

    @Scheduled(fixedDelayString = "#{@tugdkGpsProperties.healthCheckInterval.toMillis()}")
    public void healthCheck() {
        if (!properties.isHealthMonitoringEnabled()) {
            return;
        }

        tugdkGpsAdapter.getHealthStatus()
                .doOnNext(this::logHealthStatus)
                .doOnError(error -> log.error("Health check failed for TUGDK GPS adapter", error))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void executePolling() {
        Instant pollStart = Instant.now();
        lastPoll.set(pollStart);
        pollCount.incrementAndGet();

        log.debug("Starting GPS data polling cycle #{}", pollCount.get());

        tugdkGpsAdapter.getVehicleLocations()
                .doOnNext(this::processGpsData)
                .doOnComplete(() -> handlePollingSuccess(pollStart))
                .doOnError(error -> handlePollingError(pollStart, error))
                .onErrorComplete() // Don't let errors break the scheduler
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void processGpsData(GpsDataProvider.GpsLocationData data) {
        if (log.isDebugEnabled()) {
            log.debug("Received GPS data for vehicle: {} at [{}, {}]",
                    data.vehicleIdentifier(), data.latitude(), data.longitude());
        }

        // Here you would typically forward the data to domain services
        // For now, we just log successful data reception
    }

    private void handlePollingSuccess(Instant pollStart) {
        lastSuccessfulPoll.set(Instant.now());
        consecutiveFailures.set(0);

        Duration pollDuration = Duration.between(pollStart, Instant.now());
        log.info("GPS polling cycle #{} completed successfully in {}ms",
                pollCount.get(), pollDuration.toMillis());
    }

    private void handlePollingError(Instant pollStart, Throwable error) {
        long failures = consecutiveFailures.incrementAndGet();
        Duration pollDuration = Duration.between(pollStart, Instant.now());

        log.error("GPS polling cycle #{} failed after {}ms (consecutive failures: {}): {}",
                pollCount.get(), pollDuration.toMillis(), failures, error.getMessage());

        // Implement exponential backoff for consecutive failures
        if (failures > 3) {
            log.warn("Too many consecutive GPS polling failures ({}), consider checking TUGDK API status", failures);
        }
    }

    private void logHealthStatus(GpsDataProvider.ProviderHealthStatus status) {
        if (status.isHealthy()) {
            log.debug("TUGDK GPS adapter health: {} - Success rate: {:.1f}%, Response time: {}ms",
                    status.status(),
                    getSuccessRate(status) * 100,
                    status.responseTime().toMillis());
        } else {
            log.warn("TUGDK GPS adapter health: {} - Success rate: {:.1f}%, Failed requests: {}",
                    status.status(),
                    getSuccessRate(status) * 100,
                    status.failedRequests());
        }
    }

    private double getSuccessRate(GpsDataProvider.ProviderHealthStatus status) {
        int total = status.successfulRequests() + status.failedRequests();
        if (total == 0) {
            return 1.0;
        }
        return (double) status.successfulRequests() / total;
    }

    public PollingStatistics getPollingStatistics() {
        return new PollingStatistics(
                pollCount.get(),
                lastPoll.get(),
                lastSuccessfulPoll.get(),
                consecutiveFailures.get(),
                isRunning.get()
        );
    }

    public record PollingStatistics(
            long totalPolls,
            Instant lastPoll,
            Instant lastSuccessfulPoll,
            long consecutiveFailures,
            boolean isCurrentlyRunning
    ) {

        public Duration timeSinceLastPoll() {
            return Duration.between(lastPoll, Instant.now());
        }

        public Duration timeSinceLastSuccess() {
            return Duration.between(lastSuccessfulPoll, Instant.now());
        }

        public boolean isHealthy() {
            return consecutiveFailures < 3 &&
                    timeSinceLastSuccess().compareTo(Duration.ofMinutes(5)) < 0;
        }
    }
}