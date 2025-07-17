package tm.ugur.ugur_v3.domain.geospatial.services;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.*;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSegment;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RouteMatchingService extends DomainService {


    Mono<Optional<RouteMatch>> findMatchingRoute(GeoCoordinate gpsPoint);

    Mono<Optional<RouteMatch>> findMatchingRoute(GeoCoordinate gpsPoint,
                                                 VehicleId vehicleId,
                                                 RouteMatchingContext context);

    Mono<Optional<RouteMatch>> findMatchingRouteInArea(GeoCoordinate gpsPoint,
                                                       BoundingBox searchArea,
                                                       List<RouteId> candidateRoutes);


    Mono<Optional<RouteSegmentMatch>> findNearestSegment(GeoCoordinate gpsPoint, RouteId routeId);

    Mono<List<RouteSegmentMatch>> findSegmentsWithinTolerance(GeoCoordinate gpsPoint,
                                                              RouteId routeId,
                                                              Distance tolerance);

    Mono<Optional<RouteSegmentMatch>> matchToSegment(GeoCoordinate gpsPoint,
                                                     RouteSegment segment,
                                                     Distance maxDeviation);


    Mono<Double> calculateRouteAdherence(List<GpsTrackPoint> gpsTrack, Route route);

    Mono<RouteAdherenceReport> analyzeRouteAdherenceOverTime(VehicleId vehicleId,
                                                             RouteId routeId,
                                                             Duration timeWindow);

    Mono<List<RouteDeviation>> detectRouteDeviations(List<GpsTrackPoint> gpsTrack,
                                                     Route route,
                                                     Distance deviationThreshold);


    Mono<List<GeoCoordinate>> filterGpsNoise(List<GpsTrackPoint> rawGpsPoints);

    Mono<List<GpsTrackPoint>> removeGpsOutliers(List<GpsTrackPoint> gpsPoints,
                                                Speed maxReasonableSpeed);

    Mono<List<GpsTrackPoint>> smoothGpsTrack(List<GpsTrackPoint> gpsPoints,
                                             int windowSize,
                                             double weightDecay);

    Mono<List<GpsTrackPoint>> interpolateMissingPoints(List<GpsTrackPoint> gpsPoints,
                                                       Duration maxGapDuration);


    Mono<List<RouteMatch>> mapMatchWithHMM(List<GpsTrackPoint> gpsTrack,
                                           List<Route> candidateRoutes);

    Mono<List<RouteMatch>> mapMatchGeometric(List<GpsTrackPoint> gpsTrack,
                                             Distance searchRadius);

    Mono<List<RouteMatch>> mapMatchTopological(List<GpsTrackPoint> gpsTrack,
                                               List<Route> candidateRoutes,
                                               Distance searchRadius);


    Flux<RouteMatch> processGpsStream(Flux<GpsTrackPoint> gpsStream,
                                      VehicleId vehicleId,
                                      RouteMatchingState initialState);

    Mono<RouteMatchingState> updateMatchingState(RouteMatchingState currentState,
                                                 GpsTrackPoint newGpsPoint);

    Mono<RouteMatchingState> resetMatchingState(VehicleId vehicleId);


    double calculateMatchConfidence(GeoCoordinate gpsPoint,
                                    RouteSegment matchedSegment,
                                    Distance distanceToSegment,
                                    Bearing gpsHeading,
                                    Speed gpsSpeed);

    Mono<MatchQuality> validateMatchQuality(RouteMatch routeMatch,
                                            List<GpsTrackPoint> recentHistory);

    double calculateGpsQuality(GpsTrackPoint gpsPoint);


    Mono<Double> calculateRouteProgress(GeoCoordinate currentPosition,
                                        Route route,
                                        RouteSegment currentSegment);

    Mono<Distance> estimateRemainingDistance(GeoCoordinate currentPosition,
                                             Route route,
                                             RouteSegment currentSegment);

    Mono<Optional<RouteSegment>> predictNextSegment(RouteSegment currentSegment,
                                                    Bearing vehicleHeading,
                                                    Speed vehicleSpeed);


    Mono<RouteAnalysisReport> analyzeHistoricalGpsData(VehicleId vehicleId,
                                                       RouteId routeId,
                                                       List<GpsTrackPoint> historicalData);

    Flux<RouteMatch> batchProcessVehiclePositions(Map<VehicleId, GpsTrackPoint> vehiclePositions);

    Mono<RouteCoverageStats> analyzeRouteCoverage(RouteId routeId,
                                                  List<GpsTrackPoint> allGpsData,
                                                  Duration timeWindow);


    Mono<Void> buildSpatialIndex(List<Route> routes);

    Mono<Void> cachePopularRouteSegments(List<RouteId> popularRoutes);

    Mono<RouteMatchingMetrics> getPerformanceMetrics();



    record RouteMatch(
            RouteId routeId,
            RouteSegment matchedSegment,
            GeoCoordinate projectedPoint,
            Distance distanceFromRoute,
            double confidence,
            Bearing routeHeading,
            double progressAlongSegment,
            Timestamp matchedAt,
            MatchQuality quality
    ) {}

    record RouteSegmentMatch(
            RouteSegment segment,
            GeoCoordinate closestPoint,
            Distance distanceToSegment,
            double confidence,
            boolean isOnSegment
    ) {}

    record GpsTrackPoint(
            GeoCoordinate coordinate,
            Timestamp timestamp,
            Speed speed,
            Bearing heading,
            double accuracy,
            double horizontalDilution,
            int satelliteCount,
            VehicleId vehicleId
    ) {}

    record RouteMatchingContext(
            RouteId expectedRouteId,
            GeoCoordinate lastKnownPosition,
            Bearing lastKnownHeading,
            Speed averageSpeed,
            List<RouteId> alternativeRoutes,
            Distance searchRadius,
            boolean allowOffRoute
    ) {}

    record RouteMatchingState(
            VehicleId vehicleId,
            Optional<RouteMatch> lastMatch,
            List<GpsTrackPoint> recentHistory,
            Timestamp lastUpdateTime,
            int consecutiveMatches,
            int consecutiveMisses,
            double averageConfidence,
            boolean isOffRoute
    ) {}


    record RouteDeviation(
            GeoCoordinate deviationPoint,
            Distance maxDeviation,
            Duration deviationDuration,
            DeviationType type,
            String description
    ) {}

    enum DeviationType {
        MINOR_DEVIATION,
        MAJOR_DEVIATION,
        OFF_ROUTE,
        WRONG_DIRECTION,
        STATIONARY,
        GPS_NOISE
    }


    record RouteAdherenceReport(
            RouteId routeId,
            VehicleId vehicleId,
            Duration analysisWindow,
            double overallAdherence,
            double averageDeviation,
            int totalDeviations,
            Duration totalDeviationTime,
            List<RouteDeviation> significantDeviations,
            Map<DeviationType, Integer> deviationCounts
    ) {}

    @Getter
    enum MatchQuality {
        EXCELLENT(0.9, "High confidence, low deviation"),
        GOOD(0.7, "Good confidence, reasonable deviation"),
        FAIR(0.5, "Moderate confidence, some deviation"),
        POOR(0.3, "Low confidence, high deviation"),
        UNRELIABLE(0.1, "Very low confidence, unreliable");

        private final double threshold;
        private final String description;

        MatchQuality(double threshold, String description) {
            this.threshold = threshold;
            this.description = description;
        }

        public static MatchQuality fromConfidence(double confidence) {
            for (MatchQuality quality : values()) {
                if (confidence >= quality.threshold) {
                    return quality;
                }
            }
            return UNRELIABLE;
        }
    }

    record RouteAnalysisReport(
            RouteId routeId,
            VehicleId vehicleId,
            int totalGpsPoints,
            int successfulMatches,
            double matchSuccessRate,
            double averageConfidence,
            Distance averageDeviation,
            List<RouteSegment> problematicSegments,
            Map<String, Object> additionalMetrics
    ) {}

    record RouteCoverageStats(
            RouteId routeId,
            int totalSegments,
            int coveredSegments,
            double coveragePercentage,
            List<RouteSegment> uncoveredSegments,
            Map<RouteSegment, Integer> segmentTrafficCounts,
            Duration analysisWindow
    ) {}

    record RouteMatchingMetrics(
            long totalMatchAttempts,
            long successfulMatches,
            double successRate,
            double averageMatchingTimeMs,
            double averageConfidence,
            long cacheHits,
            long cacheMisses,
            Map<MatchQuality, Long> qualityDistribution,
            Map<DeviationType, Long> deviationCounts
    ) {}
}