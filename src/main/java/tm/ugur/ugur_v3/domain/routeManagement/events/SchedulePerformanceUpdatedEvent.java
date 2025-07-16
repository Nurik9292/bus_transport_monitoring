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
public final class SchedulePerformanceUpdatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteScheduleId scheduleId;
    private final RouteId routeId;
    private final double currentAdherence;
    private final double previousAdherence;
    private final int missedTrips;
    private final double loadFactor;
    private final Map<String, Object> metadata;

    private SchedulePerformanceUpdatedEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            double currentAdherence,
            double previousAdherence,
            int missedTrips,
            double loadFactor,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "SchedulePerformanceUpdated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.currentAdherence = currentAdherence;
        this.previousAdherence = previousAdherence;
        this.missedTrips = missedTrips;
        this.loadFactor = loadFactor;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static SchedulePerformanceUpdatedEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            double currentAdherence,
            double previousAdherence,
            int missedTrips,
            double loadFactor) {
        return new SchedulePerformanceUpdatedEvent(
                scheduleId,
                routeId,
                currentAdherence,
                previousAdherence,
                missedTrips,
                loadFactor,
                null,
                null);
    }
}