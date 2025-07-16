/**
 * COMPONENT: RouteCreatedEvent - ИСПРАВЛЕННАЯ ВЕРСИЯ
 * LAYER: Domain/RouteManagement/Events
 * PURPOSE: Событие создания нового маршрута - ТОЛЬКО данные создания
 * PERFORMANCE TARGET: < 1ms для создания события
 */
package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteType;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteDirection;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;


@Getter
public final class RouteCreatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteId routeId;
    private final String routeName;
    private final RouteType routeType;
    private final RouteDirection direction;
    private final boolean isCircular;
    private final String createdBy;
    private final Map<String, Object> metadata;


    private RouteCreatedEvent(RouteId routeId, String routeName, RouteType routeType,
                              RouteDirection direction, boolean isCircular, String createdBy,
                              String correlationId, Map<String, Object> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteCreated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = routeId.getValue();
        this.aggregateType = "Route";
        this.version = 1L;
        this.correlationId = correlationId;

        this.routeId = routeId;
        this.routeName = routeName;
        this.routeType = routeType;
        this.direction = direction;
        this.isCircular = isCircular;
        this.createdBy = createdBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();


    }


    public static RouteCreatedEvent of(
            RouteId routeId,
            String routeName,
            RouteType routeType,
            RouteDirection direction,
            boolean isCircular,
            String createdBy,
            String correlationId) {
        return new RouteCreatedEvent(
                routeId,
                routeName,
                routeType,
                direction,
                isCircular,
                createdBy,
                correlationId,
                null);
    }


    public static RouteCreatedEvent of(
            RouteId routeId,
            String routeName,
            RouteType routeType,
            RouteDirection direction,
            boolean isCircular,
            String createdBy,
            String correlationId,
            Map<String, Object> metadata) {
        return new RouteCreatedEvent(
                routeId,
                routeName,
                routeType,
                direction,
                isCircular,
                createdBy,
                correlationId,
                metadata);
    }
}