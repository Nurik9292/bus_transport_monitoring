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
public final class FrequencyPatternUpdatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteScheduleId scheduleId;
    private final RouteId routeId;
    private final String patternName;
    private final boolean replacedExisting;
    private final String modifiedBy;
    private final Map<String, Object> metadata;

    private FrequencyPatternUpdatedEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String patternName,
            boolean replacedExisting,
            String modifiedBy,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "FrequencyPatternUpdated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.patternName = patternName;
        this.replacedExisting = replacedExisting;
        this.modifiedBy = modifiedBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static FrequencyPatternUpdatedEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String patternName,
            Object pattern,
            boolean replacedExisting,
            String modifiedBy) {
        return new FrequencyPatternUpdatedEvent(
                scheduleId,
                routeId,
                patternName,
                replacedExisting,
                modifiedBy,
                null,
                null);
    }
}