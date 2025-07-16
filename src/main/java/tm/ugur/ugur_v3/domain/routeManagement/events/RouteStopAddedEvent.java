package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class RouteStopAddedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteId routeId;
    private final StopId stopId;
    private final int position;
    private final Distance distanceFromPrevious;
    private final int newTotalStops;
    private final String addedBy;
    private final Map<String, Object> metadata;

    private RouteStopAddedEvent(RouteId routeId,
                                StopId stopId,
                                int position,
                                Distance distanceFromPrevious,
                                int newTotalStops,
                                String addedBy,
                                String correlationId,
                                Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteStopAdded";
        this.occurredAt = Timestamp.now();
        this.aggregateId = routeId.getValue();
        this.aggregateType = "Route";
        this.version = 1L;
        this.correlationId = correlationId;

        this.routeId = routeId;
        this.stopId = stopId;
        this.position = position;
        this.distanceFromPrevious = distanceFromPrevious;
        this.newTotalStops = newTotalStops;
        this.addedBy = addedBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RouteStopAddedEvent of(RouteId routeId,
                                         StopId stopId,
                                         int position,
                                         Distance distanceFromPrevious,
                                         int newTotalStops) {
        return new RouteStopAddedEvent(
                routeId,
                stopId,
                position,
                distanceFromPrevious,
                newTotalStops,
                "SYSTEM",
                null,
                null);
    }

    public static RouteStopAddedEvent of(
            RouteId routeId,
            StopId stopId,
            int position,
            Distance distanceFromPrevious,
            int newTotalStops,
            String addedBy) {
        return new RouteStopAddedEvent(
                routeId,
                stopId,
                position,
                distanceFromPrevious,
                newTotalStops,
                addedBy,
                null,
                null);
    }
}