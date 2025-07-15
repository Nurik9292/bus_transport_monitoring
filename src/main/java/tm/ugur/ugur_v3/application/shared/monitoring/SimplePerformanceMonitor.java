package tm.ugur.ugur_v3.application.shared.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Supplier;

@Slf4j
@Component
public class SimplePerformanceMonitor implements PerformanceMonitor {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> gauges = new ConcurrentHashMap<>();
    private final Map<String, TimerStats> timers = new ConcurrentHashMap<>();

    // ============= TIMING OPERATIONS =============

    @Override
    public <T> T time(String operationName, Supplier<T> operation) {
        Instant start = Instant.now();
        try {
            T result = operation.get();
            recordTime(operationName, Duration.between(start, Instant.now()));
            incrementCounter(operationName + ".success");
            return result;
        } catch (Exception e) {
            recordTime(operationName, Duration.between(start, Instant.now()));
            incrementCounter(operationName + ".error");
            throw e;
        }
    }

    @Override
    public <T> Mono<T> timeReactive(String operationName, Supplier<Mono<T>> operation) {
        return Mono.fromCallable(Instant::now)
                .flatMap(start -> operation.get()
                        .doOnSuccess(result -> {
                            recordTime(operationName, Duration.between(start, Instant.now()));
                            incrementCounter(operationName + ".success");
                        })
                        .doOnError(error -> {
                            recordTime(operationName, Duration.between(start, Instant.now()));
                            incrementCounter(operationName + ".error");
                        }));
    }

    @Override
    public <T> Flux<T> timeReactiveFlux(String operationName, Supplier<Flux<T>> operation) {
        return Mono.fromCallable(Instant::now)
                .flatMapMany(start -> operation.get()
                        .doOnComplete(() -> {
                            recordTime(operationName, Duration.between(start, Instant.now()));
                            incrementCounter(operationName + ".success");
                        })
                        .doOnError(error -> {
                            recordTime(operationName, Duration.between(start, Instant.now()));
                            incrementCounter(operationName + ".error");
                        }));
    }

    @Override
    public void recordTime(String operationName, Duration duration) {
        timers.computeIfAbsent(operationName, k -> new TimerStats())
                .record(duration);
        log.debug("Recorded time for {}: {}ms", operationName, duration.toMillis());
    }

    // ============= COUNTER OPERATIONS =============

    @Override
    public void incrementCounter(String counterName) {
        counters.computeIfAbsent(counterName, k -> new AtomicLong(0))
                .incrementAndGet();
        log.debug("Incremented counter {}", counterName);
    }

    @Override
    public void incrementCounter(String counterName, long amount) {
        counters.computeIfAbsent(counterName, k -> new AtomicLong(0))
                .addAndGet(amount);
        log.debug("Incremented counter {} by {}", counterName, amount);
    }

    @Override
    public void incrementCounter(String counterName, Map<String, String> tags) {
        String taggedName = counterName + tags.toString();
        incrementCounter(taggedName);
    }

    // ============= GAUGE OPERATIONS =============

    @Override
    public void recordGauge(String gaugeName, double value) {
        gauges.computeIfAbsent(gaugeName, k -> new DoubleAdder())
                .reset();
        gauges.get(gaugeName).add(value);
        log.debug("Recorded gauge {}: {}", gaugeName, value);
    }

    @Override
    public void recordGauge(String gaugeName, double value, Map<String, String> tags) {
        String taggedName = gaugeName + tags.toString();
        recordGauge(taggedName, value);
    }

    // ============= METRICS RETRIEVAL =============

    @Override
    public long getCounterValue(String counterName) {
        return counters.getOrDefault(counterName, new AtomicLong(0)).get();
    }

    @Override
    public double getGaugeValue(String gaugeName) {
        return gauges.getOrDefault(gaugeName, new DoubleAdder()).doubleValue();
    }

    @Override
    public double getTimerMean(String timerName) {
        TimerStats stats = timers.get(timerName);
        return stats != null ? stats.getMeanMillis() : 0.0;
    }

    @Override
    public PerformanceSummary getPerformanceSummary(String operationName) {
        TimerStats stats = timers.get(operationName);
        long successCount = getCounterValue(operationName + ".success");
        long errorCount = getCounterValue(operationName + ".error");
        long totalExecutions = successCount + errorCount;
        double errorRate = totalExecutions > 0 ? (double) errorCount / totalExecutions * 100.0 : 0.0;

        if (stats != null) {
            return new PerformanceSummary(
                    operationName,
                    totalExecutions,
                    stats.getMeanMillis(),
                    stats.getMaxMillis(),
                    stats.getP95Millis(),
                    stats.getP99Millis(),
                    errorCount,
                    errorRate,
                    stats.getLastExecution()
            );
        } else {
            return new PerformanceSummary(
                    operationName, totalExecutions, 0.0, 0.0, 0.0, 0.0,
                    errorCount, errorRate, Instant.now()
            );
        }
    }

    // ============= HEALTH CHECKS =============

    @Override
    public boolean isHealthy() {
        return true; // Simple implementation always healthy
    }

    @Override
    public MonitoringMetrics getSystemMetrics() {
        return new MonitoringMetrics(
                counters.size() + gauges.size() + timers.size(),
                timers.size(),
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                0.0, // CPU usage not available in simple implementation
                isHealthy(),
                Instant.now()
        );
    }

    // ============= INTERNAL TIMER STATS =============

    private static class TimerStats {
        private final java.util.List<Duration> recordings = new java.util.concurrent.CopyOnWriteArrayList<>();
        private volatile Instant lastExecution = Instant.now();

        void record(Duration duration) {
            recordings.add(duration);
            lastExecution = Instant.now();

            // Keep only last 1000 recordings to prevent memory leak
            if (recordings.size() > 1000) {
                recordings.subList(0, recordings.size() - 1000).clear();
            }
        }

        double getMeanMillis() {
            return recordings.stream()
                    .mapToLong(Duration::toMillis)
                    .average()
                    .orElse(0.0);
        }

        double getMaxMillis() {
            return recordings.stream()
                    .mapToLong(Duration::toMillis)
                    .max()
                    .orElse(0L);
        }

        double getP95Millis() {
            return getPercentile(95.0);
        }

        double getP99Millis() {
            return getPercentile(99.0);
        }

        private double getPercentile(double percentile) {
            if (recordings.isEmpty()) return 0.0;

            java.util.List<Long> sorted = recordings.stream()
                    .mapToLong(Duration::toMillis)
                    .sorted()
                    .boxed()
                    .toList();

            int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));

            return sorted.get(index);
        }

        Instant getLastExecution() {
            return lastExecution;
        }
    }
}