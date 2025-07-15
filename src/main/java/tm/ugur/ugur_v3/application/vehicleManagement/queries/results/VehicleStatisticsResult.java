package tm.ugur.ugur_v3.application.vehicleManagement.queries.results;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.QueryResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Builder
public record VehicleStatisticsResult(
        Duration queryTime,
        boolean fromCache,
        Instant timestamp,
        boolean includePerformanceStats,
        Optional<VehicleType> vehicleTypeFilter,

        int totalVehicles,
        int activeVehicles,
        int maintenanceVehicles,
        double utilizationRate,
        Map<VehicleStatus, Long> statusBreakdown,
        Map<VehicleType, Long> typeBreakdown,

        Double averageSpeedKmh,
        Double totalDistanceKm,
        Long totalOperatingHours,
        Integer maintenanceAlerts,

        String errorMessage
) implements QueryResult {

    public static VehicleStatisticsResult success(int totalVehicles,
                                                  int activeVehicles,
                                                  int maintenanceVehicles,
                                                  double utilizationRate,
                                                  Map<VehicleStatus, Long> statusBreakdown,
                                                  Map<VehicleType, Long> typeBreakdown,
                                                  boolean fromCache,
                                                  Duration queryTime,
                                                  boolean includePerformanceStats,
                                                  Optional<VehicleType> vehicleTypeFilter) {
        return VehicleStatisticsResult.builder()
                .queryTime(queryTime)
                .fromCache(fromCache)
                .timestamp(Instant.now())
                .includePerformanceStats(includePerformanceStats)
                .vehicleTypeFilter(vehicleTypeFilter)
                .totalVehicles(totalVehicles)
                .activeVehicles(activeVehicles)
                .maintenanceVehicles(maintenanceVehicles)
                .utilizationRate(utilizationRate)
                .statusBreakdown(statusBreakdown)
                .typeBreakdown(typeBreakdown)
                .build();
    }

    public static VehicleStatisticsResult withPerformance(VehicleStatisticsResult basicStats,
                                                          Double averageSpeedKmh,
                                                          Double totalDistanceKm,
                                                          Long totalOperatingHours,
                                                          Integer maintenanceAlerts) {
        return builder()
                .includePerformanceStats(true)
                .averageSpeedKmh(averageSpeedKmh)
                .totalDistanceKm(totalDistanceKm)
                .totalOperatingHours(totalOperatingHours)
                .maintenanceAlerts(maintenanceAlerts)
                .build();
    }

    public static VehicleStatisticsResult error(String errorMessage,
                                                Duration queryTime) {
        return VehicleStatisticsResult.builder()
                .queryTime(queryTime)
                .fromCache(false)
                .timestamp(Instant.now())
                .totalVehicles(0)
                .activeVehicles(0)
                .maintenanceVehicles(0)
                .utilizationRate(0.0)
                .statusBreakdown(Map.of())
                .typeBreakdown(Map.of())
                .includePerformanceStats(false)
                .vehicleTypeFilter(Optional.empty())
                .errorMessage(errorMessage)
                .build();
    }

    @Override
    public boolean isFromCache() {
        return fromCache;
    }

    @Override
    public Duration getQueryTime() {
        return queryTime;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("resultType", this.getClass().getSimpleName());
        metadata.put("fromCache", isFromCache());
        metadata.put("timestamp", getTimestamp());
        metadata.put("queryTime", getQueryTime().toMillis());
        metadata.put("includePerformanceStats", includePerformanceStats);
        metadata.put("totalVehicles", totalVehicles);
        metadata.put("activeVehicles", activeVehicles);
        metadata.put("maintenanceVehicles", maintenanceVehicles);
        metadata.put("utilizationRate", utilizationRate);
        metadata.put("activePercentage", getActiveVehiclePercentage());
        metadata.put("maintenancePercentage", getMaintenanceVehiclePercentage());
        metadata.put("wasPerformant", wasPerformant());
        metadata.put("needsAttention", needsAttention());
        metadata.put("hasPerformanceData", hasPerformanceData());

        vehicleTypeFilter.ifPresent(type -> metadata.put("vehicleTypeFilter", type.name()));

        if (hasPerformanceData()) {
            metadata.put("averageSpeedKmh", averageSpeedKmh);
            if (totalDistanceKm != null) metadata.put("totalDistanceKm", totalDistanceKm);
            if (totalOperatingHours != null) metadata.put("totalOperatingHours", totalOperatingHours);
            if (maintenanceAlerts != null) metadata.put("maintenanceAlerts", maintenanceAlerts);
        }

        statusBreakdown.forEach((status, count) ->
                metadata.put("status_" + status.name().toLowerCase(), count));
        typeBreakdown.forEach((type, count) ->
                metadata.put("type_" + type.name().toLowerCase(), count));

        if (errorMessage != null) {
            metadata.put("errorMessage", errorMessage);
        }

        return metadata;
    }

    @Override
    public boolean shouldCache() {
        return !includePerformanceStats && errorMessage == null;
    }

    public boolean isSuccess() {
        return errorMessage == null;
    }

    public boolean wasPerformant() {
        return queryTime.toMillis() < (includePerformanceStats ? 500 : 300);
    }

    public String getCacheStatus() {
        return fromCache ? "HIT" : "MISS";
    }

    public double getActiveVehiclePercentage() {
        return totalVehicles > 0 ? (double) activeVehicles / totalVehicles * 100.0 : 0.0;
    }

    public double getMaintenanceVehiclePercentage() {
        return totalVehicles > 0 ? (double) maintenanceVehicles / totalVehicles * 100.0 : 0.0;
    }

    public boolean hasPerformanceData() {
        return includePerformanceStats && averageSpeedKmh != null;
    }

    public String getStatisticsSummary() {
        if (errorMessage != null) {
            return "ERROR: " + errorMessage;
        }

        StringBuilder summary = new StringBuilder();
        String filter = vehicleTypeFilter.map(t -> "[" + t.name() + "] ").orElse("");

        summary.append(String.format("%sFleet: %d total, %d active (%.1f%% utilization)",
                filter, totalVehicles, activeVehicles, utilizationRate * 100));

        if (hasPerformanceData()) {
            summary.append(String.format(", avg speed: %.1f km/h", averageSpeedKmh));
            if (totalDistanceKm != null) {
                summary.append(String.format(", total: %.1f km", totalDistanceKm));
            }
        }

        summary.append(String.format(" (%s)", getCacheStatus()));
        return summary.toString();
    }

    public boolean needsAttention() {
        return maintenanceAlerts != null && maintenanceAlerts > 0 ||
                utilizationRate < 0.5 ||
                getMaintenanceVehiclePercentage() > 20.0;
    }
}