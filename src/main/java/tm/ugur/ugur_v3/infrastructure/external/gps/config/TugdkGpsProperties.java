package tm.ugur.ugur_v3.infrastructure.external.gps.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "gps.tugdk")
public class TugdkGpsProperties {

    @NotBlank(message = "TUGDK GPS API URL cannot be blank")
    private String url = "https://gps.tugdk.gov.tm/api";

    @NotBlank(message = "TUGDK GPS Bearer token cannot be blank")
    private String bearerToken = "${TUGDK_GPS_TOKEN:sfdsfsdfsdfsfsfsft4t4t43543543efgdf}";

    @NotNull(message = "Timeout cannot be null")
    private Duration timeout = Duration.ofSeconds(10);

    @Min(value = 1, message = "Max retries must be at least 1")
    private int maxRetries = 3;

    @NotNull(message = "Retry delay cannot be null")
    private Duration retryDelay = Duration.ofSeconds(2);

    @NotNull(message = "Polling interval cannot be null")
    private Duration pollingInterval = Duration.ofSeconds(30);

    @NotNull(message = "Max data age cannot be null")
    private Duration maxDataAge = Duration.ofMinutes(5);

    private boolean cachingEnabled = true;

    @NotNull(message = "Cache TTL cannot be null")
    private Duration cacheTtl = Duration.ofSeconds(30);

    private boolean healthMonitoringEnabled = true;

    @NotNull(message = "Health check interval cannot be null")
    private Duration healthCheckInterval = Duration.ofMinutes(1);

    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    private RateLimitConfig rateLimit = new RateLimitConfig();

    private MetricsConfig metrics = new MetricsConfig();

    @Data
    public static class CircuitBreakerConfig {
        private boolean enabled = true;

        @Min(value = 1, message = "Failure threshold must be at least 1")
        private int failureThreshold = 5;

        @Min(value = 1, message = "Success threshold must be at least 1")
        private int successThreshold = 3;

        @NotNull(message = "Circuit breaker timeout cannot be null")
        private Duration timeout = Duration.ofSeconds(30);

        @NotNull(message = "Wait duration cannot be null")
        private Duration waitDuration = Duration.ofMinutes(1);
    }

    @Data
    public static class RateLimitConfig {
        private boolean enabled = true;

        @Min(value = 1, message = "Max requests must be at least 1")
        private int maxRequests = 100;

        @NotNull(message = "Time window cannot be null")
        private Duration timeWindow = Duration.ofMinutes(1);
    }

    @Data
    public static class MetricsConfig {
        private boolean enabled = true;

        @NotBlank(message = "Metrics prefix cannot be blank")
        private String prefix = "tugdk.gps";

        private boolean requestDurationEnabled = true;

        private boolean successRateEnabled = true;

        private boolean errorRateEnabled = true;

        private boolean dataQualityEnabled = true;
    }

    public String getPositionsUrl() {
        return url + "/positions";
    }

    public boolean isProductionReady() {
        return !bearerToken.contains("${") &&
                !bearerToken.equals("sfdsfsdfsdfsfsfsft4t4t43543543efgdf") &&
                url.startsWith("https://") &&
                timeout.getSeconds() >= 5 &&
                maxRetries >= 1;
    }

    public String getConfigSummary() {
        return String.format(
                "TugdkGpsProperties{url='%s', timeout=%s, maxRetries=%d, pollingInterval=%s, " +
                        "cachingEnabled=%s, healthMonitoringEnabled=%s, circuitBreakerEnabled=%s}",
                url, timeout, maxRetries, pollingInterval,
                cachingEnabled, healthMonitoringEnabled, circuitBreaker.enabled
        );
    }
}