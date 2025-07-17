package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteScheduleId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Set;

@Getter
public final class RouteScheduleActivatedEvent extends BaseRouteEvent {

    private final RouteScheduleId scheduleId;
    private final String scheduleName;
    private final Timestamp activatedAt;
    private final String activatedBy;
    private final int totalDailyTrips;
    private final Set<DayOfWeek> activeDays;
    private final boolean hasSpecialPeriods;
    private final String activationReason;

    private RouteScheduleActivatedEvent(RouteId routeId, RouteScheduleId scheduleId, String scheduleName,
                                        String activatedBy, int totalDailyTrips, Set<DayOfWeek> activeDays,
                                        boolean hasSpecialPeriods, String activationReason,
                                        String correlationId, Map<String, Object> metadata) {
        super("RouteScheduleActivated", routeId, correlationId, metadata);
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.activatedAt = Timestamp.now();
        this.activatedBy = activatedBy;
        this.totalDailyTrips = totalDailyTrips;
        this.activeDays = Set.copyOf(activeDays);
        this.hasSpecialPeriods = hasSpecialPeriods;
        this.activationReason = activationReason;
    }

    public static RouteScheduleActivatedEvent of(RouteId routeId, RouteScheduleId scheduleId, String scheduleName,
                                                 String activatedBy, int totalDailyTrips, Set<DayOfWeek> activeDays) {
        return new RouteScheduleActivatedEvent(routeId, scheduleId, scheduleName, activatedBy,
                totalDailyTrips, activeDays, false, "Manual activation", null, null);
    }

    public static RouteScheduleActivatedEvent automatic(RouteId routeId, RouteScheduleId scheduleId, String scheduleName,
                                                        int totalDailyTrips, Set<DayOfWeek> activeDays, String reason) {
        return new RouteScheduleActivatedEvent(routeId, scheduleId, scheduleName, "SYSTEM",
                totalDailyTrips, activeDays, false, reason, null, null);
    }

    public boolean isWeekdaysOnly() {
        return activeDays.stream().noneMatch(day -> day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
    }

    public boolean isHighFrequencySchedule() { return totalDailyTrips > 100; }

    @Override
    public String toString() {
        return String.format("RouteScheduleActivatedEvent{routeId=%s, scheduleId=%s, name='%s', trips=%d, days=%d, by='%s'}",
                routeId, scheduleId, scheduleName, totalDailyTrips, activeDays.size(), activatedBy);
    }
}