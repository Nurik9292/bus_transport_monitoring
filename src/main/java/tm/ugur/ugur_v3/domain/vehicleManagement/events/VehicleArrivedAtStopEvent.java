package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

import java.util.Map;
import java.util.UUID;

@Getter
public final class VehicleArrivedAtStopEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final VehicleId vehicleId;
    private final String stopId;
    private final String stopName;
    private final GeoCoordinate stopLocation;
    private final GeoCoordinate vehicleLocation;
    private final String routeId;
    private final String routeName;
    private final Speed arrivalSpeed;
    private final VehicleStatus vehicleStatus;
    private final Timestamp scheduledArrivalTime;
    private final long delayMinutes;
    private final Integer sequenceNumber;

    private final Map<String, String> metadata;

    private VehicleArrivedAtStopEvent(VehicleId vehicleId, String stopId, String stopName,
                                      GeoCoordinate stopLocation, GeoCoordinate vehicleLocation,
                                      String routeId, String routeName, Speed arrivalSpeed,
                                      VehicleStatus vehicleStatus, Timestamp scheduledArrivalTime,
                                      long delayMinutes, Integer sequenceNumber,
                                      String correlationId, Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "VehicleArrivedAtStop";
        this.occurredAt = Timestamp.now();
        this.aggregateId = vehicleId.getValue();
        this.aggregateType = "Vehicle";
        this.version = 1L;
        this.correlationId = correlationId;

        this.vehicleId = vehicleId;
        this.stopId = stopId;
        this.stopName = stopName;
        this.stopLocation = stopLocation;
        this.vehicleLocation = vehicleLocation;
        this.routeId = routeId;
        this.routeName = routeName;
        this.arrivalSpeed = arrivalSpeed;
        this.vehicleStatus = vehicleStatus;
        this.scheduledArrivalTime = scheduledArrivalTime;
        this.delayMinutes = delayMinutes;
        this.sequenceNumber = sequenceNumber;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static VehicleArrivedAtStopEvent of(VehicleId vehicleId, String stopId, String stopName,
                                               GeoCoordinate stopLocation, String routeId) {
        return new VehicleArrivedAtStopEvent(
                vehicleId, stopId, stopName, stopLocation, stopLocation,
                routeId, null, Speed.zero(), VehicleStatus.IN_ROUTE,
                null, 0L, null, null, null
        );
    }

    public static VehicleArrivedAtStopEvent of(VehicleId vehicleId, String stopId, String stopName,
                                               GeoCoordinate stopLocation, GeoCoordinate vehicleLocation,
                                               String routeId, String routeName, Speed arrivalSpeed,
                                               Timestamp scheduledArrivalTime, long delayMinutes,
                                               Integer sequenceNumber) {
        return new VehicleArrivedAtStopEvent(
                vehicleId, stopId, stopName, stopLocation, vehicleLocation,
                routeId, routeName, arrivalSpeed, VehicleStatus.IN_ROUTE,
                scheduledArrivalTime, delayMinutes, sequenceNumber, null, null
        );
    }

    public static VehicleArrivedAtStopEvent of(VehicleId vehicleId, String stopId, String stopName,
                                               GeoCoordinate stopLocation, GeoCoordinate vehicleLocation,
                                               String routeId, String routeName, Speed arrivalSpeed,
                                               VehicleStatus vehicleStatus, Timestamp scheduledArrivalTime,
                                               long delayMinutes, Integer sequenceNumber,
                                               String correlationId, Map<String, String> metadata) {
        return new VehicleArrivedAtStopEvent(
                vehicleId, stopId, stopName, stopLocation, vehicleLocation,
                routeId, routeName, arrivalSpeed, vehicleStatus,
                scheduledArrivalTime, delayMinutes, sequenceNumber, correlationId, metadata
        );
    }

    public boolean isOnTime() {
        return Math.abs(delayMinutes) <= 2;
    }

    public boolean isLate() {
        return delayMinutes > 2;
    }

    public boolean isEarly() {
        return delayMinutes < -2;
    }

    public boolean isSignificantDelay() {
        return delayMinutes > 5;
    }

    public boolean isFirstStop() {
        return sequenceNumber != null && sequenceNumber == 1;
    }

    public boolean isFinalStop() {
        // This would need route information to determine
        // For now, we'll use metadata or external lookup
        return metadata.containsKey("isFinalStop") &&
                Boolean.parseBoolean(metadata.get("isFinalStop"));
    }

    public PerformanceRating getPerformanceRating() {
        if (isOnTime()) {
            return PerformanceRating.EXCELLENT;
        } else if (Math.abs(delayMinutes) <= 5) {
            return PerformanceRating.GOOD;
        } else if (Math.abs(delayMinutes) <= 10) {
            return PerformanceRating.ACCEPTABLE;
        } else {
            return PerformanceRating.POOR;
        }
    }

    public double getLocationAccuracyMeters() {
        if (vehicleLocation == null || stopLocation == null) {
            return 0.0;
        }
        return vehicleLocation.distanceTo(stopLocation);
    }

    public boolean isLocationAccurate() {
        return getLocationAccuracyMeters() <= 50.0;
    }

    public int getProcessingPriority() {
        if (isSignificantDelay() || isFinalStop()) {
            return 1;
        } else if (isLate()) {
            return 2;
        } else {
            return 3;
        }
    }

    public boolean shouldTriggerPassengerNotifications() {
        return isLocationAccurate() && (isOnTime() || isLate());
    }

    public boolean shouldUpdateETACalculations() {
        return !isFinalStop() && isLocationAccurate();
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
        allMetadata.put("stopId", stopId);
        allMetadata.put("stopName", stopName);
        allMetadata.put("routeId", routeId);
        allMetadata.put("isOnTime", isOnTime());
        allMetadata.put("isLate", isLate());
        allMetadata.put("delayMinutes", delayMinutes);
        allMetadata.put("performanceRating", getPerformanceRating().name());
        allMetadata.put("locationAccuracyMeters", getLocationAccuracyMeters());
        allMetadata.put("isLocationAccurate", isLocationAccurate());
        allMetadata.put("processingPriority", getProcessingPriority());
        allMetadata.put("shouldTriggerNotifications", shouldTriggerPassengerNotifications());
        allMetadata.put("shouldUpdateETA", shouldUpdateETACalculations());

        if (routeName != null) {
            allMetadata.put("routeName", routeName);
        }
        if (sequenceNumber != null) {
            allMetadata.put("sequenceNumber", sequenceNumber);
            allMetadata.put("isFirstStop", isFirstStop());
            allMetadata.put("isFinalStop", isFinalStop());
        }
        if (arrivalSpeed != null) {
            allMetadata.put("arrivalSpeedKmh", arrivalSpeed.getKmh());
        }

        return Map.copyOf(allMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        VehicleArrivedAtStopEvent that = (VehicleArrivedAtStopEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("VehicleArrivedAtStopEvent{eventId='%s', vehicleId=%s, stop='%s', route='%s', delay=%dm, timestamp=%s}",
                eventId.substring(0, 8) + "...",
                vehicleId,
                stopName != null ? stopName : stopId,
                routeName != null ? routeName : routeId,
                delayMinutes,
                occurredAt
        );
    }

    @Getter
    public enum PerformanceRating {
        EXCELLENT("Excellent", "green"),
        GOOD("Good", "lightgreen"),
        ACCEPTABLE("Acceptable", "yellow"),
        POOR("Poor", "red");

        private final String description;
        private final String colorCode;

        PerformanceRating(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

    }
}