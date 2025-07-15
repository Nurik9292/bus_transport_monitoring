package tm.ugur.ugur_v3.domain.shared.exceptions;

import lombok.Getter;

import java.util.Map;

@Getter
public final class InvalidGeoCoordinateException extends DomainException {

    private final Double latitude;
    private final Double longitude;
    private final Double accuracy;

    public InvalidGeoCoordinateException(String message) {
        super(message, "INVALID_GEO_COORDINATE");
        this.latitude = null;
        this.longitude = null;
        this.accuracy = null;
    }

    public InvalidGeoCoordinateException(String message, double latitude, double longitude) {
        super(
                String.format("%s (lat=%.6f, lng=%.6f)", message, latitude, longitude),
                "INVALID_GEO_COORDINATE",
                Map.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "validationFailed", true
                )
        );
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = null;
    }

    public InvalidGeoCoordinateException(String message, double latitude, double longitude, double accuracy) {
        super(
                String.format("%s (lat=%.6f, lng=%.6f, accuracy=%.1fm)", message, latitude, longitude, accuracy),
                "INVALID_GEO_COORDINATE",
                Map.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "accuracy", accuracy,
                        "validationFailed", true
                )
        );
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    public static InvalidGeoCoordinateException latitudeOutOfRange(double latitude) {
        return new InvalidGeoCoordinateException(
                String.format("Latitude out of range: %.6f (must be between -90 and 90)", latitude),
                latitude, 0.0
        );
    }

    public static InvalidGeoCoordinateException longitudeOutOfRange(double longitude) {
        return new InvalidGeoCoordinateException(
                String.format("Longitude out of range: %.6f (must be between -180 and 180)", longitude),
                0.0, longitude
        );
    }

    public static InvalidGeoCoordinateException bothCoordinatesInvalid(double latitude, double longitude) {
        return new InvalidGeoCoordinateException(
                "Both coordinates are out of valid range", latitude, longitude
        );
    }

    public static InvalidGeoCoordinateException accuracyTooLow(double latitude, double longitude, double accuracy, double minRequired) {
        return new InvalidGeoCoordinateException(
                String.format("GPS accuracy too low: %.1fm (minimum required: %.1fm)", accuracy, minRequired),
                latitude, longitude, accuracy
        );
    }

    public static InvalidGeoCoordinateException nullCoordinates() {
        return new InvalidGeoCoordinateException("Coordinates cannot be null");
    }

    public static InvalidGeoCoordinateException invalidFormat(String coordinateString) {
        return new InvalidGeoCoordinateException(
                String.format("Invalid coordinate format: %s", coordinateString)
        );
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public boolean hasAccuracy() {
        return accuracy != null;
    }

    public String getFailureReason() {
        if (!hasCoordinates()) {
            return "MISSING_COORDINATES";
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            return "INVALID_LATITUDE";
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            return "INVALID_LONGITUDE";
        }
        if (hasAccuracy() && accuracy < 0) {
            return "NEGATIVE_ACCURACY";
        }
        return "UNKNOWN";
    }
}