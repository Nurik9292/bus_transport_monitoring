package tm.ugur.ugur_v3.domain.shared.valueobjects;

public record GeoBounds(double northLatitude, double southLatitude, double eastLongitude, double westLongitude) {
    public GeoBounds {
        if (northLatitude < southLatitude) {
            throw new IllegalArgumentException("North latitude must be greater than south latitude");
        }
        if (eastLongitude < westLongitude) {
            throw new IllegalArgumentException("East longitude must be greater than west longitude");
        }

    }

    public boolean contains(GeoCoordinate coordinate) {
        return coordinate.getLatitude() >= southLatitude &&
                coordinate.getLatitude() <= northLatitude &&
                coordinate.getLongitude() >= westLongitude &&
                coordinate.getLongitude() <= eastLongitude;
    }

    public GeoCoordinate getCenter() {
        double centerLat = (northLatitude + southLatitude) / 2.0;
        double centerLon = (eastLongitude + westLongitude) / 2.0;
        return new GeoCoordinate(centerLat, centerLon);
    }

    public double getAreaKm2() {
        double latDiff = northLatitude - southLatitude;
        double lonDiff = eastLongitude - westLongitude;
        return latDiff * lonDiff * 111.0 * 111.0;
    }

    @Override
    public String toString() {
        return String.format("GeoBounds(N:%.6f, S:%.6f, E:%.6f, W:%.6f)",
                northLatitude, southLatitude, eastLongitude, westLongitude);
    }
}
