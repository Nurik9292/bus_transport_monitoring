package tm.ugur.ugur_v3.application.shared.executor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface AsyncUseCaseExecutor {

    <C, R> CompletableFuture<R> executeAsync(
            C command,
            UseCaseExecutor.CommandHandler<C, R> handler,
            ExecutionPriority priority
    );

    <Q, R> CompletableFuture<R> executeQueryAsync(
            Q query,
            UseCaseExecutor.QueryHandler<Q, R> handler,
            CacheStrategy cacheStrategy
    );

    <T, R> CompletableFuture<java.util.List<R>> executeBatch(
            java.util.List<T> operations,
            Function<T, R> processor,
            int batchSize
    );

    <T, R> java.util.concurrent.Flow.Publisher<R> executeStream(
            java.util.concurrent.Flow.Publisher<T> dataStream,
            Function<T, CompletionStage<R>> processor
    );

    <R> CompletableFuture<R> executeWithTimeout(
            java.util.function.Supplier<CompletableFuture<R>> operation,
            Duration timeout,
            java.util.function.Supplier<R> fallback
    );

    ExecutorMetrics getMetrics();

    enum ExecutionPriority {

        CRITICAL(1),

        HIGH(2),

        NORMAL(3),

        LOW(4);

        private final int level;

        ExecutionPriority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    interface CacheStrategy {

        Duration getTtl();

        String getCacheKey(Object query);

        boolean shouldCache(Object result);

        boolean isRefreshAhead();
    }

    interface ExecutorMetrics {

        long getActiveOperations();

        long getQueuedOperations();

        Duration getAverageExecutionTime();

        double getSuccessRate();

        double getThroughput();

        CircuitBreakerStatus getCircuitBreakerStatus();
    }


    enum CircuitBreakerStatus {
        CLOSED,    // Нормальная работа
        OPEN,      // Блокировка операций из-за ошибок
        HALF_OPEN  // Тестирование восстановления
    }
}