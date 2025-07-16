package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteScheduleId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class RouteScheduleActivatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteScheduleId scheduleId;
    private final RouteId routeId;
    private final int totalDailyTrips;
    private final String baseFrequency;
    private final String activatedBy;
    private final Map<String, Object> metadata;

    private RouteScheduleActivatedEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            int totalDailyTrips,
            String baseFrequency,
            String activatedBy,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteScheduleActivated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.totalDailyTrips = totalDailyTrips;
        this.baseFrequency = baseFrequency;
        this.activatedBy = activatedBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RouteScheduleActivatedEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            int totalDailyTrips,
            Object baseFrequency,
            String activatedBy) {
        return new RouteScheduleActivatedEvent(
                scheduleId,
                routeId,
                totalDailyTrips,
                baseFrequency.toString(),
                activatedBy,
                null,
                null);
    }
}