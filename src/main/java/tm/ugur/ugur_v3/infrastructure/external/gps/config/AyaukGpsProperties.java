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
@ConfigurationProperties(prefix = "gps.ayauk")
public class AyaukGpsProperties {

    @NotBlank(message = "AYAUK GPS API URL cannot be blank")
    private String url = "https://edu.ayauk.gov.tm/gps";

    @NotBlank(message = "AYAUK username cannot be blank")
    private String username = "${AYAUK_USERNAME:test}";

    @NotBlank(message = "AYAUK password cannot be blank")
    private String password = "${AYAUK_PASSWORD:test}";

    @NotNull(message = "Timeout cannot be null")
    private Duration timeout = Duration.ofSeconds(15);

    @Min(value = 1, message = "Max retries must be at least 1")
    private int maxRetries = 2;

    @NotNull(message = "Retry delay cannot be null")
    private Duration retryDelay = Duration.ofSeconds(5);

    @NotNull(message = "Polling interval cannot be null")
    private Duration pollingInterval = Duration.ofMinutes(5);

    @NotNull(message = "Max data age cannot be null")
    private Duration maxDataAge = Duration.ofHours(1);

    private boolean cachingEnabled = true;

    @NotNull(message = "Cache TTL cannot be null")
    private Duration cacheTtl = Duration.ofMinutes(10);

    private boolean healthMonitoringEnabled = true;

    @NotNull(message = "Health check interval cannot be null")
    private Duration healthCheckInterval = Duration.ofMinutes(5);

    private boolean assignmentValidationEnabled = true;

    private boolean conflictDetectionEnabled = true;

    private boolean autoRefreshEnabled = true;

    private MetricsConfig metrics = new MetricsConfig();

    private AssignmentValidationConfig assignmentValidation = new AssignmentValidationConfig();

    @Data
    public static class MetricsConfig {
        private boolean enabled = true;

        @NotBlank(message = "Metrics prefix cannot be blank")
        private String prefix = "ayauk.routes";

        private boolean requestDurationEnabled = true;

        private boolean assignmentCountEnabled = true;

        private boolean routeCoverageEnabled = true;

        private boolean conflictDetectionEnabled = true;
    }

    @Data
    public static class AssignmentValidationConfig {
        private boolean dateValidationEnabled = true;

        private boolean vehicleIdValidationEnabled = true;

        private boolean routeNumberValidationEnabled = true;

        @Min(value = 1, message = "Max assignment age must be at least 1 day")
        private int maxAssignmentAgeDays = 7;

        private boolean duplicateDetectionEnabled = true;
    }

    public String getBusesInfoUrl() {
        return url + "/buses/info";
    }

    public String getBasicAuthCredentials() {
        String credentials = username + ":" + password;
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    public boolean isProductionReady() {
        return !username.contains("${") &&
                !password.contains("${") &&
                !username.equals("test") &&
                !password.equals("test") &&
                url.startsWith("https://") &&
                timeout.getSeconds() >= 10 &&
                maxRetries >= 1;
    }

    public String getConfigSummary() {
        return String.format(
                "AyaukGpsProperties{url='%s', username='%s', timeout=%s, maxRetries=%d, " +
                        "pollingInterval=%s, cachingEnabled=%s, healthMonitoringEnabled=%s}",
                url, username, timeout, maxRetries, pollingInterval,
                cachingEnabled, healthMonitoringEnabled
        );
    }
}