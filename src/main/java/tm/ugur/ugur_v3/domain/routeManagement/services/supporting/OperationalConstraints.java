package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import java.time.LocalTime;

public record OperationalConstraints(
        LocalTime earliestStartTime,
        LocalTime latestEndTime,
        int minHeadwayMinutes,
        int maxHeadwayMinutes,
        int driverShiftDurationHours,
        double budgetConstraint
) {}