package tm.ugur.ugur_v3.application.shared.executor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class AsyncRetryWrapper {

    private final UseCaseExecutor syncExecutor;
    private final AsyncUseCaseExecutor asyncExecutor;
    private final ScheduledExecutorService scheduler;

    public AsyncRetryWrapper(UseCaseExecutor syncExecutor,
                             AsyncUseCaseExecutor asyncExecutor,
                             ScheduledExecutorService scheduler) {
        this.syncExecutor = syncExecutor;
        this.asyncExecutor = asyncExecutor;
        this.scheduler = scheduler;
    }


    public <C, R> CompletableFuture<R> executeAsyncWithRetry(
            C command,
            UseCaseExecutor.CommandHandler<C, R> handler,
            AsyncUseCaseExecutor.ExecutionPriority priority,
            UseCaseExecutor.RetryPolicy retryPolicy) {

        return executeWithRetryInternal(
                () -> asyncExecutor.executeAsync(command, handler, priority),
                retryPolicy,
                1
        );
    }

    public <Q, R> CompletableFuture<R> executeQueryAsyncWithRetry(
            Q query,
            UseCaseExecutor.QueryHandler<Q, R> handler,
            AsyncUseCaseExecutor.CacheStrategy cacheStrategy,
            UseCaseExecutor.RetryPolicy retryPolicy) {

        return executeWithRetryInternal(
                () -> asyncExecutor.executeQueryAsync(query, handler, cacheStrategy),
                retryPolicy,
                1
        );
    }


    public <R> CompletableFuture<R> executeWithTimeoutAndRetry(
            Supplier<CompletableFuture<R>> operation,
            Duration timeout,
            Supplier<R> fallback,
            UseCaseExecutor.RetryPolicy retryPolicy) {

        return executeWithRetryInternal(
                () -> asyncExecutor.executeWithTimeout(operation, timeout, fallback),
                retryPolicy,
                1
        );
    }

    private <R> CompletableFuture<R> executeWithRetryInternal(
            Supplier<CompletableFuture<R>> operation,
            UseCaseExecutor.RetryPolicy retryPolicy,
            int currentAttempt) {

        return operation.get()
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        return CompletableFuture.completedFuture(result);
                    }

                    Throwable actualException = throwable instanceof CompletionException ?
                            throwable.getCause() : throwable;

                    if (currentAttempt >= retryPolicy.getMaxAttempts() ||
                            !retryPolicy.shouldRetry((Exception) actualException)) {

                        CompletableFuture<R> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(actualException);
                        return failedFuture;
                    }

                    long delayMillis = calculateDelay(retryPolicy, currentAttempt);

                    CompletableFuture<R> retryFuture = new CompletableFuture<>();
                    scheduler.schedule(
                            () -> {
                                executeWithRetryInternal(operation, retryPolicy, currentAttempt + 1)
                                        .whenComplete((retryResult, retryThrowable) -> {
                                            if (retryThrowable != null) {
                                                retryFuture.completeExceptionally(retryThrowable);
                                            } else {
                                                retryFuture.complete(retryResult);
                                            }
                                        });
                            },
                            delayMillis,
                            TimeUnit.MILLISECONDS
                    );

                    return retryFuture;
                })
                .thenCompose(future -> future);
    }

    private long calculateDelay(UseCaseExecutor.RetryPolicy retryPolicy, int attempt) {
        // Exponential backoff: baseDelay * 2^(attempt-1)
        return retryPolicy.getDelayMillis() * (long) Math.pow(2, attempt - 1);
    }


    public <C, R> CompletableFuture<R> executeWithDatabaseRetry(
            C command,
            UseCaseExecutor.CommandHandler<C, R> handler) {

        return executeAsyncWithRetry(
                command,
                handler,
                AsyncUseCaseExecutor.ExecutionPriority.HIGH,
                createDatabaseRetryPolicy()
        );
    }

    public <Q, R> CompletableFuture<R> executeWithExternalServiceRetry(
            Q query,
            UseCaseExecutor.QueryHandler<Q, R> handler) {

        return executeQueryAsyncWithRetry(
                query,
                handler,
                createDefaultCacheStrategy(),
                createExternalServiceRetryPolicy()
        );
    }


    private UseCaseExecutor.RetryPolicy createDatabaseRetryPolicy() {
        return new UseCaseExecutor.RetryPolicy() {
            @Override
            public int getMaxAttempts() { return 3; }

            @Override
            public long getDelayMillis() { return 1000; } // 1 second base delay

            @Override
            public boolean shouldRetry(Exception exception) {
                return isDatabaseConnectivityIssue(exception) ||
                        isOptimisticLockingException(exception);
            }
        };
    }

    private UseCaseExecutor.RetryPolicy createExternalServiceRetryPolicy() {
        return new UseCaseExecutor.RetryPolicy() {
            @Override
            public int getMaxAttempts() { return 5; }

            @Override
            public long getDelayMillis() { return 500; }

            @Override
            public boolean shouldRetry(Exception exception) {
                return isNetworkException(exception) ||
                        isServiceUnavailableException(exception);
            }
        };
    }

    private AsyncUseCaseExecutor.CacheStrategy createDefaultCacheStrategy() {
        return new AsyncUseCaseExecutor.CacheStrategy() {
            @Override
            public Duration getTtl() { return Duration.ofMinutes(15); }

            @Override
            public String getCacheKey(Object query) { return query.toString(); }

            @Override
            public boolean shouldCache(Object result) { return result != null; }

            @Override
            public boolean isRefreshAhead() { return false; }
        };
    }


    private boolean isDatabaseConnectivityIssue(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("connection") ||
                        message.contains("timeout") ||
                        message.contains("Connection refused")
        );
    }

    private boolean isOptimisticLockingException(Exception e) {
        return e.getClass().getSimpleName().contains("OptimisticLock") ||
                e.getClass().getSimpleName().contains("StaleObject");
    }

    private boolean isNetworkException(Exception e) {
        return e instanceof java.net.ConnectException ||
                e instanceof java.net.SocketTimeoutException ||
                e instanceof java.net.UnknownHostException;
    }

    private boolean isServiceUnavailableException(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("503") ||
                        message.contains("Service Unavailable") ||
                        message.contains("Circuit breaker")
        );
    }
}
