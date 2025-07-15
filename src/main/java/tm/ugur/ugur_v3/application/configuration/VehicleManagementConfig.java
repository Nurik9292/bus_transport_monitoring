package tm.ugur.ugur_v3.application.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;
import java.time.Duration;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "vehicle-management")
public class VehicleManagementConfig {

    private ValidationConfig validation = new ValidationConfig();
    private PerformanceConfig performance = new PerformanceConfig();
    private CacheConfig cache = new CacheConfig();
    private BusinessRulesConfig businessRules = new BusinessRulesConfig();
    private MonitoringConfig monitoring = new MonitoringConfig();

    @Data
    public static class ValidationConfig {

        @NotNull
        private GpsValidation gps = new GpsValidation();

        @NotNull
        private StatusChangeValidation statusChange = new StatusChangeValidation();

        private boolean enableBusinessRuleValidation = true;
        private boolean enableBeanValidation = true;

        @Min(1)
        @Max(30)
        private int validationTimeoutSeconds = 5;

        @Data
        public static class GpsValidation {

            @DecimalMin("0.1")
            @DecimalMax("1000.0")
            private double maxAccuracyMeters = 100.0;

            @DecimalMin("0.0")
            @DecimalMax("500.0")
            private double maxSpeedKmh = 300.0;

            @NotNull
            private Duration maxGpsAge = Duration.ofMinutes(10);

            private boolean enableLocationReasonabilityCheck = true;
            private boolean rejectNullIslandCoordinates = true;
        }

        @Data
        public static class StatusChangeValidation {

            @Min(3)
            @Max(1000)
            private int minReasonLength = 5;

            @Min(10)
            @Max(5000)
            private int maxReasonLength = 200;

            private boolean requireSupervisorApprovalForCriticalChanges = true;
            private boolean allowGenericReasons = false;

            @NotNull
            private Duration maxStatusChangeAge = Duration.ofHours(1);
        }
    }

    @Data
    public static class PerformanceConfig {

        @Min(1)
        @Max(1000)
        private int maxConcurrentLocationUpdates = 50;

        @Min(1)
        @Max(100)
        private int maxConcurrentStatusChanges = 20;

        @Min(1)
        @Max(50)
        private int maxConcurrentQueries = 30;

        @NotNull
        private Duration commandTimeout = Duration.ofSeconds(10);

        @NotNull
        private Duration queryTimeout = Duration.ofSeconds(5);

        @NotNull
        private Duration batchProcessingInterval = Duration.ofSeconds(1);

        @Min(10)
        @Max(10000)
        private int batchSize = 100;

        @Min(1)
        @Max(100)
        private int retryAttempts = 3;

        @NotNull
        private Duration retryDelay = Duration.ofMillis(500);

        private boolean enableAsyncProcessing = true;
        private boolean enableBatchProcessing = true;
    }

    @Data
    public static class CacheConfig {

        private boolean enableCaching = true;
        private boolean enableDistributedCache = false;

        @NotNull
        private Duration vehicleDataTtl = Duration.ofMinutes(5);

        @NotNull
        private Duration searchResultsTtl = Duration.ofMinutes(2);

        @NotNull
        private Duration locationDataTtl = Duration.ofMinutes(1);

        @Min(100)
        @Max(1000000)
        private int maxCacheSize = 10000;

        @DecimalMin("0.1")
        @DecimalMax("0.99")
        private double cacheHitRateThreshold = 0.6; // 60% minimum hit rate

        private boolean enableCacheMetrics = true;
        private boolean enableCacheWarming = false;
    }

    @Data
    public static class BusinessRulesConfig {

        @NotNull
        private LocationRules location = new LocationRules();

        @NotNull
        private StatusTransitionRules statusTransition = new StatusTransitionRules();

        @NotNull
        private AuthorizationRules authorization = new AuthorizationRules();

        @Data
        public static class LocationRules {

            @DecimalMin("1.0")
            @DecimalMax("1000.0")
            private double significantMovementThresholdMeters = 10.0;

            @DecimalMin("0.1")
            @DecimalMax("100.0")
            private double maxLocationJumpKm = 50.0;

            @NotNull
            private Duration locationUpdateCooldown = Duration.ofSeconds(5);

            private boolean enableLocationHistoryTracking = true;
            private boolean enableMovementAnalysis = true;
        }

        @Data
        public static class StatusTransitionRules {

            private boolean allowDirectInactiveToInRoute = false;
            private boolean requireRouteAssignmentForInRoute = true;
            private boolean allowMaintenanceOverride = true;

            @NotNull
            private Duration statusChangeCooldown = Duration.ofSeconds(10);

            private boolean enableTransitionLogging = true;
            private boolean enableStatusHistory = true;
        }

        @Data
        public static class AuthorizationRules {

            private boolean enableRoleBasedAccess = true;
            private boolean requireApprovalForCriticalChanges = true;

            @NotEmpty
            private String supervisorRole = "SUPERVISOR";

            @NotEmpty
            private String managerRole = "MANAGER";

            @NotEmpty
            private String systemRole = "SYSTEM";
        }
    }

    @Data
    public static class MonitoringConfig {

        private boolean enableMetrics = true;
        private boolean enableDetailedLogging = false;
        private boolean enablePerformanceTracking = true;

        @NotNull
        private Duration metricsCollectionInterval = Duration.ofSeconds(30);

        @NotNull
        private Duration healthCheckInterval = Duration.ofMinutes(1);

        @DecimalMin("0.01")
        @DecimalMax("10.0")
        private double slowOperationThresholdSeconds = 1.0;

        @DecimalMin("0.0")
        @DecimalMax("50.0")
        private double errorRateThresholdPercentage = 5.0;

        private boolean enableAlerts = true;
        private boolean enableAuditLogging = true;

        @Min(100)
        @Max(100000)
        private int maxMetricsHistorySize = 10000;
    }

    // ============= CONVENIENCE METHODS =============

    public boolean isHighPerformanceMode() {
        return performance.enableAsyncProcessing &&
                performance.enableBatchProcessing &&
                cache.enableCaching &&
                performance.maxConcurrentLocationUpdates >= 30;
    }

    public boolean isDevelopmentMode() {
        return monitoring.enableDetailedLogging &&
                !cache.enableDistributedCache &&
                !businessRules.authorization.enableRoleBasedAccess;
    }

    public Duration getTotalCommandTimeout() {
        return performance.commandTimeout.plus(
                performance.retryDelay.multipliedBy(performance.retryAttempts)
        );
    }

    public int getRecommendedBatchSize() {
        if (isHighPerformanceMode()) {
            return Math.min(performance.batchSize * 2, 500);
        }
        return performance.batchSize;
    }

    public void validateConfiguration() {
        if (performance.commandTimeout.compareTo(performance.queryTimeout) <= 0) {
            throw new IllegalStateException("Command timeout must be longer than query timeout");
        }

        if (cache.vehicleDataTtl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalStateException("Vehicle data TTL too long - max 1 hour recommended");
        }

        if (performance.batchSize > 1000) {
            throw new IllegalStateException("Batch size too large - max 1000 recommended");
        }

        if (validation.gps.maxAccuracyMeters > 500.0) {
            throw new IllegalStateException("GPS accuracy threshold too high - vehicles would be unusable");
        }
    }
}