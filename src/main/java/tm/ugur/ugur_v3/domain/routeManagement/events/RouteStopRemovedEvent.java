package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class RouteStopRemovedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteId routeId;
    private final StopId stopId;
    private final int previousPosition;
    private final String removalReason;
    private final String removedBy;
    private final int newTotalStops;
    private final boolean requiresScheduleUpdate;
    private final Map<String, Object> metadata;

    private RouteStopRemovedEvent(RouteId routeId,
                                  StopId stopId,
                                  int previousPosition,
                                  String removalReason,
                                  String removedBy,
                                  int newTotalStops,
                                  boolean requiresScheduleUpdate,
                                  String correlationId,
                                  Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteStopRemoved";
        this.occurredAt = Timestamp.now();
        this.aggregateId = routeId.getValue();
        this.aggregateType = "Route";
        this.version = 1L;
        this.correlationId = correlationId;

        this.routeId = routeId;
        this.stopId = stopId;
        this.previousPosition = previousPosition;
        this.removalReason = removalReason;
        this.removedBy = removedBy;
        this.newTotalStops = newTotalStops;
        this.requiresScheduleUpdate = true;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RouteStopRemovedEvent of(
            RouteId routeId,
            StopId stopId,
            int previousPosition,
            String removalReason,
            String removedBy,
            int newTotalStops) {
        return new RouteStopRemovedEvent(
                routeId,
                stopId,
                previousPosition,
                removalReason,
                removedBy,
                newTotalStops,
                true,
                null,
                null);
    }
}