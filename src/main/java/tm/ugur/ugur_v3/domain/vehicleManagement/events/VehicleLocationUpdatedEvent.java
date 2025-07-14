package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.util.Map;
import java.util.UUID;

@Getter
public final class VehicleLocationUpdatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final VehicleId vehicleId;
    private final GeoCoordinate location;
    private final GeoCoordinate previousLocation;
    private final Double speed;
    private final Double bearing;
    private final String assignedRouteId;
    private final VehicleStatus vehicleStatus;
    private final Double distanceTraveled;

    private final Map<String, String> metadata;

    private VehicleLocationUpdatedEvent(VehicleId vehicleId, GeoCoordinate location,
                                        GeoCoordinate previousLocation, Double speed, Double bearing,
                                        String assignedRouteId, VehicleStatus vehicleStatus,
                                        String correlationId, Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "VehicleLocationUpdated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = vehicleId.getValue();
        this.aggregateType = "Vehicle";
        this.version = 1L;
        this.correlationId = correlationId;

        this.vehicleId = vehicleId;
        this.location = location;
        this.previousLocation = previousLocation;
        this.speed = speed;
        this.bearing = bearing;
        this.assignedRouteId = assignedRouteId;
        this.vehicleStatus = vehicleStatus;
        this.distanceTraveled = calculateDistanceTraveled();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static VehicleLocationUpdatedEvent of(VehicleId vehicleId, GeoCoordinate location,
                                                 Double speed, Double bearing, String assignedRouteId,
                                                 VehicleStatus vehicleStatus) {
        return new VehicleLocationUpdatedEvent(
                vehicleId, location, null, speed, bearing,
                assignedRouteId, vehicleStatus, null, null
        );
    }

    public static VehicleLocationUpdatedEvent of(VehicleId vehicleId, GeoCoordinate location,
                                                 GeoCoordinate previousLocation, Double speed, Double bearing,
                                                 String assignedRouteId, VehicleStatus vehicleStatus) {
        return new VehicleLocationUpdatedEvent(
                vehicleId, location, previousLocation, speed, bearing,
                assignedRouteId, vehicleStatus, null, null
        );
    }

    public static VehicleLocationUpdatedEvent of(VehicleId vehicleId, GeoCoordinate location,
                                                 GeoCoordinate previousLocation, Double speed, Double bearing,
                                                 String assignedRouteId, VehicleStatus vehicleStatus,
                                                 String correlationId, Map<String, String> metadata) {
        return new VehicleLocationUpdatedEvent(
                vehicleId, location, previousLocation, speed, bearing,
                assignedRouteId, vehicleStatus, correlationId, metadata
        );
    }

    private Double calculateDistanceTraveled() {
        if (previousLocation != null && location != null) {
            return previousLocation.distanceTo(location);
        }
        return null;
    }



    public boolean isVehicleMoving() {
        return speed != null && speed > 5.0;
    }

    public boolean isSignificantMovement() {
        return distanceTraveled != null && distanceTraveled > 10.0;
    }

    public boolean isActiveOnRoute() {
        return assignedRouteId != null &&
                vehicleStatus == VehicleStatus.IN_ROUTE;
    }

    public boolean isHighPriority() {
        return isActiveOnRoute() && isVehicleMoving();
    }

    public boolean shouldRecalculateEta() {
        return isActiveOnRoute() && isSignificantMovement();
    }

    public boolean shouldBroadcastToWebSocket() {
        return vehicleStatus.isTrackable() &&
                (isSignificantMovement() || isActiveOnRoute());
    }

    public int getProcessingPriority() {
        if (vehicleStatus == VehicleStatus.BREAKDOWN) {
            return 1;
        }
        if (isActiveOnRoute() && isVehicleMoving()) {
            return 2;
        }
        if (vehicleStatus.isTrackable()) {
            return 3;
        }
        return 4;
    }

    public String toLogString() {
        return String.format("VehicleLocation[%s]: lat=%.6f, lng=%.6f, speed=%.1f, route=%s, status=%s",
                vehicleId.getValue(),
                location.getLatitude(),
                location.getLongitude(),
                speed != null ? speed : 0.0,
                assignedRouteId != null ? assignedRouteId : "NONE",
                vehicleStatus
        );
    }

    public Map<String, Object> toWebSocketPayload() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("vehicleId", vehicleId.getValue());
        payload.put("latitude", location.getLatitude());
        payload.put("longitude", location.getLongitude());
        payload.put("accuracy", location.getAccuracy());
        payload.put("speed", speed);
        payload.put("bearing", bearing);
        payload.put("timestamp", occurredAt.getEpochMillis());
        payload.put("routeId", assignedRouteId);
        payload.put("status", vehicleStatus.name());
        payload.put("isMoving", isVehicleMoving());

        if (distanceTraveled != null) {
            payload.put("distanceTraveled", Math.round(distanceTraveled));
        }

        return Map.copyOf(payload);
    }

    public Map<String, Object> toEtaCalculationPayload() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("vehicleId", vehicleId.getValue());
        payload.put("routeId", assignedRouteId);
        payload.put("location", Map.of(
                "lat", location.getLatitude(),
                "lng", location.getLongitude(),
                "accuracy", location.getAccuracy()
        ));
        payload.put("speed", speed);
        payload.put("bearing", bearing);
        payload.put("timestamp", occurredAt.getEpochMillis());

        if (previousLocation != null) {
            payload.put("previousLocation", Map.of(
                    "lat", previousLocation.getLatitude(),
                    "lng", previousLocation.getLongitude()
            ));
            payload.put("distanceTraveled", distanceTraveled);
        }

        return Map.copyOf(payload);
    }


    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public Timestamp getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public Map<String, Object> getMetadata() {

        Map<String, Object> allMetadata = new java.util.HashMap<>(metadata);
        allMetadata.put("vehicleStatus", vehicleStatus.name());
        allMetadata.put("isMoving", isVehicleMoving());
        allMetadata.put("isOnRoute", isActiveOnRoute());
        allMetadata.put("processingPriority", getProcessingPriority());

        if (assignedRouteId != null) {
            allMetadata.put("routeId", assignedRouteId);
        }

        return Map.copyOf(allMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        VehicleLocationUpdatedEvent that = (VehicleLocationUpdatedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("VehicleLocationUpdatedEvent{eventId='%s', vehicleId=%s, location=%s, speed=%.1f, status=%s, timestamp=%s}",
                eventId.substring(0, 8) + "...",
                vehicleId,
                location,
                speed != null ? speed : 0.0,
                vehicleStatus,
                occurredAt
        );
    }
}