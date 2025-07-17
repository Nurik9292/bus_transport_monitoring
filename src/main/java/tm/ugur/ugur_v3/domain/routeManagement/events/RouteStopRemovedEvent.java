/**
 * COMPONENT: RouteStopRemovedEvent Domain Event
 * LAYER: Domain/RouteManagement/Events
 * PURPOSE: Event published when stop is removed from route
 * PERFORMANCE TARGET: Lightweight event creation < 0.1ms
 * SCALABILITY: Immutable event for reliable event sourcing
 */
package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.Map;
import java.util.UUID;

@Getter
public class RouteStopRemovedEvent implements DomainEvent {

    private final String eventId;
    private final RouteId routeId;
    private final StopId stopId;
    private final int previousPosition;
    private final String stopName;
    private final Timestamp occurredAt;
    private final int version;
    private final String reason;

    // 🔥 PRIVATE CONSTRUCTOR - Use factory method
    private RouteStopRemovedEvent(RouteId routeId,
                                  StopId stopId,
                                  int previousPosition,
                                  String stopName,
                                  String reason,
                                  Timestamp occurredAt) {
        this.eventId = UUID.randomUUID().toString();
        this.routeId = routeId;
        this.stopId = stopId;
        this.previousPosition = previousPosition;
        this.stopName = stopName;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.version = 1;
    }

    public static RouteStopRemovedEvent of(RouteId routeId,
                                           StopId stopId,
                                           int previousPosition,
                                           String stopName,
                                           String reason,
                                           Timestamp occurredAt) {
        return new RouteStopRemovedEvent(routeId, stopId, previousPosition, stopName, reason, occurredAt);
    }

    // 🔥 FACTORY METHOD - With current timestamp
    public static RouteStopRemovedEvent of(RouteId routeId,
                                           StopId stopId,
                                           int previousPosition,
                                           String reason) {
        return new RouteStopRemovedEvent(routeId, stopId, previousPosition, null, reason, Timestamp.now());
    }

    public static RouteStopRemovedEvent of(RouteId routeId,
                                           StopId stopId,
                                           int previousPosition) {
        return new RouteStopRemovedEvent(routeId, stopId, previousPosition, null, null, Timestamp.now());
    }

    @Override
    public String getAggregateId() {
        return routeId.getValue();
    }

    @Override
    public String getAggregateType() {
        return "Route";
    }

    @Override
    public String getCorrelationId() {
        return DomainEvent.super.getCorrelationId();
    }

    @Override
    public String getEventType() {
        return "RouteStopRemoved";
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = Map.of(
                "aggregateType", "Route",
                "aggregateId", routeId.getValue(),
                "stopId", stopId.getValue(),
                "previousPosition", previousPosition,
                "eventVersion", version,
                "hasStopName", stopName != null && !stopName.trim().isEmpty(),
                "hasReason", reason != null && !reason.trim().isEmpty()
        );

        // Add optional fields if available
        Map<String, Object> extendedMetadata = new java.util.HashMap<>(metadata);
        if (stopName != null && !stopName.trim().isEmpty()) {
            extendedMetadata.put("stopName", stopName);
        }
        if (reason != null && !reason.trim().isEmpty()) {
            extendedMetadata.put("reason", reason);
        }

        return Map.copyOf(extendedMetadata);
    }


    public boolean wasFirstStop() {
        return previousPosition == 0;
    }

    public boolean hasStopName() {
        return stopName != null && !stopName.trim().isEmpty();
    }

    public boolean hasReason() {
        return reason != null && !reason.trim().isEmpty();
    }

    public RemovalReason getRemovalReason() {
        if (reason == null || reason.trim().isEmpty()) {
            return RemovalReason.UNSPECIFIED;
        }

        String reasonLower = reason.toLowerCase().trim();
        return switch (reasonLower) {
            case "optimization", "route_optimization" -> RemovalReason.ROUTE_OPTIMIZATION;
            case "maintenance", "stop_maintenance" -> RemovalReason.STOP_MAINTENANCE;
            case "low_usage", "insufficient_demand" -> RemovalReason.LOW_USAGE;
            case "closure", "permanent_closure" -> RemovalReason.STOP_CLOSURE;
            case "schedule_change", "timetable_update" -> RemovalReason.SCHEDULE_CHANGE;
            default -> RemovalReason.OTHER;
        };
    }

    @Getter
    public enum RemovalReason {
        ROUTE_OPTIMIZATION("Route optimization"),
        STOP_MAINTENANCE("Stop maintenance"),
        LOW_USAGE("Low usage/demand"),
        STOP_CLOSURE("Stop closure"),
        SCHEDULE_CHANGE("Schedule change"),
        OTHER("Other reason"),
        UNSPECIFIED("Unspecified");

        private final String description;

        RemovalReason(String description) {
            this.description = description;
        }
    }

    // 🔥 EQUALS & HASHCODE

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RouteStopRemovedEvent that = (RouteStopRemovedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("RouteStopRemovedEvent{eventId='%s', routeId=%s, stopId=%s, position=%d, reason='%s', timestamp=%s}",
                eventId.substring(0, 8) + "...",
                routeId,
                stopId,
                previousPosition,
                reason != null ? reason : "none",
                occurredAt
        );
    }
}