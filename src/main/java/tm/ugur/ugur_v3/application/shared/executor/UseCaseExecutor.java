package tm.ugur.ugur_v3.application.shared.executor;


import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface UseCaseExecutor {


    <C, R> R execute(C command, CommandHandler<C, R> handler);

    <C, R> CompletableFuture<R> executeAsync(C command, CommandHandler<C, R> handler);

    <Q, R> R execute(Q query, QueryHandler<Q, R> handler);

    <Q, R> CompletableFuture<R> executeAsync(Q query, QueryHandler<Q, R> handler);


    <R> R executeWithRetry(Supplier<R> operation, RetryPolicy retryPolicy);

    interface CommandHandler<C, R> {
        R handle(C command);
    }

    interface QueryHandler<Q, R> {
        R handle(Q query);
    }

    interface RetryPolicy {
        int getMaxAttempts();
        long getDelayMillis();
        boolean shouldRetry(Exception exception);
    }

    class UseCaseExecutionException extends RuntimeException {
        public UseCaseExecutionException(String message) {
            super(message);
        }

        public UseCaseExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}