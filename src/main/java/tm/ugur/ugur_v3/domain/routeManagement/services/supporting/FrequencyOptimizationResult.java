package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

public record FrequencyOptimizationResult(
        int recommendedHeadwayMinutes,
        int peakHeadwayMinutes,
        int offPeakHeadwayMinutes,
        double expectedLoadFactor,
        int estimatedDailyRidership
) {}
