package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public  record DynamicHeadwayPlan(
        Map<LocalTime, Integer> hourlyHeadways,
        int baseHeadwayMinutes,
        int peakReductionPercentage,
        List<HeadwayAdjustment> adjustments
) {}
