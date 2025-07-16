package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class RouteDeactivatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteId routeId;
    private final String reason;
    private final String deactivatedBy;
    private final int affectedVehicles;
    private final boolean isTemporary;
    private final Timestamp expectedReactivation;
    private final Map<String, Object> metadata;

    private RouteDeactivatedEvent(
            RouteId routeId,
            String reason,
            String deactivatedBy,
            int affectedVehicles,
            boolean isTemporary,
            Timestamp expectedReactivation,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteDeactivated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = routeId.getValue();
        this.aggregateType = "Route";
        this.version = 1L;
        this.correlationId = correlationId;

        this.routeId = routeId;
        this.reason = reason;
        this.deactivatedBy = deactivatedBy;
        this.affectedVehicles = affectedVehicles;
        this.isTemporary = isTemporary;
        this.expectedReactivation = expectedReactivation;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RouteDeactivatedEvent of(
            RouteId routeId,
            String reason,
            String deactivatedBy,
            int affectedVehicles) {
        return new RouteDeactivatedEvent(
                routeId,
                reason,
                deactivatedBy,
                affectedVehicles,
                false,
                null,
                null,
                null);
    }

    public static RouteDeactivatedEvent of(
            RouteId routeId,
            String reason,
            String deactivatedBy,
            int affectedVehicles,
            boolean isTemporary,
            Timestamp expectedReactivation) {
        return new RouteDeactivatedEvent(
                routeId,
                reason,
                deactivatedBy,
                affectedVehicles,
                isTemporary,
                expectedReactivation,
                null,
                null);
    }
}