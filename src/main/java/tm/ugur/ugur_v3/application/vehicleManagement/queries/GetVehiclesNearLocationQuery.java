package tm.ugur.ugur_v3.application.vehicleManagement.queries;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.Query;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

@Builder
public record GetVehiclesNearLocationQuery(
        @NotNull(message = "Location cannot be null")
        @Valid
        GeoCoordinate location,

        @DecimalMin(value = "0.1", message = "Radius must be at least 0.1 km")
        @DecimalMax(value = "100.0", message = "Radius cannot exceed 100 km")
        double radiusKm,

        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 500, message = "Limit cannot exceed 500")
        int limit,

        java.util.Optional<VehicleType> vehicleType,
        boolean includeInactive,
        boolean requireRecentGps,
        boolean includeDistance
) implements Query {

    public static GetVehiclesNearLocationQuery standard(GeoCoordinate location, double radiusKm) {
        return GetVehiclesNearLocationQuery.builder()
                .location(location)
                .radiusKm(radiusKm)
                .limit(20)
                .vehicleType(java.util.Optional.empty())
                .includeInactive(false)
                .requireRecentGps(true)
                .includeDistance(true)
                .build();
    }

    public static GetVehiclesNearLocationQuery emergency(GeoCoordinate location) {
        return GetVehiclesNearLocationQuery.builder()
                .location(location)
                .radiusKm(5.0)
                .limit(10)
                .vehicleType(java.util.Optional.empty())
                .includeInactive(false)
                .requireRecentGps(true)
                .includeDistance(true)
                .build();
    }

    public static GetVehiclesNearLocationQuery byType(GeoCoordinate location,
                                                      double radiusKm,
                                                      VehicleType vehicleType,
                                                      int limit) {
        return GetVehiclesNearLocationQuery.builder()
                .location(location)
                .radiusKm(radiusKm)
                .limit(limit)
                .vehicleType(java.util.Optional.of(vehicleType))
                .includeInactive(false)
                .requireRecentGps(true)
                .includeDistance(true)
                .build();
    }

    public static GetVehiclesNearLocationQuery comprehensive(GeoCoordinate location, double radiusKm) {
        return GetVehiclesNearLocationQuery.builder()
                .location(location)
                .radiusKm(radiusKm)
                .limit(100)
                .vehicleType(java.util.Optional.empty())
                .includeInactive(true)
                .requireRecentGps(false)
                .includeDistance(true)
                .build();
    }

    @Override
    public boolean isCacheable() {

        return false;
    }

    @Override
    public java.time.Duration getCacheTtl() {
        return java.time.Duration.ofSeconds(30);
    }

    @Override
    public java.time.Duration getTimeout() {

        return java.time.Duration.ofSeconds(20);
    }

    public boolean isWideAreaSearch() {
        return radiusKm > 50.0;
    }

    public boolean isEmergencySearch() {
        return radiusKm <= 5.0 && limit <= 10 && requireRecentGps;
    }

    public boolean hasTypeFilter() {
        return vehicleType.isPresent();
    }

    public double getSearchAreaSqKm() {
        return Math.PI * radiusKm * radiusKm;
    }

    public String getQueryDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("GetVehiclesNearLocation[")
                .append(String.format("%.6f,%.6f", location.getLatitude(), location.getLongitude()))
                .append(" r=").append(radiusKm).append("km")
                .append(" limit=").append(limit);

        vehicleType.ifPresent(type -> desc.append(" type=").append(type));

        if (requireRecentGps) desc.append(" +GPS");
        if (includeInactive) desc.append(" +Inactive");
        if (includeDistance) desc.append(" +Distance");

        desc.append("]");
        return desc.toString();
    }

    public QueryComplexity getEstimatedComplexity() {
        if (isEmergencySearch()) {
            return QueryComplexity.LOW;
        } else if (isWideAreaSearch() || limit > 100) {
            return QueryComplexity.HIGH;
        } else {
            return QueryComplexity.MEDIUM;
        }
    }

    public enum QueryComplexity {
        LOW,
        MEDIUM,
        HIGH
    }
}