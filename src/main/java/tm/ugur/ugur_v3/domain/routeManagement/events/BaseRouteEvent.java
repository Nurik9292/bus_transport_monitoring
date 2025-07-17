package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseRouteEvent implements DomainEvent {

    protected final String eventId;
    protected final String eventType;
    protected final Timestamp occurredAt;
    @Getter
    protected final RouteId routeId;
    protected final String correlationId;
    protected final Map<String, Object> metadata;

    protected BaseRouteEvent(String eventType, RouteId routeId, String correlationId, Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = Timestamp.now();
        this.routeId = routeId;
        this.correlationId = correlationId;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    @Override
    public String getEventId() { return eventId; }

    @Override
    public String getEventType() { return eventType; }

    @Override
    public Timestamp getOccurredAt() { return occurredAt; }

    @Override
    public String getAggregateId() { return routeId.getValue(); }

    @Override
    public String getAggregateType() { return "Route"; }

    @Override
    public String getCorrelationId() { return correlationId; }

    @Override
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }

}