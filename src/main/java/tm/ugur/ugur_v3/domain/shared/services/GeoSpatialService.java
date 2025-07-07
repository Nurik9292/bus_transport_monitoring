package tm.ugur.ugur_v3.domain.shared.services;

import tm.ugur.ugur_v3.domain.shared.valueobjects.Direction;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoBounds;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;

public interface GeoSpatialService extends DistanceCalculationService {

    GeoBounds calculateBounds(java.util.List<GeoCoordinate> points);

    GeoCoordinate findClosestPoint(GeoCoordinate target, java.util.List<GeoCoordinate> candidates);

    boolean isWithinGeofence(GeoCoordinate location, GeoBounds geofence);


    GeoCoordinate projectOntoRoute(GeoCoordinate location, java.util.List<GeoCoordinate> routePoints);


    Direction calculateBearing(GeoCoordinate from, GeoCoordinate to);
}
