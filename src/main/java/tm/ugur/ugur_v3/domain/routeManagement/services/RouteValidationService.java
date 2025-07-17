package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSchedule;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.List;
import java.util.Map;

public interface RouteValidationService extends DomainService {

    Mono<RouteValidationResult> validateRoute(Route route,
                                              Map<StopId, GeoCoordinate> stopLocations,
                                              ValidationCriteria criteria);

    Mono<ScheduleValidationResult> validateSchedule(RouteSchedule schedule,
                                                    Route route,
                                                    OperationalConstraints constraints);

    Mono<GeometryValidationResult> validateRouteGeometry(Route route,
                                                         Map<StopId, GeoCoordinate> stopLocations,
                                                         RoadNetworkData roadNetwork);

    Mono<PerformanceValidationResult> validatePerformanceRequirements(Route route,
                                                                      PerformanceRequirements requirements,
                                                                      CurrentPerformanceData currentData);

    Mono<AccessibilityValidationResult> validateAccessibility(Route route,
                                                              AccessibilityStandards standards,
                                                              InfrastructureCapabilities infrastructure);


    record RouteValidationResult(
            boolean isValid,
            ValidationSeverity overallSeverity,
            List<ValidationIssue> issues,
            List<ValidationWarning> warnings,
            ValidationSummary summary,
            List<String> recommendations
    ) {}

    record ScheduleValidationResult(
            boolean isValid,
            List<ScheduleValidationIssue> issues,
            ScheduleQualityAssessment qualityAssessment,
            OperationalFeasibility feasibility
    ) {}

    record GeometryValidationResult(
            boolean isGeometryValid,
            List<GeometryIssue> geometryIssues,
            RouteConnectivity connectivity,
            DistanceValidation distanceValidation
    ) {}

    record ValidationIssue(
            String issueType,
            ValidationSeverity severity,
            String description,
            String location,
            List<String> suggestedFixes
    ) {}

    enum ValidationSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }

    record ValidationCriteria(
            boolean strictMode,
            double maxRouteDistance,
            int maxStopsCount,
            boolean requireAccessibility,
            PerformanceThresholds performanceThresholds
    ) {}
}