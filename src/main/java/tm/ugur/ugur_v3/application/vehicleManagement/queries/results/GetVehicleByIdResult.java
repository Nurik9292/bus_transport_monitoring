package tm.ugur.ugur_v3.application.vehicleManagement.queries.results;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.QueryResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;

@Builder
public record GetVehicleByIdResult(
        Vehicle vehicle,
        boolean fromCache,
        java.time.Duration queryTime,
        boolean includesHistory
) implements QueryResult {

    public static GetVehicleByIdResult found(Vehicle vehicle,
                                             boolean fromCache,
                                             java.time.Duration queryTime) {
        return GetVehicleByIdResult.builder()
                .vehicle(vehicle)
                .fromCache(fromCache)
                .queryTime(queryTime)
                .includesHistory(false)
                .build();
    }

    public static GetVehicleByIdResult foundWithHistory(Vehicle vehicle,
                                                        boolean fromCache,
                                                        java.time.Duration queryTime) {
        return GetVehicleByIdResult.builder()
                .vehicle(vehicle)
                .fromCache(fromCache)
                .queryTime(queryTime)
                .includesHistory(true)
                .build();
    }

    public static GetVehicleByIdResult notFound() {
        return GetVehicleByIdResult.builder()
                .vehicle(null)
                .fromCache(false)
                .queryTime(java.time.Duration.ZERO)
                .includesHistory(false)
                .build();
    }

    public boolean isFound() {
        return vehicle != null;
    }

    @Override
    public boolean isFromCache() {
        return fromCache;
    }

    @Override
    public java.time.Duration getQueryTime() {
        return queryTime;
    }

    @Override
    public boolean shouldCache() {
        return isFound();
    }

    public boolean wasPerformant() {
        return queryTime.toMillis() < 50;
    }

    public String getVehicleSummary() {
        if (!isFound()) {
            return "VEHICLE_NOT_FOUND";
        }

        return String.format("Vehicle[id=%s, status=%s, type=%s]",
                vehicle.getId().getValue(),
                vehicle.getStatus().name(),
                vehicle.getVehicleType().name());
    }
}