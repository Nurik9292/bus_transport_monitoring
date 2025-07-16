package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.OperatingHours;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteComplexity;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class RouteActivatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteId routeId;
    private final OperatingHours operatingHours;
    private final RouteComplexity complexity;
    private final Distance totalDistance;
    private final int totalStops;
    private final String activatedBy;
    private final Map<String, Object> metadata;

    private RouteActivatedEvent(RouteId routeId, OperatingHours operatingHours, RouteComplexity complexity,
                                Distance totalDistance, int totalStops, String activatedBy,
                                String correlationId, Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteActivated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = routeId.getValue();
        this.aggregateType = "Route";
        this.version = 1L;
        this.correlationId = correlationId;

        this.routeId = routeId;
        this.operatingHours = operatingHours;
        this.complexity = complexity;
        this.totalDistance = totalDistance;
        this.totalStops = totalStops;
        this.activatedBy = activatedBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RouteActivatedEvent of(RouteId routeId, OperatingHours operatingHours,
                                         RouteComplexity complexity, Distance totalDistance,
                                         int totalStops, String activatedBy) {
        return new RouteActivatedEvent(routeId, operatingHours, complexity, totalDistance,
                totalStops, activatedBy, null, null);
    }

    public static RouteActivatedEvent of(RouteId routeId, OperatingHours operatingHours,
                                         RouteComplexity complexity, Distance totalDistance,
                                         int totalStops, String activatedBy, String correlationId) {
        return new RouteActivatedEvent(routeId, operatingHours, complexity, totalDistance,
                totalStops, activatedBy, correlationId, null);
    }
}
