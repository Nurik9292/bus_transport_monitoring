package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import java.util.List;

public record PerformanceForecast(
        Map<RoutePerformanceMetrics, ForecastValue> predictions,
        ConfidenceInterval confidenceInterval,
        List<ForecastAssumption> assumptions,
        RiskFactors riskFactors
) {}