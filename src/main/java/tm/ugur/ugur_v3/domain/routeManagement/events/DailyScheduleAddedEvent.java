package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteScheduleId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class DailyScheduleAddedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteScheduleId scheduleId;
    private final RouteId routeId;
    private final String dayOfWeek;
    private final int tripCount;
    private final boolean replacedExisting;
    private final String modifiedBy;
    private final Map<String, Object> metadata;

    private DailyScheduleAddedEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String dayOfWeek,
            int tripCount,
            boolean replacedExisting,
            String modifiedBy,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "DailyScheduleAdded";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.dayOfWeek = dayOfWeek;
        this.tripCount = tripCount;
        this.replacedExisting = replacedExisting;
        this.modifiedBy = modifiedBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static DailyScheduleAddedEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            DayOfWeek dayOfWeek,
            Object dailySchedule,
            boolean replacedExisting,
            String modifiedBy) {
        int tripCount = 0;
        if (dailySchedule != null) {
            try {
                Method getTripCount = dailySchedule.getClass().getMethod("getTripCount");
                tripCount = (Integer) getTripCount.invoke(dailySchedule);
            } catch (Exception e) {
            }
        }

        return new DailyScheduleAddedEvent(scheduleId, routeId, dayOfWeek.toString(),
                tripCount, replacedExisting, modifiedBy, null, null);
    }
}