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
public final class RouteScheduleCreatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteScheduleId scheduleId;
    private final RouteId routeId;
    private final String scheduleName;
    private final String scheduleType;
    private final Timestamp effectiveFrom;
    private final Timestamp effectiveTo;
    private final String createdBy;
    private final Map<String, Object> metadata;

    private RouteScheduleCreatedEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String scheduleName,
            String scheduleType,
            Timestamp effectiveFrom,
            Timestamp effectiveTo,
            String createdBy,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteScheduleCreated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.scheduleName = scheduleName;
        this.scheduleType = scheduleType;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdBy = createdBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RouteScheduleCreatedEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String scheduleName,
            Object scheduleType,
            Timestamp effectiveFrom,
            Timestamp effectiveTo,
            String createdBy) {
        return new RouteScheduleCreatedEvent(
                scheduleId,
                routeId,
                scheduleName,
                scheduleType.toString(),
                effectiveFrom,
                effectiveTo,
                createdBy,
                null,
                null);
    }
}