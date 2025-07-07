package tm.ugur.ugur_v3.domain.shared.services;

import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;

public interface DistanceCalculationService {
    double calculateDistance(GeoCoordinate from, GeoCoordinate to);
    double calculateDistanceAlongRoute(GeoCoordinate from, GeoCoordinate to, java.util.List<GeoCoordinate> routePoints);

    default boolean isNearby(GeoCoordinate location1, GeoCoordinate location2, double maxDistanceMeters) {
        return calculateDistance(location1, location2) <= maxDistanceMeters;
    }
}
