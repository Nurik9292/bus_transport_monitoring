package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.SchedulePeriod;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

@Getter
public final class SchedulePeriodAddedEvent extends BaseRouteEvent {

    private final SchedulePeriod schedulePeriod;
    private final String periodName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Set<DayOfWeek> operatingDays;
    private final int headwayMinutes;
    private final SchedulePeriod.PeriodType periodType;
    private final String addedBy;

    private SchedulePeriodAddedEvent(RouteId routeId, SchedulePeriod schedulePeriod, String addedBy,
                                     String correlationId, Map<String, Object> metadata) {
        super("SchedulePeriodAdded", routeId, correlationId, metadata);
        this.schedulePeriod = schedulePeriod;
        this.periodName = schedulePeriod.getName();
        this.startTime = schedulePeriod.getStartTime();
        this.endTime = schedulePeriod.getEndTime();
        this.operatingDays = schedulePeriod.getOperatingDays();
        this.headwayMinutes = schedulePeriod.getHeadwayMinutes();
        this.periodType = schedulePeriod.getPeriodType();
        this.addedBy = addedBy;
    }

    public static SchedulePeriodAddedEvent of(RouteId routeId, SchedulePeriod schedulePeriod, String addedBy) {
        return new SchedulePeriodAddedEvent(routeId, schedulePeriod, addedBy, null, null);
    }

    public static SchedulePeriodAddedEvent of(RouteId routeId, SchedulePeriod schedulePeriod, String addedBy,
                                              String correlationId) {
        return new SchedulePeriodAddedEvent(routeId, schedulePeriod, addedBy, correlationId, null);
    }

    public boolean isRushHourPeriod() { return periodType == SchedulePeriod.PeriodType.RUSH_HOUR; }
    public boolean isHighFrequency() { return headwayMinutes <= 10; }
    public boolean isWeekendOnly() { return operatingDays.contains(DayOfWeek.SATURDAY) || operatingDays.contains(DayOfWeek.SUNDAY); }

    @Override
    public String toString() {
        return String.format("SchedulePeriodAddedEvent{routeId=%s, period='%s', type=%s, headway=%dmin, days=%s}",
                routeId, periodName, periodType, headwayMinutes, operatingDays.size());
    }
}