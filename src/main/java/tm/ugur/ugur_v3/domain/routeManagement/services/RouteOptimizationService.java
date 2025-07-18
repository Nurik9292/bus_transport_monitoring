package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.geospatial.services.GeoSpatialService;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSegment;
import tm.ugur.ugur_v3.domain.routeManagement.services.supporting.*;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.List;
import java.util.Map;

public interface RouteOptimizationService extends DomainService {


    Mono<RouteOptimizationResult> optimizeStopSequence(Route route,
                                                       Map<StopId, GeoCoordinate> stopLocations,
                                                       OptimizationCriteria criteria);

    Mono<List<RouteSegment>> findOptimalPath(List<StopId> stopIds,
                                             Map<StopId, GeoCoordinate> stopLocations,
                                             GeoSpatialService geoSpatialService);

    Mono<FrequencyOptimizationResult> optimizeFrequency(Route route,
                                                        PassengerDemandData demandData,
                                                        VehicleCapacityConstraints constraints);

    Mono<List<RouteImprovement>> suggestImprovements(Route route,
                                                     RoutePerformanceMetrics performance,
                                                     List<PassengerFeedback> feedback);

    Mono<RouteEfficiencyScore> calculateEfficiency(Route route,
                                                   RouteOperationalData operationalData);


}