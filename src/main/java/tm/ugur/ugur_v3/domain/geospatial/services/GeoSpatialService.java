package tm.ugur.ugur_v3.domain.geospatial.services;

import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface GeoSpatialService extends DomainService {

    Distance calculateDistance(GeoCoordinate from, GeoCoordinate to);

    Distance calculatePreciseDistance(GeoCoordinate from, GeoCoordinate to);

    Mono<Map<String, Distance>> calculateDistances(Map<String, GeoCoordinatePair> coordinatePairs);


    Bearing calculateBearing(GeoCoordinate from, GeoCoordinate to);

    Bearing calculateFinalBearing(GeoCoordinate from, GeoCoordinate to);

    double calculateBearingDifference(Bearing current, Bearing target);


    GeoCoordinate moveByDistanceAndBearing(GeoCoordinate start, Distance distance, Bearing bearing);

    GeoCoordinate interpolatePoint(GeoCoordinate from, GeoCoordinate to, double fraction);

    Mono<List<GeoCoordinate>> generatePointsAlongPath(GeoCoordinate from, GeoCoordinate to, Distance spacing);


    boolean isWithinRadius(GeoCoordinate center, GeoCoordinate point, Distance radius);

    Mono<List<GeoCoordinate>> findPointsWithinRadius(GeoCoordinate center, Distance radius,
                                                     List<GeoCoordinate> candidates);

    Mono<Map<GeoCoordinate, List<GeoCoordinate>>> findAllWithinRadius(
            List<GeoCoordinate> centers, Distance radius, List<GeoCoordinate> candidates);


    BoundingBox createBoundingBox(GeoCoordinate center, Distance radius);

    BoundingBox createBoundingBoxFromPoints(List<GeoCoordinate> points);

    boolean isWithinBoundingBox(GeoCoordinate point, BoundingBox boundingBox);

    Flux<GeoCoordinate> filterPointsInBoundingBox(Flux<GeoCoordinate> points, BoundingBox boundingBox);


    Distance calculatePolygonArea(List<GeoCoordinate> vertices);

    Distance calculateBoundingBoxArea(BoundingBox boundingBox);


    Distance calculatePathDistance(List<GeoCoordinate> path);

    GeoCoordinate findClosestPointOnSegment(GeoCoordinate point,
                                            GeoCoordinate segmentStart,
                                            GeoCoordinate segmentEnd);

    Distance distanceFromPointToSegment(GeoCoordinate point,
                                        GeoCoordinate segmentStart,
                                        GeoCoordinate segmentEnd);

    boolean isPointOnSegment(GeoCoordinate point,
                             GeoCoordinate segmentStart,
                             GeoCoordinate segmentEnd,
                             Distance tolerance);


    Distance calculateCrossTrackDistance(GeoCoordinate point,
                                         GeoCoordinate pathStart,
                                         GeoCoordinate pathEnd);

    Distance calculateAlongTrackDistance(GeoCoordinate point,
                                         GeoCoordinate pathStart,
                                         GeoCoordinate pathEnd);

    Mono<List<GeoCoordinate>> simplifyPath(List<GeoCoordinate> path, Distance tolerance);


    GeoCoordinate convertCoordinateSystem(GeoCoordinate coordinate,
                                          CoordinateSystem from,
                                          CoordinateSystem to);

    boolean isValidCoordinate(GeoCoordinate coordinate);

    GeoCoordinate normalizeCoordinate(GeoCoordinate coordinate);


    String generateSpatialHash(GeoCoordinate coordinate, int precision);

    BoundingBox getBoundingBoxFromSpatialHash(String spatialHash);

    List<String> getNeighboringSpatialHashes(String spatialHash);


    Mono<GeoSpatialMetrics> getPerformanceMetrics();

    Mono<Void> resetPerformanceCounters();


    record GeoCoordinatePair(String id, GeoCoordinate from, GeoCoordinate to) {}

    enum CoordinateSystem {
        WGS84,
        WEB_MERCATOR,
        UTM,
        LOCAL
    }

    record GeoSpatialMetrics(
            long totalCalculations,
            double averageCalculationTimeMs,
            long cacheHits,
            long cacheMisses,
            double cacheHitRatio
    ) {}
}