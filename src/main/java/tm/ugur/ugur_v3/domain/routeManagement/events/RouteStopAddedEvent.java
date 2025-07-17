/**
 * COMPONENT: RouteStopAddedEvent Domain Event
 * LAYER: Domain/RouteManagement/Events
 * PURPOSE: Event published when stop is added to route
 * PERFORMANCE TARGET: Lightweight event creation < 0.1ms
 * SCALABILITY: Immutable event for reliable event sourcing
 */
package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.Map;
import java.util.UUID;

@Getter
public class RouteStopAddedEvent implements DomainEvent {

    private final String eventId;
    private final RouteId routeId;
    private final StopId stopId;
    private final int position;
    private final GeoCoordinate stopLocation;
    private final String stopName;
    private final Timestamp occurredAt;
    private final int version;

    private RouteStopAddedEvent(RouteId routeId,
                                StopId stopId,
                                int position,
                                GeoCoordinate stopLocation,
                                String stopName,
                                Timestamp occurredAt) {
        this.eventId = UUID.randomUUID().toString();
        this.routeId = routeId;
        this.stopId = stopId;
        this.position = position;
        this.stopLocation = stopLocation;
        this.stopName = stopName;
        this.occurredAt = occurredAt;
        this.version = 1;
    }

    public static RouteStopAddedEvent of(RouteId routeId,
                                         StopId stopId,
                                         int position,
                                         GeoCoordinate stopLocation,
                                         String stopName,
                                         Timestamp occurredAt) {
        return new RouteStopAddedEvent(routeId, stopId, position, stopLocation, stopName, occurredAt);
    }

    public static RouteStopAddedEvent of(RouteId routeId,
                                         StopId stopId,
                                         int position,
                                         GeoCoordinate stopLocation,
                                         String stopName) {
        return new RouteStopAddedEvent(routeId, stopId, position, stopLocation, stopName, Timestamp.now());
    }

    public static RouteStopAddedEvent of(RouteId routeId,
                                         StopId stopId,
                                         int position) {
        return new RouteStopAddedEvent(routeId, stopId, position, null, null, Timestamp.now());
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
    public String getEventType() {
        return "RouteStopAdded";
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = Map.of(
                "aggregateType", "Route",
                "aggregateId", routeId.getValue(),
                "stopId", stopId.getValue(),
                "position", position,
                "eventVersion", version,
                "hasLocation", stopLocation != null,
                "hasStopName", stopName != null && !stopName.trim().isEmpty()
        );

        // Add location data if available
        if (stopLocation != null) {
            Map<String, Object> extendedMetadata = new java.util.HashMap<>(metadata);
            extendedMetadata.put("latitude", stopLocation.getLatitude());
            extendedMetadata.put("longitude", stopLocation.getLongitude());
            return Map.copyOf(extendedMetadata);
        }

        return metadata;
    }


    public boolean isFirstStop() {
        return position == 0;
    }

    public boolean hasValidLocation() {
        return stopLocation != null;
    }

    public boolean hasStopName() {
        return stopName != null && !stopName.trim().isEmpty();
    }

    // 🔥 EQUALS & HASHCODE

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RouteStopAddedEvent that = (RouteStopAddedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("RouteStopAddedEvent{eventId='%s', routeId=%s, stopId=%s, position=%d, timestamp=%s}",
                eventId.substring(0, 8) + "...",
                routeId,
                stopId,
                position,
                occurredAt
        );
    }
}