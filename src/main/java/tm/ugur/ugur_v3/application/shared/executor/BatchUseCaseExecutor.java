package tm.ugur.ugur_v3.application.shared.executor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public interface BatchUseCaseExecutor {

    <C, R> CompletableFuture<List<BatchResult<R>>> executeBatch(
            List<C> commands,
            UseCaseExecutor.CommandHandler<C, R> handler,
            BatchConfig config
    );

    <C, R> Flow.Publisher<BatchResult<R>> executeStreamingBatch(
            java.util.concurrent.Flow.Publisher<C> commandStream,
            UseCaseExecutor.CommandHandler<C, R> handler,
            StreamingBatchConfig config
    );

    <C, R> CompletableFuture<List<BatchResult<R>>> executeParallelBatch(
            List<C> commands,
            UseCaseExecutor.CommandHandler<C, R> handler,
            int parallelism
    );

    <C, R> CompletableFuture<List<BatchResult<R>>> executeOrderedBatch(
            List<C> commands,
            UseCaseExecutor.CommandHandler<C, R> handler,
            OrderedBatchConfig config
    );

    <C, R> CompletableFuture<TransactionalBatchResult<R>> executeTransactionalBatch(
            List<C> commands,
            UseCaseExecutor.CommandHandler<C, R> handler
    );

    interface BatchConfig {

        int getPreferredBatchSize();

        int getMaxBatchSize();

        Duration getMaxWaitTime();

        ErrorHandlingStrategy getErrorHandlingStrategy();

        int getMaxRetryAttempts();

        Duration getRetryDelay();
    }

    interface StreamingBatchConfig extends BatchConfig {

        Duration getWindowSize();
        BatchTrigger getBatchTrigger();

        BackpressureStrategy getBackpressureStrategy();
    }
    interface OrderedBatchConfig extends BatchConfig {

        int getMaxOutOfOrder();

        Duration getOrderingTimeout();
    }

    interface BatchResult<T> {

        boolean isSuccess();

        T getResult();

        Exception getError();

        Duration getExecutionTime();

        int getIndex();
    }

    interface TransactionalBatchResult<T> {

        boolean isTransactionSuccessful();

        List<BatchResult<T>> getResults();

        Exception getRollbackCause();

        String getTransactionId();
    }

    enum ErrorHandlingStrategy {

        CONTINUE_ON_ERROR,

        FAIL_FAST,

        RETRY_FAILED,

        DEFER_FAILED
    }

    enum BatchTrigger {

        SIZE_BASED,

        TIME_BASED,

        SIZE_OR_TIME,

        CUSTOM_CONDITION
    }

    enum BackpressureStrategy {

        BLOCK,

        DROP_OLDEST,

        DROP_NEWEST,

        BUFFER_OVERFLOW
    }
}