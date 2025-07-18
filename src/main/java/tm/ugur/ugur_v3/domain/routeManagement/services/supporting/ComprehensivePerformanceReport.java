package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.List;
import java.util.Map;

public  record ComprehensivePerformanceReport(
        RouteId routeId,
        OverallPerformanceScore overallScore,
        Map<RoutePerformanceMetrics, Double> metricValues,
        PerformanceTrends trends,
        List<KeyInsight> insights,
        PerformanceGrading grading
) {}