package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteScheduleId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class ScheduleDynamicallyAdjustedEvent implements DomainEvent {

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
    private final String adjustmentTime;
    private final int adjustmentMinutes;
    private final String reason;
    private final String adjustedBy;
    private final Map<String, Object> metadata;

    private ScheduleDynamicallyAdjustedEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String dayOfWeek,
            String adjustmentTime,
            int adjustmentMinutes,
            String reason,
            String adjustedBy,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "ScheduleDynamicallyAdjusted";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.dayOfWeek = dayOfWeek;
        this.adjustmentTime = adjustmentTime;
        this.adjustmentMinutes = adjustmentMinutes;
        this.reason = reason;
        this.adjustedBy = adjustedBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static ScheduleDynamicallyAdjustedEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            DayOfWeek dayOfWeek,
            LocalTime time,
            int adjustmentMinutes,
            String reason,
            String adjustedBy) {
        return new ScheduleDynamicallyAdjustedEvent(
                scheduleId,
                routeId,
                dayOfWeek.toString(),
                time.toString(),
                adjustmentMinutes,
                reason,
                adjustedBy,
                null,
                null);
    }
}