package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.services.RoutePerformanceService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.List;

public record PerformanceAnomaly(
        RoutePerformanceMetrics affectedMetric,
        double currentValue,
        double expectedValue,
        double deviationPercentage,
        RoutePerformanceService.AnomalySeverity severity,
        List<String> possibleCauses,
        Timestamp detectedAt
) {}
