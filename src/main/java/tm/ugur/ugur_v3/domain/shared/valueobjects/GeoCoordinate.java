package tm.ugur.ugur_v3.domain.shared.valueobjects;

public class GeoCoordinate {

    private final double latitude;
    private final double longitude;
    private final Double altitude;
    private final Double accuracy;

    public GeoCoordinate(double latitude, double longitude) {
        this(latitude, longitude, null, null);
    }

    public GeoCoordinate(double latitude, double longitude, Double altitude, Double accuracy) {
        validateLatitude(latitude);
        validateLongitude(longitude);
        validateAltitude(altitude);
        validateAccuracy(accuracy);

        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.accuracy = accuracy;
    }


    public double distanceTo(GeoCoordinate other) {
        return calculateHaversineDistance(this.latitude, this.longitude,
                other.latitude, other.longitude);
    }


    public boolean isWithinBounds(GeoBounds bounds) {
        return bounds.contains(this);
    }


    public boolean isValidForTracking() {
        return latitude != 0.0 && longitude != 0.0 &&
                (accuracy == null || accuracy <= 100.0);
    }

    private static void validateLatitude(double latitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
    }

    private static void validateLongitude(double longitude) {
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
    }

    private static void validateAltitude(Double altitude) {
        if (altitude != null && (altitude < -500.0 || altitude > 10000.0)) {
            throw new IllegalArgumentException("Altitude must be between -500 and 10000 meters");
        }
    }

    private static void validateAccuracy(Double accuracy) {
        if (accuracy != null && accuracy < 0.0) {
            throw new IllegalArgumentException("Accuracy cannot be negative");
        }
    }

    private static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    @Override
    public String toString() {
        return String.format("GeoCoordinate(%.6f, %.6f)", latitude, longitude);
    }
}
