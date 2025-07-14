package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Bearing;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

import java.util.Map;
import java.util.UUID;

@Getter
public final class VehicleDepartedFromStopEvent implements DomainEvent {

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
    private final Speed departureSpeed;
    private final Bearing departureDirection;
    private final VehicleStatus vehicleStatus;
    private final Timestamp arrivalTime;
    private final long dwellTimeMinutes;
    private final Integer sequenceNumber;
    private final String nextStopId;

    private final Map<String, String> metadata;

    private VehicleDepartedFromStopEvent(VehicleId vehicleId, String stopId, String stopName,
                                         GeoCoordinate stopLocation, GeoCoordinate vehicleLocation,
                                         String routeId, String routeName, Speed departureSpeed,
                                         Bearing departureDirection, VehicleStatus vehicleStatus,
                                         Timestamp arrivalTime, long dwellTimeMinutes,
                                         Integer sequenceNumber, String nextStopId,
                                         String correlationId, Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "VehicleDepartedFromStop";
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
        this.departureSpeed = departureSpeed;
        this.departureDirection = departureDirection;
        this.vehicleStatus = vehicleStatus;
        this.arrivalTime = arrivalTime;
        this.dwellTimeMinutes = dwellTimeMinutes;
        this.sequenceNumber = sequenceNumber;
        this.nextStopId = nextStopId;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static VehicleDepartedFromStopEvent of(VehicleId vehicleId, String stopId, String stopName,
                                                  GeoCoordinate stopLocation, String routeId) {
        return new VehicleDepartedFromStopEvent(
                vehicleId, stopId, stopName, stopLocation, stopLocation,
                routeId, null, Speed.zero(), Bearing.north(), VehicleStatus.IN_ROUTE,
                null, 0L, null, null, null, null
        );
    }

    public static VehicleDepartedFromStopEvent of(VehicleId vehicleId, String stopId, String stopName,
                                                  GeoCoordinate stopLocation, GeoCoordinate vehicleLocation,
                                                  String routeId, String routeName, Speed departureSpeed,
                                                  Bearing departureDirection, Timestamp arrivalTime,
                                                  long dwellTimeMinutes, Integer sequenceNumber,
                                                  String nextStopId) {
        return new VehicleDepartedFromStopEvent(
                vehicleId, stopId, stopName, stopLocation, vehicleLocation,
                routeId, routeName, departureSpeed, departureDirection, VehicleStatus.IN_ROUTE,
                arrivalTime, dwellTimeMinutes, sequenceNumber, nextStopId, null, null
        );
    }

    public static VehicleDepartedFromStopEvent of(VehicleId vehicleId, String stopId, String stopName,
                                                  GeoCoordinate stopLocation, GeoCoordinate vehicleLocation,
                                                  String routeId, String routeName, Speed departureSpeed,
                                                  Bearing departureDirection, VehicleStatus vehicleStatus,
                                                  Timestamp arrivalTime, long dwellTimeMinutes,
                                                  Integer sequenceNumber, String nextStopId,
                                                  String correlationId, Map<String, String> metadata) {
        return new VehicleDepartedFromStopEvent(
                vehicleId, stopId, stopName, stopLocation, vehicleLocation,
                routeId, routeName, departureSpeed, departureDirection, vehicleStatus,
                arrivalTime, dwellTimeMinutes, sequenceNumber, nextStopId, correlationId, metadata
        );
    }

    public boolean hasGoodDepartureMomentum() {
        return departureSpeed != null && !departureSpeed.isStationary();
    }

    public boolean isNormalDwellTime() {
        return dwellTimeMinutes >= 0 && dwellTimeMinutes <= 5;
    }

    public boolean isExcessiveDwellTime() {
        return dwellTimeMinutes > 10;
    }

    public boolean isQuickDeparture() {
        return dwellTimeMinutes <= 2;
    }

    public boolean isDepartureDirectionCorrect() {


        return departureDirection != null &&
                departureSpeed != null &&
                !departureSpeed.isStationary();
    }

    public boolean isDepartureFromFinalStop() {
        return nextStopId == null ||
                metadata.containsKey("isFinalStop") &&
                        Boolean.parseBoolean(metadata.get("isFinalStop"));
    }

    public boolean isDepartureFromFirstStop() {
        return sequenceNumber != null && sequenceNumber == 1;
    }

    public DeparturePerformance getPerformanceRating() {
        if (isQuickDeparture() && hasGoodDepartureMomentum()) {
            return DeparturePerformance.EXCELLENT;
        } else if (isNormalDwellTime() && hasGoodDepartureMomentum()) {
            return DeparturePerformance.GOOD;
        } else if (isNormalDwellTime()) {
            return DeparturePerformance.ACCEPTABLE;
        } else {
            return DeparturePerformance.POOR;
        }
    }

    public double getLocationAccuracyMeters() {
        if (vehicleLocation == null || stopLocation == null) {
            return 0.0;
        }
        return vehicleLocation.distanceTo(stopLocation);
    }

    public boolean isLocationAccurate() {
        return getLocationAccuracyMeters() <= 100.0;
    }

    public int getProcessingPriority() {
        if (isExcessiveDwellTime() || isDepartureFromFinalStop()) {
            return 1;
        } else if (isDepartureFromFirstStop() || !hasGoodDepartureMomentum()) {
            return 2;
        } else {
            return 3;
        }
    }

    public boolean shouldTriggerETARecalculation() {
        return nextStopId != null &&
                isLocationAccurate() &&
                hasGoodDepartureMomentum();
    }

    public boolean shouldUpdatePassengerNotifications() {
        return nextStopId != null &&
                isLocationAccurate() &&
                hasGoodDepartureMomentum();
    }

    public boolean shouldRecordDwellTimeMetrics() {
        return arrivalTime != null &&
                dwellTimeMinutes >= 0 &&
                dwellTimeMinutes <= 60;
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
        allMetadata.put("dwellTimeMinutes", dwellTimeMinutes);
        allMetadata.put("hasGoodMomentum", hasGoodDepartureMomentum());
        allMetadata.put("isNormalDwellTime", isNormalDwellTime());
        allMetadata.put("isExcessiveDwellTime", isExcessiveDwellTime());
        allMetadata.put("performanceRating", getPerformanceRating().name());
        allMetadata.put("locationAccuracyMeters", getLocationAccuracyMeters());
        allMetadata.put("isLocationAccurate", isLocationAccurate());
        allMetadata.put("processingPriority", getProcessingPriority());
        allMetadata.put("shouldTriggerETA", shouldTriggerETARecalculation());
        allMetadata.put("shouldUpdateNotifications", shouldUpdatePassengerNotifications());
        allMetadata.put("shouldRecordMetrics", shouldRecordDwellTimeMetrics());

        if (routeName != null) {
            allMetadata.put("routeName", routeName);
        }
        if (sequenceNumber != null) {
            allMetadata.put("sequenceNumber", sequenceNumber);
            allMetadata.put("isDepartureFromFirstStop", isDepartureFromFirstStop());
            allMetadata.put("isDepartureFromFinalStop", isDepartureFromFinalStop());
        }
        if (nextStopId != null) {
            allMetadata.put("nextStopId", nextStopId);
        }
        if (departureSpeed != null) {
            allMetadata.put("departureSpeedKmh", departureSpeed.getKmh());
        }
        if (departureDirection != null) {
            allMetadata.put("departureDirectionDegrees", departureDirection.getDegrees());
            allMetadata.put("departureCompassDirection", departureDirection.getCompassDirection().name());
        }

        return Map.copyOf(allMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        VehicleDepartedFromStopEvent that = (VehicleDepartedFromStopEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("VehicleDepartedFromStopEvent{eventId='%s', vehicleId=%s, stop='%s', route='%s', dwell=%dm, timestamp=%s}",
                eventId.substring(0, 8) + "...",
                vehicleId,
                stopName != null ? stopName : stopId,
                routeName != null ? routeName : routeId,
                dwellTimeMinutes,
                occurredAt
        );
    }

    @Getter
    public enum DeparturePerformance {
        EXCELLENT("Excellent", "green"),
        GOOD("Good", "lightgreen"),
        ACCEPTABLE("Acceptable", "yellow"),
        POOR("Poor", "red");

        private final String description;
        private final String colorCode;

        DeparturePerformance(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

    }
}