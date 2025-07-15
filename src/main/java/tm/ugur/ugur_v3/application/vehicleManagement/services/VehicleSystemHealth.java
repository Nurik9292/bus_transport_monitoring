package tm.ugur.ugur_v3.application.vehicleManagement.services;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

import java.time.Instant;
import java.util.Map;


public record VehicleSystemHealth(
        SystemOverview systemOverview,
        PerformanceMetrics performanceMetrics,
        OperationalStatus operationalStatus,
        java.time.Instant generatedAt
) {

    public static VehicleSystemHealth create(SystemOverview overview,
                                             PerformanceMetrics performance,
                                             OperationalStatus operational) {
        return new VehicleSystemHealth(overview, performance, operational, Instant.now());
    }

    public double getOverallHealthScore() {
        double systemScore = calculateSystemScore();
        double performanceScore = calculatePerformanceScore();
        double operationalScore = calculateOperationalScore();


        return (systemScore * 0.4) + (performanceScore * 0.35) + (operationalScore * 0.25);
    }

    private double calculateSystemScore() {
        if (systemOverview.totalVehicles() == 0) return 0.0;

        double activeRatio = (double) systemOverview.activeVehicles() / systemOverview.totalVehicles();
        double healthyRatio = 1.0 - ((double) systemOverview.vehiclesNeedingMaintenance() / systemOverview.totalVehicles());

        return (activeRatio * 0.6) + (healthyRatio * 0.4);
    }

    private double calculatePerformanceScore() {
        double errorRateScore = 1.0 - Math.min(performanceMetrics.errorRate(), 0.1) / 0.1;
        double responseTimeScore = Math.max(0.0, 1.0 - (performanceMetrics.averageResponseTime() - 50.0) / 200.0);

        return (errorRateScore * 0.5) + (responseTimeScore * 0.5);
    }

    private double calculateOperationalScore() {
        if (operationalStatus.totalVehiclesInService() == 0) return 0.0;

        double serviceRatio = (double) operationalStatus.vehiclesInRoute() / operationalStatus.totalVehiclesInService();
        double gpsRatio = (double) operationalStatus.vehiclesWithRecentGps() / operationalStatus.totalVehiclesInService();

        return (serviceRatio * 0.6) + (gpsRatio * 0.4);
    }

    public HealthStatus getHealthStatus() {
        double score = getOverallHealthScore();

        if (score >= 0.9) return HealthStatus.EXCELLENT;
        if (score >= 0.75) return HealthStatus.GOOD;
        if (score >= 0.6) return HealthStatus.FAIR;
        if (score >= 0.4) return HealthStatus.POOR;
        return HealthStatus.CRITICAL;
    }

    public boolean requiresImmediateAttention() {
        return getHealthStatus() == HealthStatus.CRITICAL ||
                performanceMetrics.errorRate() > 0.15 ||
                systemOverview.vehiclesInBreakdown() > systemOverview.totalVehicles() * 0.1;
    }

    public java.util.List<String> getHealthAlerts() {
        var alerts = new java.util.ArrayList<String>();


        if (systemOverview.vehiclesInBreakdown() > 0) {
            alerts.add(String.format("%d vehicles in breakdown status", systemOverview.vehiclesInBreakdown()));
        }

        if (systemOverview.vehiclesNeedingMaintenance() > systemOverview.totalVehicles() * 0.2) {
            alerts.add("High maintenance backlog: " + systemOverview.vehiclesNeedingMaintenance() + " vehicles");
        }


        if (performanceMetrics.errorRate() > 0.1) {
            alerts.add(String.format("High error rate: %.1f%%", performanceMetrics.errorRate() * 100));
        }

        if (performanceMetrics.averageResponseTime() > 200.0) {
            alerts.add(String.format("Slow response time: %.0fms", performanceMetrics.averageResponseTime()));
        }


        if (operationalStatus.vehiclesWithRecentGps() < operationalStatus.totalVehiclesInService() * 0.8) {
            alerts.add("Low GPS coverage: " + operationalStatus.vehiclesWithRecentGps() + " of " +
                    operationalStatus.totalVehiclesInService() + " vehicles");
        }

        return alerts;
    }

    public String getHealthSummary() {
        return String.format("System Health: %s (%.1f%%) - %d vehicles active, %.1fms avg response, %.1f%% error rate",
                getHealthStatus().name(),
                getOverallHealthScore() * 100,
                systemOverview.activeVehicles(),
                performanceMetrics.averageResponseTime(),
                performanceMetrics.errorRate() * 100);
    }


    public record SystemOverview(
            int totalVehicles,
            int activeVehicles,
            int vehiclesInBreakdown,
            int vehiclesNeedingMaintenance,
            Map<VehicleType, Integer> vehiclesByType,
            Map<VehicleStatus, Integer> vehiclesByStatus
    ) {}

    public record PerformanceMetrics(
            long totalOperations,
            long successfulOperations,
            long errorOperations,
            double errorRate,
            double averageResponseTime
    ) {}

    public record OperationalStatus(
            int totalVehiclesInService,
            int vehiclesInRoute,
            int vehiclesWithRecentGps,
            int vehiclesWithoutGps,
            double gpsDataQuality
    ) {}

    @Getter
    public enum HealthStatus {
        EXCELLENT("Excellent", "#22c55e"),
        GOOD("Good", "#84cc16"),
        FAIR("Fair", "#eab308"),
        POOR("Poor", "#f97316"),
        CRITICAL("Critical", "#ef4444");

        private final String displayName;
        private final String colorCode;

        HealthStatus(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }

    }
}