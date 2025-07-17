
package tm.ugur.ugur_v3.domain.geospatial.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Bearing;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.BoundingBox;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Speed;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.stopManagement.aggregate.Stop;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface NearbyStopsService extends DomainService {


    Mono<List<NearbyStop>> findStopsWithinRadius(GeoCoordinate center, Distance radius);

    Mono<List<NearbyStop>> findClosestStops(GeoCoordinate location, int maxStops);

    Mono<List<NearbyStop>> findStopsAlongRoute(List<GeoCoordinate> routePath,
                                               Distance corridorWidth);

    Flux<NearbyStop> findStopsInBoundingBox(BoundingBox boundingBox);


    Mono<List<NearbyVehicle>> findVehiclesWithinRadius(GeoCoordinate center, Distance radius);

    Mono<List<ApproachingVehicle>> findVehiclesApproachingStop(StopId stopId,
                                                               Distance approachRadius);

    Mono<List<NearbyVehicle>> findVehiclesOnRoute(RouteId routeId, BoundingBox searchArea);

    Mono<NearbyVehicle> findClosestVehicle(GeoCoordinate location, Distance maxSearchRadius);


    Mono<ProximityResult> checkVehicleStopProximity(VehicleId vehicleId,
                                                    GeoCoordinate vehicleLocation);

    Mono<List<StopArrival>> detectStopArrivals(VehicleId vehicleId,
                                               GeoCoordinate currentLocation,
                                               GeoCoordinate previousLocation);

    Mono<List<StopDeparture>> detectStopDepartures(VehicleId vehicleId,
                                                   GeoCoordinate currentLocation,
                                                   GeoCoordinate previousLocation);


    Mono<Map<GeoCoordinate, List<NearbyStop>>> batchFindNearbyStops(
            List<GeoCoordinate> locations, Distance radius);

    Mono<Map<VehicleId, List<NearbyStop>>> batchAnalyzeVehicleProximity(
            Map<VehicleId, GeoCoordinate> vehiclePositions, Distance radius);

    Mono<ProximityNetwork> analyzeProximityNetwork(BoundingBox area, Distance maxDistance);


    Flux<ProximityEvent> monitorProximityChanges(GeoCoordinate watchLocation,
                                                 Distance monitorRadius);

    Mono<List<ProximityAlert>> trackVehicleMovements(VehicleId vehicleId,
                                                     GeoCoordinate newLocation,
                                                     ProximityMonitoringConfig config);

    Flux<VehicleProximityUpdate> streamStopProximityUpdates(StopId stopId);


    Mono<Void> buildStopSpatialIndex(List<Stop> stops);

    Mono<Void> updateSpatialIndex(StopId stopId, GeoCoordinate newLocation);

    Mono<Void> optimizeSpatialIndex();

    Mono<SpatialIndexStats> getSpatialIndexStats();


    Mono<Duration> estimateTravelTimeBetweenStops(StopId fromStop, StopId toStop, Speed averageSpeed);

    Mono<List<StopId>> findOptimalStopSequence(GeoCoordinate startLocation,
                                               List<StopId> stopsToVisit,
                                               Speed averageSpeed);

    Mono<StopAccessibilityReport> analyzeStopAccessibility(StopId stopId, Distance walkingRadius);


    Mono<Geofence> createStopGeofence(StopId stopId, Distance radius, GeofenceType type);

    Mono<List<GeofenceViolation>> checkGeofenceViolations(VehicleId vehicleId,
                                                          GeoCoordinate location);

    Flux<GeofenceEvent> monitorGeofenceEvents(List<Geofence> geofences);


    Mono<ProximitySearchMetrics> getPerformanceMetrics();

    Mono<Void> resetPerformanceCounters();


    record NearbyStop(
            StopId stopId,
            String stopName,
            GeoCoordinate location,
            Distance distanceFromQuery,
            Bearing bearingFromQuery,
            List<RouteId> servingRoutes,
            StopType stopType,
            boolean isAccessible,
            Timestamp lastUpdated
    ) {}


    record NearbyVehicle(
            VehicleId vehicleId,
            String vehicleName,
            GeoCoordinate location,
            Distance distanceFromQuery,
            Bearing bearingFromQuery,
            Speed currentSpeed,
            Bearing heading,
            RouteId assignedRoute,
            VehicleStatus status,
            Timestamp lastLocationUpdate
    ) {}


    record ApproachingVehicle(
            VehicleId vehicleId,
            StopId targetStopId,
            Distance distanceToStop,
            Duration estimatedArrivalTime,
            Speed currentSpeed,
            Bearing approachDirection,
            double confidence,
            boolean isOnSchedule
    ) {}

    record ProximityResult(
            VehicleId vehicleId,
            List<NearbyStop> nearbyStops,
            boolean isAtStop,
            StopId currentStopId,
            Distance distanceToNearestStop,
            ProximityStatus status
    ) {}

    enum ProximityStatus {
        AT_STOP,
        APPROACHING_STOP,
        NEAR_MULTIPLE_STOPS,
        BETWEEN_STOPS,
        OFF_ROUTE,
        UNKNOWN
    }

    record StopArrival(
            VehicleId vehicleId,
            StopId stopId,
            GeoCoordinate arrivalLocation,
            Timestamp arrivalTime,
            Duration dwellTimeEstimate,
            boolean isScheduledStop,
            double confidence
    ) {}

    record StopDeparture(
            VehicleId vehicleId,
            StopId stopId,
            GeoCoordinate departureLocation,
            Timestamp departureTime,
            Duration actualDwellTime,
            Bearing departureDirection,
            boolean wasScheduledStop
    ) {}

    record ProximityNetwork(
            BoundingBox analysisArea,
            List<ProximityRelationship> relationships,
            Map<StopId, List<NearbyStop>> stopConnections,
            Map<VehicleId, List<NearbyStop>> vehicleProximities,
            ProximityNetworkStats statistics
    ) {}

    record ProximityRelationship(
            String entityId1,
            String entityId2,
            EntityType type1,
            EntityType type2,
            Distance distance,
            RelationshipStrength strength
    ) {}

    enum EntityType { STOP, VEHICLE, ROUTE_POINT }
    enum RelationshipStrength { VERY_CLOSE, CLOSE, MODERATE, FAR }

    record ProximityEvent(
            String eventId,
            EntityType entityType,
            String entityId,
            GeoCoordinate location,
            ProximityEventType eventType,
            Distance distance,
            Timestamp eventTime,
            Map<String, Object> additionalData
    ) {}

    enum ProximityEventType {
        ENTERED_RADIUS,
        EXITED_RADIUS,
        MOVED_CLOSER,
        MOVED_FARTHER,
        STOPPED_MOVING,
        STARTED_MOVING
    }

    record ProximityAlert(
            VehicleId vehicleId,
            AlertType alertType,
            String message,
            AlertSeverity severity,
            GeoCoordinate location,
            Map<String, Object> alertData,
            Timestamp alertTime
    ) {}

    enum AlertType {
        STOP_APPROACH,
        STOP_ARRIVAL,
        STOP_DEPARTURE,
        OFF_ROUTE,
        GEOFENCE_VIOLATION,
        PROXIMITY_WARNING
    }

    enum AlertSeverity { INFO, WARNING, CRITICAL }

    record VehicleProximityUpdate(
            StopId stopId,
            List<ApproachingVehicle> approachingVehicles,
            List<VehicleId> vehiclesAtStop,
            Timestamp updateTime
    ) {}

    record ProximityMonitoringConfig(
            Distance alertRadius,
            Duration minAlertInterval,
            Set<AlertType> enabledAlerts,
            boolean trackMovementHistory,
            int maxHistoryPoints
    ) {}

    record Geofence(
            String geofenceId,
            GeoCoordinate center,
            Distance radius,
            GeofenceType type,
            boolean isActive,
            Map<String, Object> metadata
    ) {}

    enum GeofenceType {
        STOP_ARRIVAL,
        STOP_DEPARTURE,
        ROUTE_BOUNDARY,
        SERVICE_AREA,
        RESTRICTED_ZONE
    }

    record GeofenceViolation(
            VehicleId vehicleId,
            Geofence violatedGeofence,
            ViolationType violationType,
            GeoCoordinate violationLocation,
            Timestamp violationTime,
            Distance distanceFromBoundary
    ) {}

    enum ViolationType { UNAUTHORIZED_ENTRY, UNAUTHORIZED_EXIT, OVERSTAY }

    record GeofenceEvent(
            VehicleId vehicleId,
            Geofence geofence,
            GeofenceEventType eventType,
            GeoCoordinate eventLocation,
            Timestamp eventTime
    ) {}

    enum GeofenceEventType { ENTRY, EXIT, DWELL }

    record StopAccessibilityReport(
            StopId stopId,
            boolean isWheelchairAccessible,
            List<NearbyStop> accessibleNearbyStops,
            Distance walkingDistanceToNearestAccessible,
            Map<String, Boolean> accessibilityFeatures
    ) {}

    record SpatialIndexStats(
            int totalIndexedEntities,
            int indexDepth,
            double averageQueryTimeMs,
            long totalQueries,
            double indexEfficiency,
            Timestamp lastOptimization
    ) {}

    record ProximityNetworkStats(
            int totalEntities,
            int totalRelationships,
            double averageConnectivity,
            Distance averageProximityDistance,
            Map<EntityType, Integer> entityCounts
    ) {}

    record ProximitySearchMetrics(
            long totalSearches,
            double averageSearchTimeMs,
            long spatialIndexHits,
            long spatialIndexMisses,
            double indexHitRatio,
            Map<String, Long> searchTypeCounts,
            ProximitySearchStats performanceStats
    ) {}

    record ProximitySearchStats(
            double minSearchTimeMs,
            double maxSearchTimeMs,
            double averageSearchTimeMs,
            double percentile95Ms,
            double percentile99Ms
    ) {}

    enum StopType { REGULAR, EXPRESS, TERMINAL, INTERCHANGE }
    enum VehicleStatus { ACTIVE, INACTIVE, MAINTENANCE, OUT_OF_SERVICE }
}