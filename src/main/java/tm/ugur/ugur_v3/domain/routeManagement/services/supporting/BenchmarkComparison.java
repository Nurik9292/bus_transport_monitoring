package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.services.RoutePerformanceService;

import java.util.List;

public  record BenchmarkComparison(
        Map<RoutePerformanceMetrics, BenchmarkResult> comparisons,
        double overallRanking,
        List<String> strengthAreas,
        List<String> improvementAreas,
        RoutePerformanceService.CompetitivePosition position
) {}
