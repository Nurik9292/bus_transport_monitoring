package tm.ugur.ugur_v3.domain.shared.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidGeoCoordinateException;


@Getter
public final class GeoCoordinate extends ValueObject {

    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LATITUDE = 90.0;
    public static final double MIN_LONGITUDE = -180.0;
    public static final double MAX_LONGITUDE = 180.0;
    public static final double MIN_ACCURACY = 0.0;
    public static final double MAX_ACCURACY = 10000.0;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

    private static final double MAX_ACCURACY_METERS = 10.0;
    private static final double IS_HIGHLY_ACCURATE = 3.0;

    private final double latitude;
    private final double longitude;
    private final double accuracy;
    private final double altitude;
    private final long timestamp;

    private transient volatile Double latitudeRadians;
    private transient volatile Double longitudeRadians;


    private GeoCoordinate(double latitude, double longitude, double accuracy, double altitude, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.altitude = altitude;
        this.timestamp = timestamp;
        validate();

    }

    public static GeoCoordinate of(double latitude, double longitude, double accuracy) {
        return new GeoCoordinate(latitude, longitude, accuracy, 0.0, System.currentTimeMillis());
    }

    public static GeoCoordinate of(double latitude, double longitude, double accuracy, double altitude) {
        return new GeoCoordinate(latitude, longitude, accuracy,  altitude, System.currentTimeMillis());
    }

    public static GeoCoordinate of(double latitude, double longitude, double accuracy, long timestamp) {
        return new GeoCoordinate(latitude, longitude, accuracy, 0.0, timestamp);
    }

    public static GeoCoordinate of(double latitude, double longitude) {
        return new GeoCoordinate(latitude, longitude, 0.0, 0.0, System.currentTimeMillis());
    }

    @Override
    protected void validate() {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new InvalidGeoCoordinateException(
                    String.format("Latitude must be between %.1f and %.1f, got: %.6f",
                            MIN_LATITUDE, MAX_LATITUDE, latitude));
        }

        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new InvalidGeoCoordinateException(
                    String.format("Longitude must be between %.1f and %.1f, got: %.6f",
                            MIN_LONGITUDE, MAX_LONGITUDE, longitude));
        }

        if (accuracy < MIN_ACCURACY || accuracy > MAX_ACCURACY) {
            throw new InvalidGeoCoordinateException(
                    String.format("Accuracy must be between %.1f and %.1f meters, got: %.2f",
                            MIN_ACCURACY, MAX_ACCURACY, accuracy));
        }

        if (timestamp <= 0) {
            throw new InvalidGeoCoordinateException("Timestamp must be positive");
        }
    }


    public double distanceTo(GeoCoordinate other) {
        double lat1Rad = getLatitudeRadians();
        double lat2Rad = other.getLatitudeRadians();
        double deltaLatRad = lat2Rad - lat1Rad;
        double deltaLonRad = other.getLongitudeRadians() - getLongitudeRadians();

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c * 1000;
    }

    public boolean isWithinRadius(GeoCoordinate center, double radiusMeters) {
        return distanceTo(center) <= radiusMeters;
    }

    public double bearingTo(GeoCoordinate other) {
        double lat1Rad = getLatitudeRadians();
        double lat2Rad = other.getLatitudeRadians();
        double deltaLonRad = other.getLongitudeRadians() - getLongitudeRadians();

        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);

        double bearingRad = Math.atan2(y, x);
        return Math.toDegrees(bearingRad);
    }

    public boolean isAccurate() {
        return accuracy <= MAX_ACCURACY_METERS;
    }

    public boolean isStale(long maxAgeMillis) {
        return (System.currentTimeMillis() - timestamp) > maxAgeMillis;
    }


    private double getLatitudeRadians() {
        if (latitudeRadians == null) {
            latitudeRadians = latitude * DEGREES_TO_RADIANS;
        }
        return latitudeRadians;
    }

    private double getLongitudeRadians() {
        if (longitudeRadians == null) {
            longitudeRadians = longitude * DEGREES_TO_RADIANS;
        }
        return longitudeRadians;
    }


    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{latitude, longitude, accuracy, altitude, timestamp};
    }

    @Override
    public String toString() {
        return String.format("GeoCoordinate{lat=%.6f, lon=%.6f, acc=%.2fm, alt=%.6f ts=%d}",
                latitude, longitude, accuracy, altitude, timestamp);
    }
}