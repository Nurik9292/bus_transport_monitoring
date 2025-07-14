package tm.ugur.ugur_v3.application.shared.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


public interface PerformanceMonitor {

    Timer startTimer(String operationName, Map<String, String> tags);

    Timer startTimer(String operationName);

    <T> T time(String operationName, Supplier<T> operation);

    <T> CompletableFuture<T> timeAsync(String operationName, Supplier<CompletableFuture<T>> operation);

    void recordMetric(String metricName, double value, Map<String, String> tags);

    void incrementCounter(String counterName, long increment, Map<String, String> tags);

    void incrementCounter(String counterName, Map<String, String> tags);

    void recordGauge(String gaugeName, double value, Map<String, String> tags);

    void recordHistogram(String histogramName, double value, Map<String, String> tags);

    void checkpoint(String operationName, String checkpointName, Timer timer);

    PerformanceMetrics getCurrentMetrics();

    PerformanceMetrics getMetricsForPeriod(Duration period);

    OperationMetrics getOperationMetrics(String operationName);

    List<SlowOperation> getSlowestOperations(int limit);

    SlaCompliance getSlaCompliance();

    void configureSla(String operationName, Duration threshold, double successRate);

    SystemHealth getSystemHealth();

    void createAlertRule(AlertRule alertRule);

    void removeAlertRule(String ruleId);

    List<Alert> getActiveAlerts();

    CompletableFuture<String> exportMetrics(ExportFormat exportFormat);


    interface Timer {

        Duration stop();

        Duration stop(boolean success);

        Duration stop(boolean success, Map<String, String> additionalTags);

        Duration getElapsed();

        void addCheckpoint(String checkpointName);
    }

    interface PerformanceMetrics {
        long getTotalOperations();

        Duration getAverageResponseTime();

        Duration getP95ResponseTime();

        Duration getP99ResponseTime();

        double getSuccessRate();

        double getThroughput();

        long getErrorCount();

        Duration getPeriod();

        Instant getStartTime();

        Instant getEndTime();

        Map<String, OperationMetrics> getOperationBreakdown();
    }

    interface OperationMetrics {

        String getOperationName();

        long getExecutionCount();

        Duration getAverageTime();

        Duration getMinTime();

        Duration getMaxTime();

        Duration getStandardDeviation();

        Map<Integer, Duration> getPercentiles();

        double getSuccessRate();

        Instant getLastExecution();

        Map<String, Duration> getCheckpoints();
    }


    interface SlowOperation {

        String getOperationName();

        Duration getExecutionTime();

        Instant getTimestamp();

        Map<String, String> getTags();

        String getStackTrace();
    }

    interface SlaCompliance {

        double getComplianceRate();

        Map<String, Double> getOperationCompliance();

        List<SlaViolation> getViolations();

        Duration getAnalysisPeriod();
    }

    interface SlaViolation {

        String getOperationName();

        Duration getActualTime();

        Duration getSlaThreshold();

        Instant getTimestamp();

        AlertSeverity getSeverity();
    }

    interface SystemHealth {

        double getHealthScore();

        HealthStatus getStatus();

        Map<String, ComponentHealth> getComponentHealth();

        List<String> getRecommendations();
    }

    interface ComponentHealth {

        String getComponentName();

        HealthStatus getStatus();

        double getScore();

        String getDetails();

        Instant getLastCheck();
    }

    enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }


    interface AlertRule {

        String getRuleId();

        String getRuleName();

        String getMetricName();

        AlertCondition getCondition();

        double getThreshold();

        Duration getEvaluationPeriod();

        AlertSeverity getSeverity();

        String getDescription();

        boolean isEnabled();
    }


    enum AlertCondition {
        GREATER_THAN,
        LESS_THAN,
        EQUALS,
        RATE_OF_CHANGE,
        PERCENTAGE_CHANGE,
        STANDARD_DEVIATION,
        MISSING_DATA
    }

    enum AlertSeverity {
        CRITICAL(4),
        HIGH(3),
        MEDIUM(2),
        LOW(1),
        INFO(0);

        private final int level;

        AlertSeverity(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    interface Alert {

        String getAlertId();

        String getRuleId();

        String getAlertName();

        String getDescription();

        AlertSeverity getSeverity();

        Instant getTriggeredAt();

        double getCurrentValue();

        double getThresholdValue();

        AlertStatus getStatus();

        Map<String, String> getMetadata();
    }

    enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        RESOLVED,
        SUPPRESSED
    }

    enum ExportFormat {
        PROMETHEUS,
        JSON,
        CSV,
        GRAFANA
    }

    interface StandardMetrics {

        String VEHICLE_LOCATION_UPDATE_TIME = "vehicle.location.update.time";
        String ETA_CALCULATION_TIME = "route.eta.calculation.time";
        String USER_REQUEST_TIME = "user.request.processing.time";
        String GPS_API_RESPONSE_TIME = "external.gps.api.response.time";

        String DATABASE_QUERY_TIME = "database.query.execution.time";
        String CACHE_ACCESS_TIME = "cache.access.time";
        String WEBSOCKET_MESSAGE_TIME = "websocket.message.delivery.time";
        String RABBITMQ_PUBLISH_TIME = "rabbitmq.message.publish.time";

        String ACTIVE_VEHICLES_COUNT = "vehicles.active.count";
        String CONCURRENT_USERS_COUNT = "users.concurrent.count";
        String GPS_UPDATES_PER_SECOND = "gps.updates.per.second";
        String ETA_ACCURACY_PERCENTAGE = "eta.accuracy.percentage";

        String GPS_API_ERROR_COUNT = "external.gps.api.errors";
        String DATABASE_CONNECTION_ERRORS = "database.connection.errors";
        String WEBSOCKET_CONNECTION_ERRORS = "websocket.connection.errors";
        String CACHE_MISS_RATE = "cache.miss.rate";

        String SLA_VIOLATION_COUNT = "sla.violations.count";
        String RESPONSE_TIME_SLA_COMPLIANCE = "response.time.sla.compliance";
        String AVAILABILITY_SLA_COMPLIANCE = "availability.sla.compliance";
    }

    interface Tags {

        static Map<String, String> forVehicle(String vehicleId, String operation) {
            return Map.of(
                    "vehicle_id", vehicleId,
                    "operation", operation,
                    "component", "vehicle"
            );
        }

        static Map<String, String> forRoute(String routeId, String operation) {
            return Map.of(
                    "route_id", routeId,
                    "operation", operation,
                    "component", "route"
            );
        }


        static Map<String, String> forUser(String userId, String operation) {
            return Map.of(
                    "user_id", userId,
                    "operation", operation,
                    "component", "user"
            );
        }

        static Map<String, String> forExternalApi(String apiName, String endpoint) {
            return Map.of(
                    "api_name", apiName,
                    "endpoint", endpoint,
                    "component", "external"
            );
        }

        static Map<String, String> forDatabase(String operation, String table) {
            return Map.of(
                    "operation", operation,
                    "table", table,
                    "component", "database"
            );
        }

        static Map<String, String> forCache(String operation, String namespace) {
            return Map.of(
                    "operation", operation,
                    "namespace", namespace,
                    "component", "cache"
            );
        }
    }
}