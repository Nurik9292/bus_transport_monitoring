package tm.ugur.ugur_v3.domain.routeManagement.repository;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteComplexity;
import tm.ugur.ugur_v3.domain.shared.repositories.Repository;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSchedule;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteDirection;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteStatus;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteType;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.*;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Distance;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

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

    // ================== ПОИСК ПО БИЗНЕС-КРИТЕРИЯМ ==================

    Flux<Route> findActiveRoutes();

    Flux<Route> findByRouteType(RouteType routeType);

    Flux<Route> findByDirection(RouteDirection direction);

    Flux<Route> findByStatus(RouteStatus status);

    Flux<Route> findCircularRoutes();

    Flux<Route> findByComplexity(RouteComplexity complexity);

    // ================== ПОИСК ПО ОСТАНОВКАМ ==================

    Flux<Route> findByStopId(StopId stopId);

    Flux<Route> findByStopIds(List<StopId> stopIds);

    Flux<Route> findByStartingStop(StopId stopId);

    Flux<Route> findByEndingStop(StopId stopId);

    Flux<Route> findRoutesBetweenStops(StopId fromStopId, StopId toStopId);

    // ================== ГЕОПРОСТРАНСТВЕННЫЕ ЗАПРОСЫ ==================

    Flux<Route> findRoutesWithinRadius(GeoCoordinate center, Distance radius);

    Flux<Route> findRoutesInBoundingBox(GeoCoordinate northEast, GeoCoordinate southWest);

    Flux<Route> findRoutesLongerThan(Distance minimumDistance);

    Flux<Route> findRoutesShorterThan(Distance maximumDistance);

    Flux<Route> findNearestRoutes(GeoCoordinate location, int maxResults);

    // ================== ФИЛЬТРАЦИЯ ПО ПРОИЗВОДИТЕЛЬНОСТИ ==================

    Flux<Route> findHighPerformanceRoutes(double minOnTimePerformance);

    Flux<Route> findLowPerformanceRoutes(double maxOnTimePerformance);

    Flux<Route> findRoutesRequiringMaintenance();

    Flux<Route> findHighRidershipRoutes(int minDailyRidership);

    Flux<Route> findSlowRoutes(double maxAverageSpeed);

    // ================== ПОИСК ПО ОПЕРАЦИОННЫМ ПАРАМЕТРАМ ==================

    Flux<Route> findByOperatingDays(Set<DayOfWeek> operatingDays);

    Flux<Route> findByVehicleCapacity(int minCapacity, int maxCapacity);

    Flux<Route> findByServiceFrequency(int minHeadwayMinutes, int maxHeadwayMinutes);

    Flux<Route> findUrbanRoutes();

    Flux<Route> findSuburbanRoutes();

    // ================== АГРЕГАЦИОННЫЕ ЗАПРОСЫ ==================

    Mono<Long> countAllRoutes();

    Mono<Long> countActiveRoutes();

    Mono<Long> countByRouteType(RouteType routeType);

    Mono<Double> getAverageRouteDistance();

    Mono<Double> getAverageOnTimePerformance();

    Mono<Long> getTotalDailyRidership();

    // ================== COMPLEX QUERIES ==================

    Flux<Route> findRoutesByCriteria(RouteSearchCriteria criteria);

    Mono<RouteStatistics> getRouteStatistics();

    Flux<Route> findOptimalRoutes(GeoCoordinate origin, GeoCoordinate destination,
                                  RouteOptimizationCriteria criteria);

    Flux<Route> findCompetingRoutes(RouteId routeId, double overlapThreshold);

    Flux<Route> findAlternativeRoutes(StopId fromStop, StopId toStop, RouteId excludeRoute);

    // ================== РАСПИСАНИЯ ==================

    Flux<RouteSchedule> findSchedulesByRouteId(RouteId routeId);

    Flux<RouteSchedule> findActiveSchedules();

    Flux<RouteSchedule> findSchedulesForDay(DayOfWeek dayOfWeek);

    // ================== PERFORMANCE & MONITORING ==================

    Flux<Route> findRoutesWithAlerts();

    Flux<Route> findTopPerformingRoutes(int limit);

    Flux<Route> findWorstPerformingRoutes(int limit);

    Flux<Route> findMostPopularRoutes(int limit);

    Flux<Route> findSlowestRoutes(int limit);

    // ================== BULK OPERATIONS ==================

    Flux<Route> saveAll(Flux<Route> routes);

    Mono<Void> deleteAllById(Flux<RouteId> routeIds);

    Mono<Void> activateRoutes(List<RouteId> routeIds);

    Mono<Void> deactivateRoutes(List<RouteId> routeIds);

    // ================== SEARCH CRITERIA CLASSES ==================

    @Getter
    class RouteSearchCriteria {
        private RouteType routeType;
        private RouteStatus status;
        private RouteDirection direction;
        private RouteComplexity complexity;
        private Boolean isCircular;
        private Boolean isActive;
        private Double minDistance;
        private Double maxDistance;
        private Double minOnTimePerformance;
        private Integer minDailyRidership;
        private Set<DayOfWeek> operatingDays;
        private GeoCoordinate centerPoint;
        private Distance searchRadius;
        private List<StopId> mustPassThroughStops;
        private Integer minVehicleCapacity;
        private Integer maxVehicleCapacity;

        public static RouteSearchCriteria builder() {
            return new RouteSearchCriteria();
        }

        public RouteSearchCriteria withRouteType(RouteType routeType) {
            this.routeType = routeType;
            return this;
        }

        public RouteSearchCriteria withStatus(RouteStatus status) {
            this.status = status;
            return this;
        }

        public RouteSearchCriteria withDirection(RouteDirection direction) {
            this.direction = direction;
            return this;
        }

        public RouteSearchCriteria withComplexity(RouteComplexity complexity) {
            this.complexity = complexity;
            return this;
        }

        public RouteSearchCriteria circular(Boolean isCircular) {
            this.isCircular = isCircular;
            return this;
        }

        public RouteSearchCriteria active(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public RouteSearchCriteria withDistanceRange(Double minDistance, Double maxDistance) {
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
            return this;
        }

        public RouteSearchCriteria withMinPerformance(Double minOnTimePerformance) {
            this.minOnTimePerformance = minOnTimePerformance;
            return this;
        }

        public RouteSearchCriteria withMinRidership(Integer minDailyRidership) {
            this.minDailyRidership = minDailyRidership;
            return this;
        }

        public RouteSearchCriteria operatingOn(Set<DayOfWeek> operatingDays) {
            this.operatingDays = operatingDays;
            return this;
        }

        public RouteSearchCriteria nearLocation(GeoCoordinate centerPoint, Distance searchRadius) {
            this.centerPoint = centerPoint;
            this.searchRadius = searchRadius;
            return this;
        }

        public RouteSearchCriteria passingThrough(List<StopId> stops) {
            this.mustPassThroughStops = stops;
            return this;
        }

        public RouteSearchCriteria withCapacityRange(Integer minCapacity, Integer maxCapacity) {
            this.minVehicleCapacity = minCapacity;
            this.maxVehicleCapacity = maxCapacity;
            return this;
        }

    }

    class RouteStatistics {
        @Getter
        private final long totalRoutes;
        @Getter
        private final long activeRoutes;
        @Getter
        private final long inactiveRoutes;
        @Getter
        private final double averageDistance;
        @Getter
        private final double averageOnTimePerformance;
        @Getter
        private final long totalDailyRidership;
        private final int routesByTypeRegular;
        private final int routesByTypeExpress;
        private final int routesByTypeNight;
        @Getter
        private final int circularRoutes;
        @Getter
        private final int linearRoutes;

        public RouteStatistics(long totalRoutes, long activeRoutes, long inactiveRoutes,
                               double averageDistance, double averageOnTimePerformance,
                               long totalDailyRidership, int regular, int express, int night,
                               int circular, int linear) {
            this.totalRoutes = totalRoutes;
            this.activeRoutes = activeRoutes;
            this.inactiveRoutes = inactiveRoutes;
            this.averageDistance = averageDistance;
            this.averageOnTimePerformance = averageOnTimePerformance;
            this.totalDailyRidership = totalDailyRidership;
            this.routesByTypeRegular = regular;
            this.routesByTypeExpress = express;
            this.routesByTypeNight = night;
            this.circularRoutes = circular;
            this.linearRoutes = linear;
        }

        public int getRegularRoutes() { return routesByTypeRegular; }
        public int getExpressRoutes() { return routesByTypeExpress; }
        public int getNightRoutes() { return routesByTypeNight; }

        public double getActiveRoutesPercentage() {
            return totalRoutes > 0 ? (activeRoutes * 100.0) / totalRoutes : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "RouteStatistics{total=%d, active=%d (%.1f%%), avgDistance=%.2fkm, " +
                            "avgPerformance=%.1f%%, ridership=%d, types=[regular=%d, express=%d, night=%d]}",
                    totalRoutes, activeRoutes, getActiveRoutesPercentage(), averageDistance,
                    averageOnTimePerformance, totalDailyRidership, routesByTypeRegular,
                    routesByTypeExpress, routesByTypeNight
            );
        }
    }


    @Getter
    enum RouteOptimizationCriteria {
        SHORTEST_DISTANCE("Кратчайшее расстояние"),
        FASTEST_TIME("Быстрейшее время"),
        LEAST_TRANSFERS("Минимум пересадок"),
        HIGHEST_FREQUENCY("Наивысшая частота"),
        LOWEST_COST("Наименьшая стоимость"),
        BEST_PERFORMANCE("Лучшая производительность");

        private final String description;

        RouteOptimizationCriteria(String description) {
            this.description = description;
        }

        @Override
        public String toString() { return description; }
    }
}