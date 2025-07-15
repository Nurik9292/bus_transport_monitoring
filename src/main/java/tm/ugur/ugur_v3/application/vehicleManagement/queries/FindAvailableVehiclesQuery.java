package tm.ugur.ugur_v3.application.vehicleManagement.queries;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.Query;
import tm.ugur.ugur_v3.application.shared.pagination.PageRequest;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

@Builder
public record FindAvailableVehiclesQuery(
        @NotNull(message = "Page request cannot be null")
        @Valid
        PageRequest pageRequest,

        java.util.Optional<VehicleType> vehicleType,
        java.util.Optional<Integer> minimumCapacity,
        java.util.Optional<GeoCoordinate> nearLocation,

        @DecimalMin(value = "0.1", message = "Distance must be positive")
        @DecimalMax(value = "100.0", message = "Distance cannot exceed 100km")
        double maxDistanceKm,

        boolean requireRecentGps
) implements Query {

    public static FindAvailableVehiclesQuery all(PageRequest pageRequest) {
        return FindAvailableVehiclesQuery.builder()
                .pageRequest(pageRequest)
                .vehicleType(java.util.Optional.empty())
                .minimumCapacity(java.util.Optional.empty())
                .nearLocation(java.util.Optional.empty())
                .maxDistanceKm(50.0)
                .requireRecentGps(false)
                .build();
    }

    public static FindAvailableVehiclesQuery byType(PageRequest pageRequest, VehicleType vehicleType) {
        return FindAvailableVehiclesQuery.builder()
                .pageRequest(pageRequest)
                .vehicleType(java.util.Optional.of(vehicleType))
                .minimumCapacity(java.util.Optional.empty())
                .nearLocation(java.util.Optional.empty())
                .maxDistanceKm(50.0)
                .requireRecentGps(true)
                .build();
    }

    public static FindAvailableVehiclesQuery nearLocation(PageRequest pageRequest,
                                                          GeoCoordinate location,
                                                          double radiusKm) {
        return FindAvailableVehiclesQuery.builder()
                .pageRequest(pageRequest)
                .vehicleType(java.util.Optional.empty())
                .minimumCapacity(java.util.Optional.empty())
                .nearLocation(java.util.Optional.of(location))
                .maxDistanceKm(radiusKm)
                .requireRecentGps(true)
                .build();
    }

    public static FindAvailableVehiclesQuery withMinimumCapacity(PageRequest pageRequest,
                                                                 int minimumCapacity) {
        return FindAvailableVehiclesQuery.builder()
                .pageRequest(pageRequest)
                .vehicleType(java.util.Optional.empty())
                .minimumCapacity(java.util.Optional.of(minimumCapacity))
                .nearLocation(java.util.Optional.empty())
                .maxDistanceKm(50.0)
                .requireRecentGps(false)
                .build();
    }

    @Override
    public boolean isCacheable() {
        return nearLocation.isEmpty();
    }

    @Override
    public java.time.Duration getCacheTtl() {
        if (nearLocation.isPresent()) {
            return java.time.Duration.ofSeconds(30);
        }
        return java.time.Duration.ofMinutes(2);
    }

    @Override
    public java.time.Duration getTimeout() {
        return nearLocation.isPresent() ?
                java.time.Duration.ofSeconds(15) :
                java.time.Duration.ofSeconds(10);
    }

    public boolean isLocationBasedSearch() {
        return nearLocation.isPresent();
    }

    public boolean hasFiltersApplied() {
        return vehicleType.isPresent() ||
                minimumCapacity.isPresent() ||
                nearLocation.isPresent() ||
                requireRecentGps;
    }


}