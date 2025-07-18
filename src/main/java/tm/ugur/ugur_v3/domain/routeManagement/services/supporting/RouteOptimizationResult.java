package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.EstimatedDuration;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.List;

public record RouteOptimizationResult(
        List<StopId> optimizedSequence,
        Distance totalDistance,
        EstimatedDuration totalTime,
        double improvementPercentage,
        List<String> changesApplied,
        OptimizationMetrics metrics
) {}
