package tm.ugur.ugur_v3.infrastructure.external.gps.resilience;

import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class RetryPolicy {

    public Retry createRetryPolicy(String providerName) {
        return Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(10))
                .jitter(0.75)
                .filter(this::isRetryableException)
                .onRetryExhaustedThrow((retrySpec, signal) ->
                        new GpsProviderException("Retry exhausted for " + providerName, signal.failure()));
    }

    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof java.net.ConnectException ||
                throwable instanceof java.util.concurrent.TimeoutException ||
                throwable instanceof org.springframework.web.reactive.function.client.WebClientRequestException;
    }
}
