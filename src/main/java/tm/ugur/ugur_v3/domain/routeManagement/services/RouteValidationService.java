package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSchedule;
import tm.ugur.ugur_v3.domain.routeManagement.services.supporting.*;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.Map;

public interface RouteValidationService extends DomainService {

    Mono<RouteValidationResult> validateRoute(Route route,
                                              Map<StopId, GeoCoordinate> stopLocations,
                                              ValidationCriteria criteria);

    Mono<ScheduleValidationResult> validateSchedule(RouteSchedule schedule,
                                                    Route route,
                                                    SchedulePlanningService.OperationalConstraints constraints);

    Mono<GeometryValidationResult> validateRouteGeometry(Route route,
                                                         Map<StopId, GeoCoordinate> stopLocations,
                                                         RoadNetworkData roadNetwork);

    Mono<PerformanceValidationResult> validatePerformanceRequirements(Route route,
                                                                      PerformanceRequirements requirements,
                                                                      CurrentPerformanceData currentData);

    Mono<AccessibilityValidationResult> validateAccessibility(Route route,
                                                              AccessibilityStandards standards,
                                                              InfrastructureCapabilities infrastructure);













}