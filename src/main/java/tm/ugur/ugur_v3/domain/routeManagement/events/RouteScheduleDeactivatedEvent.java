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
public final class RouteScheduleDeactivatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteScheduleId scheduleId;
    private final RouteId routeId;
    private final String reason;
    private final String deactivatedBy;
    private final Map<String, Object> metadata;

    private RouteScheduleDeactivatedEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String reason,
            String deactivatedBy,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteScheduleDeactivated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.reason = reason;
        this.deactivatedBy = deactivatedBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RouteScheduleDeactivatedEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String reason,
            String deactivatedBy) {
        return new RouteScheduleDeactivatedEvent(
                scheduleId,
                routeId,
                reason,
                deactivatedBy,
                null,
                null);
    }
}