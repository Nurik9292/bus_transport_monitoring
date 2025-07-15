package tm.ugur.ugur_v3.application.vehicleManagement.queries.results;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.QueryResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.LicensePlate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Builder
public record FindVehicleByLicensePlateResult(
        LicensePlate searchedLicensePlate,
        Vehicle vehicle,
        boolean vehicleFound,
        Duration queryTime,
        boolean fromCache,
        Instant timestamp,
        String errorMessage
) implements QueryResult {

    public static FindVehicleByLicensePlateResult found(Vehicle vehicle, boolean fromCache, Duration queryTime) {
        return FindVehicleByLicensePlateResult.builder()
                .searchedLicensePlate(vehicle.getLicensePlate())
                .vehicle(vehicle)
                .vehicleFound(true)
                .fromCache(fromCache)
                .queryTime(queryTime)
                .timestamp(Instant.now())
                .build();
    }

    public static FindVehicleByLicensePlateResult notFound(LicensePlate searchedLicensePlate) {
        return FindVehicleByLicensePlateResult.builder()
                .searchedLicensePlate(searchedLicensePlate)
                .vehicle(null)
                .vehicleFound(false)
                .fromCache(false)
                .queryTime(Duration.ZERO)
                .timestamp(Instant.now())
                .build();
    }

    public static FindVehicleByLicensePlateResult error(LicensePlate searchedLicensePlate,
                                                        String errorMessage,
                                                        Duration queryTime) {
        return FindVehicleByLicensePlateResult.builder()
                .searchedLicensePlate(searchedLicensePlate)
                .vehicle(null)
                .vehicleFound(false)
                .fromCache(false)
                .queryTime(queryTime)
                .timestamp(Instant.now())
                .errorMessage(errorMessage)
                .build();
    }

    public boolean isFound() {
        return vehicleFound;
    }

    public Optional<Vehicle> getVehicleOptional() {
        return vehicleFound ? Optional.ofNullable(vehicle) : Optional.empty();
    }

    public Vehicle getVehicleOrNull() {
        return vehicleFound ? vehicle : null;
    }

    public boolean hasVehicle() {
        return vehicleFound && vehicle != null;
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
    public boolean shouldCache() {
        return vehicleFound && errorMessage == null;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("resultType", this.getClass().getSimpleName());
        metadata.put("fromCache", isFromCache());
        metadata.put("timestamp", getTimestamp());
        metadata.put("queryTime", getQueryTime().toMillis());
        metadata.put("vehicleFound", vehicleFound);

        if (searchedLicensePlate != null) {
            metadata.put("searchedLicensePlate", searchedLicensePlate.getValue());
        }

        if (vehicle() != null) {  // record автоматически создает этот метод
            metadata.put("vehicleId", vehicle().getId().getValue());
            metadata.put("vehicleLicensePlate", vehicle().getLicensePlate().getValue());
        }

        if (errorMessage != null) {
            metadata.put("errorMessage", errorMessage);
        }

        return metadata;
    }

    public String getSearchedLicensePlateValue() {
        return searchedLicensePlate != null ? searchedLicensePlate.getValue() : null;
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    public String getResultSummary() {
        if (hasError()) {
            return "ERROR: " + errorMessage;
        }

        if (vehicleFound && vehicle() != null) {
            return String.format("Found vehicle %s with license plate %s",
                    vehicle().getId().getValue(),
                    vehicle().getLicensePlate().getValue());
        }

        return "Vehicle with license plate " + getSearchedLicensePlateValue() + " not found";
    }

    public Vehicle getVehicleOrThrow() {
        if (!vehicleFound || vehicle() == null) {
            throw new IllegalStateException("Vehicle not found or result is in error state");
        }
        return vehicle();
    }
}