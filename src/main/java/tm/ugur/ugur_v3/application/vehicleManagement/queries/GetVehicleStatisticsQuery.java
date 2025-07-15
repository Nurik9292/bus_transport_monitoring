package tm.ugur.ugur_v3.application.vehicleManagement.queries;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.Query;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Builder
public record GetVehicleStatisticsQuery(
        Optional<VehicleType> vehicleType,
        Optional<java.time.Instant> fromDate,
        Optional<java.time.Instant> toDate,
        boolean includeLocationStats,
        boolean includePerformanceStats,
        boolean includeUtilizationStats,
        boolean includeMaintenanceStats,
        boolean useCache
) implements Query {

    public static GetVehicleStatisticsQuery basic() {
        return GetVehicleStatisticsQuery.builder()
                .vehicleType(Optional.empty())
                .fromDate(Optional.empty())
                .toDate(Optional.empty())
                .includeLocationStats(false)
                .includePerformanceStats(false)
                .includeUtilizationStats(true)
                .includeMaintenanceStats(false)
                .useCache(true)
                .build();
    }

    public static GetVehicleStatisticsQuery detailed() {
        return GetVehicleStatisticsQuery.builder()
                .vehicleType(Optional.empty())
                .fromDate(Optional.empty())
                .toDate(Optional.empty())
                .includeLocationStats(true)
                .includePerformanceStats(true)
                .includeUtilizationStats(true)
                .includeMaintenanceStats(true)
                .useCache(true)
                .build();
    }

    public static GetVehicleStatisticsQuery forType(VehicleType vehicleType) {
        return GetVehicleStatisticsQuery.builder()
                .vehicleType(Optional.of(vehicleType))
                .fromDate(Optional.empty())
                .toDate(Optional.empty())
                .includeLocationStats(true)
                .includePerformanceStats(true)
                .includeUtilizationStats(true)
                .includeMaintenanceStats(true)
                .useCache(true)
                .build();
    }

    public static GetVehicleStatisticsQuery forPeriod(java.time.Instant fromDate,
                                                      java.time.Instant toDate) {
        return GetVehicleStatisticsQuery.builder()
                .vehicleType(Optional.empty())
                .fromDate(Optional.of(fromDate))
                .toDate(Optional.of(toDate))
                .includeLocationStats(true)
                .includePerformanceStats(true)
                .includeUtilizationStats(true)
                .includeMaintenanceStats(false)
                .useCache(true)
                .build();
    }

    public static GetVehicleStatisticsQuery realTime() {
        return GetVehicleStatisticsQuery.builder()
                .vehicleType(Optional.empty())
                .fromDate(Optional.empty())
                .toDate(Optional.empty())
                .includeLocationStats(true)
                .includePerformanceStats(false)
                .includeUtilizationStats(true)
                .includeMaintenanceStats(false)
                .useCache(false)
                .build();
    }

    public static GetVehicleStatisticsQuery dashboard() {
        return GetVehicleStatisticsQuery.builder()
                .vehicleType(Optional.empty())
                .fromDate(Optional.empty())
                .toDate(Optional.empty())
                .includeLocationStats(false)
                .includePerformanceStats(false)
                .includeUtilizationStats(true)
                .includeMaintenanceStats(true)
                .useCache(true)
                .build();
    }

    @Override
    public boolean isCacheable() {
        return useCache && !isRealTimeQuery();
    }

    @Override
    public java.time.Duration getCacheTtl() {
        if (hasTimeFilter()) {
            return java.time.Duration.ofMinutes(30);
        } else if (includePerformanceStats) {
            return java.time.Duration.ofMinutes(5);
        } else {
            return java.time.Duration.ofMinutes(10);
        }
    }

    @Override
    public java.time.Duration getTimeout() {

        if (isComplexQuery()) {
            return java.time.Duration.ofSeconds(30);
        } else {
            return java.time.Duration.ofSeconds(15);
        }
    }

    public boolean hasTimeFilter() {
        return fromDate.isPresent() || toDate.isPresent();
    }

    public boolean isRealTimeQuery() {
        return !useCache;
    }

    public boolean isComplexQuery() {
        int complexityScore = 0;
        if (includeLocationStats) complexityScore++;
        if (includePerformanceStats) complexityScore++;
        if (includeUtilizationStats) complexityScore++;
        if (includeMaintenanceStats) complexityScore++;
        if (hasTimeFilter()) complexityScore++;

        return complexityScore >= 3;
    }

    public boolean isTypeSpecific() {
        return vehicleType.isPresent();
    }

    public java.time.temporal.TemporalAmount getEffectiveDateRange() {
        if (fromDate.isPresent() && toDate.isPresent()) {
            return java.time.Duration.between(fromDate.get(), toDate.get());
        } else if (fromDate.isPresent()) {
            return java.time.Duration.between(fromDate.get(), java.time.Instant.now());
        } else {
            return java.time.Duration.ofDays(30);
        }
    }

    public String getQueryDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("GetVehicleStatistics[");

        vehicleType.ifPresent(type -> desc.append("type=").append(type).append(" "));

        if (hasTimeFilter()) {
            desc.append("period=");
            fromDate.ifPresent(from -> desc.append(from.toString().substring(0, 10)));
            desc.append(":");
            toDate.ifPresent(to -> desc.append(to.toString().substring(0, 10)));
            desc.append(" ");
        }

        List<String> includes = new ArrayList<>();
        if (includeLocationStats) includes.add("Location");
        if (includePerformanceStats) includes.add("Performance");
        if (includeUtilizationStats) includes.add("Utilization");
        if (includeMaintenanceStats) includes.add("Maintenance");

        if (!includes.isEmpty()) {
            desc.append("includes=").append(String.join(",", includes));
        }

        desc.append("]");
        return desc.toString();
    }

    public EstimatedResultSize getEstimatedResultSize() {
        if (isComplexQuery() && !isTypeSpecific()) {
            return EstimatedResultSize.LARGE;
        } else if (includePerformanceStats || includeLocationStats) {
            return EstimatedResultSize.MEDIUM;
        } else {
            return EstimatedResultSize.SMALL;
        }
    }

    public enum EstimatedResultSize {
        SMALL,
        MEDIUM,
        LARGE
    }
}