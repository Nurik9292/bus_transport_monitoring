package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

public record ValidationCriteria(
        boolean strictMode,
        double maxRouteDistance,
        int maxStopsCount,
        boolean requireAccessibility,
        PerformanceThresholds performanceThresholds
) {}
