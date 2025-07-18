package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.services.RoutePerformanceService;

import java.util.List;

public  record PerformanceRecommendation(
        String recommendationType,
        String description,
        double expectedImprovement,
        RoutePerformanceService.ImplementationComplexity complexity,
        double estimatedCost,
        int priorityScore,
        List<String> actionSteps
) {}