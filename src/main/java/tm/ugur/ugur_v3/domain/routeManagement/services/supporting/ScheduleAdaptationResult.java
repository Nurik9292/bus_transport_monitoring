package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSchedule;

import java.util.List;

public record ScheduleAdaptationResult(
        RouteSchedule adaptedSchedule,
        List<ScheduleModification> modifications,
        double reliabilityImpact,
        String adaptationReason
) {}