package tm.ugur.ugur_v3.application.shared.monitoring;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

public interface PerformanceMonitor {


    <T> T time(String operationName, Supplier<T> operation);

    <T> Mono<T> timeReactive(String operationName, Supplier<Mono<T>> operation);

    <T> Flux<T> timeReactiveFlux(String operationName, Supplier<Flux<T>> operation);

    void recordTime(String operationName, Duration duration);


    void incrementCounter(String counterName);

    void incrementCounter(String counterName, long amount);

    void incrementCounter(String counterName, Map<String, String> tags);


    void recordGauge(String gaugeName, double value);

    void recordGauge(String gaugeName, double value, Map<String, String> tags);


    long getCounterValue(String counterName);

    double getGaugeValue(String gaugeName);

    double getTimerMean(String timerName);

    PerformanceSummary getPerformanceSummary(String operationName);

    boolean isHealthy();

    MonitoringMetrics getSystemMetrics();

    record PerformanceSummary(
            String operationName,
            long totalExecutions,
            double meanDuration,
            double maxDuration,
            double p95Duration,
            double p99Duration,
            long errorCount,
            double errorRate,
            Instant lastExecution
    ) {}

    record MonitoringMetrics(
            long totalMetrics,
            long activeTimers,
            long memoryUsageBytes,
            double cpuUsagePercent,
            boolean isHealthy,
            Instant lastUpdate
    ) {}
}