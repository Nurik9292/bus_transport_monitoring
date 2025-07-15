package tm.ugur.ugur_v3.application.vehicleManagement.queries;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tm.ugur.ugur_v3.application.shared.commands.Query;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.time.Duration;

public record GetVehicleByIdQuery(
        @NotNull(message = "Vehicle ID cannot be null")
        @Valid
        VehicleId vehicleId,

        boolean includeLocationHistory,
        boolean useCache
) implements Query {

    public static GetVehicleByIdQuery simple(VehicleId vehicleId) {
        return new GetVehicleByIdQuery(vehicleId, false, true);
    }

    public static GetVehicleByIdQuery withHistory(VehicleId vehicleId) {
        return new GetVehicleByIdQuery(vehicleId, true, true);
    }

    public static GetVehicleByIdQuery realTime(VehicleId vehicleId) {
        return new GetVehicleByIdQuery(vehicleId, false, false);
    }

    @Override
    public boolean isCacheable() {
        return useCache;
    }

    @Override
    public Duration getCacheTtl() {
        if (!useCache) {
            return Duration.ZERO;
        }

        return includeLocationHistory ?
                Duration.ofMinutes(2) :
                Duration.ofMinutes(5);
    }

    @Override
    public Duration getTimeout() {
        return includeLocationHistory ?
                Duration.ofSeconds(10) :
                Duration.ofSeconds(5);
    }
}
