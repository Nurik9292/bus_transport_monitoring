package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.DailySchedule;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

@Getter
public final class DailyScheduleAddedEvent extends BaseRouteEvent {

    private final DayOfWeek dayOfWeek;
    private final DailySchedule dailySchedule;
    private final int tripCount;
    private final LocalTime firstDeparture;
    private final LocalTime lastDeparture;
    private final int averageIntervalMinutes;
    private final boolean isWeekend;
    private final boolean isHighFrequency;
    private final String addedBy;

    private DailyScheduleAddedEvent(RouteId routeId, DayOfWeek dayOfWeek, DailySchedule dailySchedule,
                                    String addedBy, String correlationId, Map<String, Object> metadata) {
        super("DailyScheduleAdded", routeId, correlationId, metadata);
        this.dayOfWeek = dayOfWeek;
        this.dailySchedule = dailySchedule;
        this.tripCount = dailySchedule.getTripCount();
        this.firstDeparture = dailySchedule.getFirstDeparture();
        this.lastDeparture = dailySchedule.getLastDeparture();
        this.averageIntervalMinutes = dailySchedule.getAverageIntervalMinutes();
        this.isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        this.isHighFrequency = averageIntervalMinutes <= 15;
        this.addedBy = addedBy;
    }

    public static DailyScheduleAddedEvent of(RouteId routeId, DayOfWeek dayOfWeek, DailySchedule dailySchedule, String addedBy) {
        return new DailyScheduleAddedEvent(routeId, dayOfWeek, dailySchedule, addedBy, null, null);
    }

    public boolean isExtendedService() {
        if (firstDeparture == null || lastDeparture == null) return false;
        return firstDeparture.isBefore(LocalTime.of(6, 0)) || lastDeparture.isAfter(LocalTime.of(23, 0));
    }

    public boolean isLimitedService() { return tripCount < 10; }
    public boolean isFullService() { return tripCount >= 50; }

    @Override
    public String toString() {
        return String.format("DailyScheduleAddedEvent{routeId=%s, day=%s, trips=%d, span=%s-%s, interval=%dmin, by='%s'}",
                routeId, dayOfWeek, tripCount, firstDeparture, lastDeparture, averageIntervalMinutes, addedBy);
    }
}