package tm.ugur.ugur_v3.domain.routeManagement.repository;

import tm.ugur.ugur_v3.domain.shared.repositories.Repository;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.*;
import tm.ugur.ugur_v3.domain.routeManagement.enums.*;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public interface RouteRepository extends Repository<Route, RouteId> {



    @Override
    Mono<Route> save(Route route);

    @Override
    Mono<Route> findById(RouteId routeId);

    @Override
    Mono<Boolean> existsById(RouteId routeId);

    @Override
    Mono<Void> deleteById(RouteId routeId);

    @Override
    Flux<Route> findAll();



    Flux<Route> findByNameContaining(String namePattern);

    Mono<Route> findByExactName(String routeName);

    Flux<Route> findByStatus(RouteStatus status);

    Flux<Route> findByStatusIn(List<RouteStatus> statuses);

    Flux<Route> findByRouteType(RouteType routeType);

    Flux<Route> findByDirection(RouteDirection direction);

    Flux<Route> findActiveRoutes();

    Flux<Route> findRoutesRequiringMaintenance();



    Flux<Route> findRoutesPassingThroughStop(StopId stopId);

    Flux<Route> findRoutesInBoundingBox(GeoCoordinate southwest, GeoCoordinate northeast);

    Flux<Route> findRoutesWithinRadius(GeoCoordinate center, Distance radius);

    Flux<Route> findRoutesConnecting(StopId fromStop, StopId toStop);

    Flux<Route> findRoutesWithStopsNear(GeoCoordinate location, Distance maxDistance);



    Flux<Route> findRoutesByPerformanceRange(double minOnTimePerformance, double maxOnTimePerformance);

    Flux<Route> findLowRidershipRoutes(int maxDailyRidership);

    Flux<Route> findHighRidershipRoutes(int minDailyRidership);

    Flux<Route> findRoutesBySpeedRange(double minSpeedKmh, double maxSpeedKmh);



    Flux<Route> findRoutesOperatingAt(Timestamp operatingTime);

    Flux<Route> findRoutesOperatingOnDay(DayOfWeek dayOfWeek);

    Flux<Route> findRoutesByComplexity(RouteComplexity complexity);

    Flux<Route> findRoutesByCapacityRequirement(int minCapacity, int maxCapacity);



    Mono<Long> countByStatus(RouteStatus status);

    Mono<Long> countByRouteType(RouteType routeType);

    Mono<RouteStatistics> getRouteStatistics();

    Mono<RoutePerformanceSummary> getPerformanceSummary();



    Flux<Route> saveAll(Flux<Route> routes);

    Mono<Long> updateRoutesStatus(List<RouteId> routeIds, RouteStatus newStatus, String reason);

    Mono<Long> updatePerformanceMetrics(Flux<RoutePerformanceUpdate> updates);



    Flux<Route> findRoutesAssignedToVehicles(List<VehicleId> vehicleIds);

    Flux<RouteVehicleAssignment> getRouteVehicleAssignments();

    Flux<Route> findAlternativeRoutes(RouteId routeId, Distance maxDetourDistance);



    Flux<Route> findRoutesWithActiveSchedules();

    Flux<Route> findRoutesNeedingScheduleUpdates();



    Mono<Map<RouteId, RouteEfficiencyMetrics>> getRouteEfficiencyMetrics(Timestamp fromDate, Timestamp toDate);

    Flux<RouteUsagePattern> getRouteUsagePatterns(Timestamp fromDate, Timestamp toDate);

    Flux<Route> findUnderperformingRoutes(double performanceThreshold);

    Flux<RouteOptimizationCandidate> getOptimizationCandidates();



    Mono<Void> warmCache(List<RouteId> routeIds);

    Mono<Void> invalidateCache(List<RouteId> routeIds);



    @Override
    Mono<Boolean> isHealthy();

    Mono<RepositoryMetrics> getRepositoryMetrics();



    record RouteStatistics(
            long totalRoutes,
            long activeRoutes,
            long inactiveRoutes,
            long routesRequiringMaintenance,
            Map<RouteType, Long> routesByType,
            Map<RouteStatus, Long> routesByStatus,
            double averageOnTimePerformance,
            int totalDailyRidership
    ) {}

    record RoutePerformanceSummary(
            double averageOnTimePerformance,
            double averageSpeed,
            int totalMissedTrips,
            int routesAboveTarget,
            int routesBelowTarget,
            List<RoutePerformanceAlert> alerts
    ) {}

    record RoutePerformanceUpdate(
            RouteId routeId,
            double onTimePerformance,
            double averageSpeed,
            int dailyRidership,
            Timestamp updateTime
    ) {}

    record RouteVehicleAssignment(
            RouteId routeId,
            VehicleId vehicleId,
            Timestamp assignedAt,
            boolean isActive
    ) {}

    record RouteEfficiencyMetrics(
            RouteId routeId,
            double distanceEfficiency,
            double timeEfficiency,
            double fuelEfficiency,
            double passengerEfficiency,
            double overallScore
    ) {}

    record RouteUsagePattern(
            RouteId routeId,
            Map<DayOfWeek, Integer> dailyRidership,
            Map<Integer, Integer> hourlyUsage,
            List<StopId> popularStops,
            double peakUtilization
    ) {}

    record RouteOptimizationCandidate(
            RouteId routeId,
            String routeName,
            OptimizationType optimizationType,
            double potentialImprovement,
            String optimizationReason,
            int priority
    ) {}

    record RoutePerformanceAlert(
            RouteId routeId,
            String alertType,
            String description,
            double severity,
            Timestamp alertTime
    ) {}

    record RepositoryMetrics(
            long totalQueries,
            double averageQueryTime,
            long cacheHits,
            long cacheMisses,
            double cacheHitRate,
            Map<String, Long> queryTypeStats
    ) {}



    enum OptimizationType {
        DISTANCE_OPTIMIZATION,
        TIME_OPTIMIZATION,
        STOP_REORDERING,
        FREQUENCY_ADJUSTMENT,
        SCHEDULE_OPTIMIZATION
    }
}