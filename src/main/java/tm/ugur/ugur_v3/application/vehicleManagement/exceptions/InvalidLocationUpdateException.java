package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;

import java.time.Instant;
import java.util.Map;

@Getter
public final class InvalidLocationUpdateException extends VehicleManagementException {

    private final GeoCoordinate location;
    private final Instant timestamp;
    private final String validationFailure;

    public InvalidLocationUpdateException(String message, VehicleId vehicleId) {
        super(message, "INVALID_LOCATION_UPDATE", vehicleId);
        this.location = null;
        this.timestamp = Instant.now();
        this.validationFailure = "UNKNOWN";
    }

    public InvalidLocationUpdateException(String message, VehicleId vehicleId, GeoCoordinate location) {
        super(
                String.format("%s (location: %s)", message, location != null ? location.toString() : "null"),
                "INVALID_LOCATION_UPDATE",
                vehicleId
        );
        this.location = location;
        this.timestamp = Instant.now();
        this.validationFailure = "COORDINATE_VALIDATION";
    }

    public InvalidLocationUpdateException(String message, VehicleId vehicleId,
                                          GeoCoordinate location, String validationFailure) {
        super(
                String.format("%s (location: %s, reason: %s)",
                        message,
                        location != null ? location.toString() : "null",
                        validationFailure),
                "INVALID_LOCATION_UPDATE",
                vehicleId
        );
        this.location = location;
        this.timestamp = Instant.now();
        this.validationFailure = validationFailure;
    }

    public static InvalidLocationUpdateException coordinatesOutOfRange(VehicleId vehicleId,
                                                                       double lat, double lng) {
        return new InvalidLocationUpdateException(
                String.format("Coordinates out of valid range: lat=%.6f, lng=%.6f", lat, lng),
                vehicleId,
                null,
                "COORDINATES_OUT_OF_RANGE"
        );
    }

    public static InvalidLocationUpdateException jumpTooLarge(VehicleId vehicleId,
                                                              GeoCoordinate previous,
                                                              GeoCoordinate current,
                                                              double distanceKm) {
        return new InvalidLocationUpdateException(
                String.format("Location jump too large: %.2f km (previous: %s, current: %s)",
                        distanceKm, previous, current),
                vehicleId,
                current,
                "JUMP_TOO_LARGE"
        );
    }

    public static InvalidLocationUpdateException timestampTooOld(VehicleId vehicleId,
                                                                 GeoCoordinate location,
                                                                 Instant updateTime) {
        return new InvalidLocationUpdateException(
                String.format("GPS timestamp too old: %s", updateTime),
                vehicleId,
                location,
                "TIMESTAMP_TOO_OLD"
        );
    }

    public static InvalidLocationUpdateException accuracyTooLow(VehicleId vehicleId,
                                                                GeoCoordinate location,
                                                                double accuracy,
                                                                double minRequired) {
        return new InvalidLocationUpdateException(
                String.format("GPS accuracy too low: %.1fm (minimum: %.1fm)", accuracy, minRequired),
                vehicleId,
                location,
                "ACCURACY_TOO_LOW"
        );
    }

    public static InvalidLocationUpdateException vehicleNotTrackable(VehicleId vehicleId, String status) {
        return new InvalidLocationUpdateException(
                String.format("Vehicle not trackable in current status: %s", status),
                vehicleId,
                null,
                "VEHICLE_NOT_TRACKABLE"
        );
    }

    public static InvalidLocationUpdateException duplicateLocation(VehicleId vehicleId, GeoCoordinate location) {
        return new InvalidLocationUpdateException(
                "Duplicate location update ignored",
                vehicleId,
                location,
                "DUPLICATE_LOCATION"
        );
    }

    public boolean hasLocation() {
        return location != null;
    }

    @Override
    public String getErrorCategory() {
        return "LOCATION_UPDATE_ERROR";
    }

    public Map<String, Object> getLocationContext() {
        if (!hasLocation()) {
            return Map.of("validationFailure", validationFailure);
        }

        return Map.of(
                "latitude", location.getLatitude(),
                "longitude", location.getLongitude(),
                "accuracy", location.getAccuracy(),
                "validationFailure", validationFailure,
                "timestamp", timestamp.toString()
        );
    }
}