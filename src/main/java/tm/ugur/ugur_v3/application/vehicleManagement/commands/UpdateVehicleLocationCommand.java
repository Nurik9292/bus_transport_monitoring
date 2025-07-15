package tm.ugur.ugur_v3.application.vehicleManagement.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.Command;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Builder
public record UpdateVehicleLocationCommand(
        @NotNull(message = "Vehicle ID cannot be null")
        @Valid
        VehicleId vehicleId,

        @NotNull(message = "Location cannot be null")
        @Valid
        GeoCoordinate location,

        @DecimalMin(value = "0.0", message = "Speed cannot be negative")
        @DecimalMax(value = "300.0", message = "Speed cannot exceed 300 km/h")
        Double speedKmh,

        @DecimalMin(value = "0.0", message = "Bearing must be 0-360 degrees")
        @DecimalMax(value = "360.0", message = "Bearing must be 0-360 degrees")
        Double bearingDegrees,

        @NotNull(message = "Timestamp cannot be null")
        @PastOrPresent(message = "Timestamp cannot be in the future")
        java.time.Instant timestamp
) implements Command {

    public static UpdateVehicleLocationCommand create(VehicleId vehicleId,
                                                      GeoCoordinate location,
                                                      Double speed,
                                                      Double bearing) {
        return UpdateVehicleLocationCommand.builder()
                .vehicleId(vehicleId)
                .location(location)
                .speedKmh(speed)
                .bearingDegrees(bearing)
                .timestamp(java.time.Instant.now())
                .build();
    }

    public static UpdateVehicleLocationCommand fromGpsApi(VehicleId vehicleId,
                                                          double latitude,
                                                          double longitude,
                                                          double accuracy,
                                                          Double speed,
                                                          Double bearing) {
        GeoCoordinate location =  GeoCoordinate.of(latitude, longitude, accuracy);
        return create(vehicleId, location, speed, bearing);
    }

    @Override
    public CommandPriority getPriority() {
        return CommandPriority.HIGH;
    }

    public boolean isGpsDataRecent(java.time.Duration maxAge) {
        return timestamp.isAfter(java.time.Instant.now().minus(maxAge));
    }

    public boolean isSignificantLocationChange(GeoCoordinate previousLocation) {
        if (previousLocation == null) {
            return true; // First location is always significant
        }

        double distanceMeters = previousLocation.distanceTo(location);
        return distanceMeters > 10.0; // More than 10 meters is significant
    }
}