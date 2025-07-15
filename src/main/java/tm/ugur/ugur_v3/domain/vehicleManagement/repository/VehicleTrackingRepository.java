/**
 * COMPONENT: VehicleTrackingRepository Interface
 * LAYER: Domain/VehicleManagement/Repository
 * PURPOSE: GPS tracking session persistence - optimized for high-frequency data
 * PERFORMANCE TARGET: < 5ms GPS data writes, < 10ms session queries
 * SCALABILITY: Ultra-high throughput for continuous GPS streams
 */
package tm.ugur.ugur_v3.domain.vehicleManagement.repository;

import tm.ugur.ugur_v3.domain.shared.repositories.Repository;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.VehicleTrackingSession;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.TrackingSessionId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Bearing;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;
import java.util.Map;


public interface VehicleTrackingRepository extends Repository<VehicleTrackingSession, TrackingSessionId> {


    @Override
    Mono<VehicleTrackingSession> save(VehicleTrackingSession session);

    @Override
    Mono<VehicleTrackingSession> findById(TrackingSessionId sessionId);

    @Override
    Mono<Boolean> existsById(TrackingSessionId sessionId);

    @Override
    Mono<Void> deleteById(TrackingSessionId sessionId);

    @Override
    Flux<VehicleTrackingSession> findAll();


    Mono<VehicleTrackingSession> findActiveSessionByVehicleId(VehicleId vehicleId);

    Flux<VehicleTrackingSession> findActiveSessions();

    Flux<VehicleTrackingSession> findByVehicleId(VehicleId vehicleId);

    Flux<VehicleTrackingSession> findByRouteId(String routeId);

    Flux<VehicleTrackingSession> findByStatus(VehicleTrackingSession.SessionStatus status);

    Flux<VehicleTrackingSession> findSessionsInTimeRange(Timestamp startTime, Timestamp endTime);

    Flux<VehicleTrackingSession> findLongRunningSessions(Duration minimumDuration);

    Flux<VehicleTrackingSession> findSessionsWithPoorGpsQuality(double maxAccuracyPercentage);


    Mono<Void> storeGpsDataPoint(TrackingSessionId sessionId, GPSDataPoint gpsData);

    Mono<Long> storeGpsDataPoints(TrackingSessionId sessionId, Flux<GPSDataPoint> gpsDataPoints);

    Flux<GPSDataPoint> getGpsDataForSession(TrackingSessionId sessionId,
                                            Timestamp startTime, Timestamp endTime);

    Flux<GPSDataPoint> getRecentGpsData(TrackingSessionId sessionId, int count);

    Flux<GPSDataPoint> getGpsDataWithinArea(TrackingSessionId sessionId,
                                            GeoCoordinate center, double radiusMeters);

    Flux<GPSDataPoint> getStationaryGpsData(TrackingSessionId sessionId,
                                            double maxSpeedKmh, long minimumDurationSeconds);


    Mono<SessionStatistics> getSessionStatistics(TrackingSessionId sessionId);

    Mono<RoutePerformanceMetrics> getRoutePerformanceMetrics(String routeId,
                                                             Timestamp startTime, Timestamp endTime);

    Mono<VehiclePerformanceMetrics> getVehiclePerformanceMetrics(VehicleId vehicleId,
                                                                 Timestamp startTime, Timestamp endTime);

    Mono<SystemTrackingStats> getSystemTrackingStats(Timestamp startTime, Timestamp endTime);


    Mono<Long> archiveOldSessions(Timestamp cutoffDate);

    Mono<Long> deleteOldGpsData(Timestamp cutoffDate);

    Mono<Long> cleanupOrphanedGpsData();

    Mono<Void> optimizeGpsDataStorage();


    Flux<VehicleTrackingSession> endSessions(List<TrackingSessionId> sessionIds, String reason);

    Mono<Long> updateSessionStatuses(Flux<SessionStatusUpdate> sessionStatusUpdates);


    record GPSDataPoint(
            GeoCoordinate location,
            Speed speed,
            Bearing bearing,
            Timestamp timestamp,
            double accuracy,
            Map<String, Object> additionalData
    ) {
        public GPSDataPoint {
            if (location == null || timestamp == null) {
                throw new IllegalArgumentException("Location and timestamp cannot be null");
            }
            if (accuracy < 0) {
                throw new IllegalArgumentException("Accuracy cannot be negative");
            }
        }

        public static GPSDataPoint of(GeoCoordinate location, Speed speed, Bearing bearing,
                                      Timestamp timestamp, double accuracy) {
            return new GPSDataPoint(location, speed, bearing, timestamp, accuracy, Map.of());
        }
    }

    record SessionStatusUpdate(
            TrackingSessionId sessionId,
            VehicleTrackingSession.SessionStatus newStatus,
            String reason,
            Timestamp timestamp
    ) {}

    record SessionStatistics(
            TrackingSessionId sessionId,
            VehicleId vehicleId,
            Duration sessionDuration,
            int totalGpsPoints,
            int validGpsPoints,
            double gpsAccuracyPercentage,
            double totalDistanceKm,
            Speed averageSpeed,
            Speed maxSpeed,
            long stoppedTimeMinutes,
            VehicleTrackingSession.SessionQuality quality
    ) {}

    record RoutePerformanceMetrics(
            String routeId,
            int totalSessions,
            int completedSessions,
            Duration averageSessionDuration,
            double averageDistanceKm,
            Speed averageSpeed,
            double averageGpsAccuracy,
            int sessionsWithIssues,
            List<String> commonIssues
    ) {}

    record VehiclePerformanceMetrics(
            VehicleId vehicleId,
            int totalSessions,
            int completedSessions,
            Duration totalOperatingTime,
            double totalDistanceKm,
            Speed averageSpeed,
            double averageGpsAccuracy,
            int maintenanceEvents,
            double utilizationPercentage
    ) {}

    record SystemTrackingStats(
            int totalActiveSessions,
            int totalVehiclesTracked,
            long gpsPointsPerHour,
            double systemGpsAccuracy,
            double averageSessionDuration,
            int sessionsWithIssues,
            double systemHealthScore
    ) {}
}