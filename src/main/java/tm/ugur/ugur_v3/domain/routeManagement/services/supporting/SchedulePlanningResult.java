package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSchedule;
import tm.ugur.ugur_v3.domain.routeManagement.enums.ServiceFrequency;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.DailySchedule;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.SchedulePeriod;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public  record SchedulePlanningResult(
        RouteSchedule proposedSchedule,
        ServiceFrequency recommendedFrequency,
        Map<DayOfWeek, DailySchedule> weeklySchedule,
        List<SchedulePeriod> specialPeriods,
        ScheduleQualityMetrics qualityMetrics
) {}