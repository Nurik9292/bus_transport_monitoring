package tm.ugur.ugur_v3.domain.geospatial.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class BoundingBox extends ValueObject {


    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;


    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final double MIN_LONGITUDE = -180.0;

    private final GeoCoordinate southWest;
    private final GeoCoordinate northEast;

    private BoundingBox(GeoCoordinate southWest, GeoCoordinate northEast) {
        validateBounds(southWest, northEast);
        this.southWest = southWest;
        this.northEast = northEast;
    }


    public static BoundingBox of(GeoCoordinate southWest, GeoCoordinate northEast) {
        return new BoundingBox(southWest, northEast);
    }

    public static BoundingBox ofCorners(double swLat, double swLng, double neLat, double neLng) {
        GeoCoordinate sw = GeoCoordinate.of(swLat, swLng);
        GeoCoordinate ne = GeoCoordinate.of(neLat, neLng);
        return new BoundingBox(sw, ne);
    }


    public static BoundingBox fromCenterAndRadius(GeoCoordinate center, Distance radius) {
        double radiusMeters = radius.toMeters();
        double lat = center.getLatitude();
        double lng = center.getLongitude();


        double latOffset = radiusMeters / EARTH_RADIUS_METERS * (180.0 / Math.PI);


        double lngOffset = latOffset / Math.cos(lat * DEGREES_TO_RADIANS);

        GeoCoordinate southWest = GeoCoordinate.of(lat - latOffset, lng - lngOffset);
        GeoCoordinate northEast = GeoCoordinate.of(lat + latOffset, lng + lngOffset);

        return new BoundingBox(southWest, northEast);
    }


    public static BoundingBox fromPoints(List<GeoCoordinate> points) {
        if (points == null || points.isEmpty()) {
            throw new BusinessRuleViolationException("EMPTY_POINTS_LIST",
                    "Cannot create bounding box from empty points list");
        }

        if (points.size() == 1) {
            GeoCoordinate point = points.getFirst();
            return new BoundingBox(point, point);
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;

        for (GeoCoordinate point : points) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLng = Math.min(minLng, point.getLongitude());
            maxLng = Math.max(maxLng, point.getLongitude());
        }

        return ofCorners(minLat, minLng, maxLat, maxLng);
    }


    public GeoCoordinate getNorthWest() {
        return GeoCoordinate.of(northEast.getLatitude(), southWest.getLongitude());
    }

    public GeoCoordinate getSouthEast() {
        return GeoCoordinate.of(southWest.getLatitude(), northEast.getLongitude());
    }

    public GeoCoordinate getCenter() {
        double centerLat = (southWest.getLatitude() + northEast.getLatitude()) / 2.0;
        double centerLng = (southWest.getLongitude() + northEast.getLongitude()) / 2.0;
        return GeoCoordinate.of(centerLat, centerLng);
    }

    public List<GeoCoordinate> getCorners() {
        List<GeoCoordinate> corners = new ArrayList<>(4);
        corners.add(southWest);
        corners.add(getSouthEast());
        corners.add(northEast);
        corners.add(getNorthWest());
        return corners;
    }


    public double getWidth() {
        return northEast.getLongitude() - southWest.getLongitude();
    }

    public double getHeight() {
        return northEast.getLatitude() - southWest.getLatitude();
    }

    public Distance getWidthDistance() {
        GeoCoordinate west = GeoCoordinate.of(getCenter().getLatitude(), southWest.getLongitude());
        GeoCoordinate east = GeoCoordinate.of(getCenter().getLatitude(), northEast.getLongitude());
        double distanceMeters = west.distanceTo(east);
        return Distance.ofMeters(distanceMeters);
    }

    public Distance getHeightDistance() {
        GeoCoordinate south = GeoCoordinate.of(southWest.getLatitude(), getCenter().getLongitude());
        GeoCoordinate north = GeoCoordinate.of(northEast.getLatitude(), getCenter().getLongitude());
        double distanceMeters = south.distanceTo(north);
        return Distance.ofMeters(distanceMeters);
    }


    public double getAreaDegrees() {
        return getWidth() * getHeight();
    }

    public Distance getAreaSquareMeters() {

        double latCenter = getCenter().getLatitude();
        double widthMeters = getWidthDistance().toMeters();
        double heightMeters = getHeightDistance().toMeters();

        double areaMeters = widthMeters * heightMeters;
        return Distance.ofMeters(areaMeters);
    }


    public boolean contains(GeoCoordinate point) {
        return point.getLatitude() >= southWest.getLatitude() &&
                point.getLatitude() <= northEast.getLatitude() &&
                point.getLongitude() >= southWest.getLongitude() &&
                point.getLongitude() <= northEast.getLongitude();
    }

    public boolean contains(BoundingBox other) {
        return contains(other.southWest) && contains(other.northEast);
    }


    public boolean intersects(BoundingBox other) {
        return !(other.northEast.getLatitude() < this.southWest.getLatitude() ||
                other.southWest.getLatitude() > this.northEast.getLatitude() ||
                other.northEast.getLongitude() < this.southWest.getLongitude() ||
                other.southWest.getLongitude() > this.northEast.getLongitude());
    }

    public BoundingBox intersection(BoundingBox other) {
        if (!intersects(other)) {
            throw new BusinessRuleViolationException("NO_INTERSECTION",
                    "Bounding boxes do not intersect");
        }

        double maxSouthLat = Math.max(this.southWest.getLatitude(), other.southWest.getLatitude());
        double minNorthLat = Math.min(this.northEast.getLatitude(), other.northEast.getLatitude());
        double maxWestLng = Math.max(this.southWest.getLongitude(), other.southWest.getLongitude());
        double minEastLng = Math.min(this.northEast.getLongitude(), other.northEast.getLongitude());

        return BoundingBox.ofCorners(maxSouthLat, maxWestLng, minNorthLat, minEastLng);
    }

    public BoundingBox union(BoundingBox other) {
        double minSouthLat = Math.min(this.southWest.getLatitude(), other.southWest.getLatitude());
        double maxNorthLat = Math.max(this.northEast.getLatitude(), other.northEast.getLatitude());
        double minWestLng = Math.min(this.southWest.getLongitude(), other.southWest.getLongitude());
        double maxEastLng = Math.max(this.northEast.getLongitude(), other.northEast.getLongitude());

        return BoundingBox.ofCorners(minSouthLat, minWestLng, maxNorthLat, maxEastLng);
    }


    public BoundingBox expand(Distance distance) {
        return fromCenterAndRadius(getCenter(),
                Distance.ofMeters(getMaxRadius().toMeters() + distance.toMeters()));
    }

    public BoundingBox expandBy(double factor) {
        if (factor < 0) {
            throw new BusinessRuleViolationException("NEGATIVE_EXPANSION_FACTOR",
                    "Expansion factor cannot be negative: " + factor);
        }

        GeoCoordinate center = getCenter();
        double halfWidth = getWidth() / 2.0 * factor;
        double halfHeight = getHeight() / 2.0 * factor;

        double newSouthLat = center.getLatitude() - halfHeight;
        double newNorthLat = center.getLatitude() + halfHeight;
        double newWestLng = center.getLongitude() - halfWidth;
        double newEastLng = center.getLongitude() + halfWidth;

        return BoundingBox.ofCorners(newSouthLat, newWestLng, newNorthLat, newEastLng);
    }

    public BoundingBox contract(Distance distance) {
        Distance currentRadius = getMaxRadius();
        if (distance.isGreaterThan(currentRadius)) {
            throw new BusinessRuleViolationException("EXCESSIVE_CONTRACTION",
                    "Cannot contract bounding box by more than its radius");
        }

        return fromCenterAndRadius(getCenter(),
                Distance.ofMeters(currentRadius.toMeters() - distance.toMeters()));
    }


    public Distance getMaxRadius() {
        double distanceMeters = getCenter().distanceTo(northEast);
        return Distance.ofMeters(distanceMeters);
    }

    public Distance getMinRadius() {
        double widthMeters = getWidthDistance().toMeters();
        double heightMeters = getHeightDistance().toMeters();
        return Distance.ofMeters(Math.min(widthMeters, heightMeters) / 2.0);
    }

    public Distance distanceTo(GeoCoordinate point) {
        if (contains(point)) {
            return Distance.zero();
        }


        double closestLat = Math.max(southWest.getLatitude(),
                Math.min(point.getLatitude(), northEast.getLatitude()));
        double closestLng = Math.max(southWest.getLongitude(),
                Math.min(point.getLongitude(), northEast.getLongitude()));

        GeoCoordinate closestPoint = GeoCoordinate.of(closestLat, closestLng);
        double distanceMeters = point.distanceTo(closestPoint);
        return Distance.ofMeters(distanceMeters);
    }

    public Distance distanceTo(BoundingBox other) {
        if (intersects(other)) {
            return Distance.zero();
        }


        double minDistance = Double.MAX_VALUE;

        for (GeoCoordinate corner : this.getCorners()) {
            double distance = other.distanceTo(corner).toMeters();
            minDistance = Math.min(minDistance, distance);
        }

        return Distance.ofMeters(minDistance);
    }


    public boolean isWithinDistance(GeoCoordinate point, Distance maxDistance) {
        return distanceTo(point).isLessThan(maxDistance) || distanceTo(point).isEqualTo(maxDistance);
    }

    public boolean isWithinDistance(BoundingBox other, Distance maxDistance) {
        return distanceTo(other).isLessThan(maxDistance) || distanceTo(other).isEqualTo(maxDistance);
    }

    public boolean isAdjacent(BoundingBox other, Distance tolerance) {
        return !intersects(other) && distanceTo(other).isLessThan(tolerance);
    }


    public List<BoundingBox> subdivide(int gridSize) {
        if (gridSize <= 0) {
            throw new BusinessRuleViolationException("INVALID_GRID_SIZE",
                    "Grid size must be positive: " + gridSize);
        }

        List<BoundingBox> subdivisions = new ArrayList<>(gridSize * gridSize);

        double latStep = getHeight() / gridSize;
        double lngStep = getWidth() / gridSize;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double subSouthLat = southWest.getLatitude() + (i * latStep);
                double subNorthLat = southWest.getLatitude() + ((i + 1) * latStep);
                double subWestLng = southWest.getLongitude() + (j * lngStep);
                double subEastLng = southWest.getLongitude() + ((j + 1) * lngStep);

                subdivisions.add(BoundingBox.ofCorners(
                        subSouthLat, subWestLng, subNorthLat, subEastLng));
            }
        }

        return subdivisions;
    }

    public List<BoundingBox> quadrants() {
        return subdivide(2);
    }


    public List<GeoCoordinate> generateGridPoints(Distance spacing) {
        double spacingMeters = spacing.toMeters();
        List<GeoCoordinate> points = new ArrayList<>();

        GeoCoordinate center = getCenter();
        double latStep = spacingMeters / EARTH_RADIUS_METERS * (180.0 / Math.PI);
        double lngStep = latStep / Math.cos(center.getLatitude() * DEGREES_TO_RADIANS);

        double currentLat = southWest.getLatitude();
        while (currentLat <= northEast.getLatitude()) {
            double currentLng = southWest.getLongitude();
            while (currentLng <= northEast.getLongitude()) {
                points.add(GeoCoordinate.of(currentLat, currentLng));
                currentLng += lngStep;
            }
            currentLat += latStep;
        }

        return points;
    }

    public boolean isValidForRouting() {
        Distance maxDimension = Distance.max(getWidthDistance(), getHeightDistance());
        return maxDimension.isLessThan(Distance.ofKilometers(200));
    }

    public boolean isSuitableForRealTimeTracking() {
        Distance area = getAreaSquareMeters();
        return area.isLessThan(Distance.ofKilometers(10000));
    }

    public String toWKT() {
        return String.format("POLYGON((%f %f,%f %f,%f %f,%f %f,%f %f))",
                southWest.getLongitude(), southWest.getLatitude(),
                northEast.getLongitude(), southWest.getLatitude(),
                northEast.getLongitude(), northEast.getLatitude(),
                southWest.getLongitude(), northEast.getLatitude(),
                southWest.getLongitude(), southWest.getLatitude()
        );
    }

    public String toBBoxString() {
        return String.format("%.6f,%.6f,%.6f,%.6f",
                southWest.getLongitude(), southWest.getLatitude(),
                northEast.getLongitude(), northEast.getLatitude());
    }

    public String toDisplayString() {
        return String.format("BBox[SW:(%.4f,%.4f) NE:(%.4f,%.4f)]",
                southWest.getLatitude(), southWest.getLongitude(),
                northEast.getLatitude(), northEast.getLongitude());
    }


    private void validateBounds(GeoCoordinate southWest, GeoCoordinate northEast) {
        if (southWest == null || northEast == null) {
            throw new BusinessRuleViolationException("NULL_COORDINATES",
                    "Bounding box coordinates cannot be null");
        }

        if (southWest.getLatitude() > northEast.getLatitude()) {
            throw new BusinessRuleViolationException("INVALID_LATITUDE_BOUNDS",
                    String.format("South latitude (%.6f) cannot be greater than north latitude (%.6f)",
                            southWest.getLatitude(), northEast.getLatitude()));
        }

        if (southWest.getLongitude() > northEast.getLongitude()) {
            throw new BusinessRuleViolationException("INVALID_LONGITUDE_BOUNDS",
                    String.format("West longitude (%.6f) cannot be greater than east longitude (%.6f)",
                            southWest.getLongitude(), northEast.getLongitude()));
        }

        GeoCoordinate eastPoint = GeoCoordinate.of(southWest.getLatitude(), northEast.getLongitude());
        GeoCoordinate northPoint = GeoCoordinate.of(northEast.getLatitude(), southWest.getLongitude());

        Distance widthDistance = Distance.ofMeters(southWest.distanceTo(eastPoint));
        Distance heightDistance = Distance.ofMeters(southWest.distanceTo(northPoint));
        Distance minDimension = Distance.min(widthDistance, heightDistance);

        if (minDimension.isLessThan(Distance.ofMeters(1.0))) {
            throw new BusinessRuleViolationException("BOUNDING_BOX_TOO_SMALL",
                    "Bounding box dimensions must be at least 1 meter");
        }
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{southWest, northEast};
    }


    @Override
    public String toString() {
        return String.format("BoundingBox{SW:%s, NE:%s}", southWest, northEast);
    }
}