package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.TrackingSessionId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Bearing;

import java.util.Map;
import java.util.UUID;

@Getter
public final class GPSDataReceivedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;


    private final TrackingSessionId sessionId;
    private final VehicleId vehicleId;
    private final GeoCoordinate location;
    private final Speed speed;
    private final Bearing bearing;
    private final GeoCoordinate previousLocation;
    private final double distanceTraveled;
    private final DataQuality quality;


    private final Map<String, String> metadata;

    private GPSDataReceivedEvent(TrackingSessionId sessionId, VehicleId vehicleId,
                                 GeoCoordinate location, Speed speed, Bearing bearing,
                                 GeoCoordinate previousLocation, double distanceTraveled,
                                 DataQuality quality, String correlationId,
                                 Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "GPSDataReceived";
        this.occurredAt = Timestamp.now();
        this.aggregateId = sessionId.getValue();
        this.aggregateType = "VehicleTrackingSession";
        this.version = 1L;
        this.correlationId = correlationId;

        this.sessionId = sessionId;
        this.vehicleId = vehicleId;
        this.location = location;
        this.speed = speed;
        this.bearing = bearing;
        this.previousLocation = previousLocation;
        this.distanceTraveled = distanceTraveled;
        this.quality = quality != null ? quality : DataQuality.UNKNOWN;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static GPSDataReceivedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                          GeoCoordinate location, Speed speed, Bearing bearing,
                                          GeoCoordinate previousLocation, double distanceTraveled) {
        return new GPSDataReceivedEvent(
                sessionId, vehicleId, location, speed, bearing,
                previousLocation, distanceTraveled, DataQuality.GOOD, null, null
        );
    }

    public static GPSDataReceivedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                          GeoCoordinate location, Speed speed, Bearing bearing,
                                          GeoCoordinate previousLocation, double distanceTraveled,
                                          DataQuality quality) {
        return new GPSDataReceivedEvent(
                sessionId, vehicleId, location, speed, bearing,
                previousLocation, distanceTraveled, quality, null, null
        );
    }

    public static GPSDataReceivedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                          GeoCoordinate location, Speed speed, Bearing bearing,
                                          GeoCoordinate previousLocation, double distanceTraveled,
                                          DataQuality quality, String correlationId,
                                          Map<String, String> metadata) {
        return new GPSDataReceivedEvent(
                sessionId, vehicleId, location, speed, bearing,
                previousLocation, distanceTraveled, quality, correlationId, metadata
        );
    }

    public boolean isSignificantMovement() {
        return speed.isMoving() && distanceTraveled >= 5.0;
    }

    public boolean isHighQualityData() {
        return quality == DataQuality.EXCELLENT || quality == DataQuality.GOOD;
    }

    public boolean isVehicleStationary() {
        return speed.isStationary() && distanceTraveled < 2.0;
    }

    public boolean isHighwaySpeed() {
        return speed.getKmh() > 60.0;
    }

    public boolean indicatesTurning() {


        return speed.getKmh() > 5.0 && speed.getKmh() < 30.0;
    }

    public MovementType getMovementType() {
        if (isVehicleStationary()) {
            return MovementType.STATIONARY;
        } else if (speed.isSlow()) {
            return MovementType.SLOW;
        } else if (speed.isNormal()) {
            return MovementType.NORMAL;
        } else if (isHighwaySpeed()) {
            return MovementType.HIGHWAY;
        } else {
            return MovementType.FAST;
        }
    }

    public int getProcessingPriority() {
        if (!isHighQualityData()) {
            return 5;
        } else if (isVehicleStationary()) {
            return 4;
        } else if (isSignificantMovement()) {
            return 1;
        } else {
            return 3;
        }
    }

    public boolean shouldTriggerRealTimeUpdate() {
        return isHighQualityData() &&
                (isSignificantMovement() || isVehicleStationary());
    }

    public boolean shouldStoreForAnalytics() {
        return isHighQualityData() && isSignificantMovement();
    }

    public boolean shouldUpdateETA() {
        return isHighQualityData() &&
                speed.isMoving() &&
                distanceTraveled >= 10.0;
    }

    public double getGpsAccuracyMeters() {
        return location.getAccuracy();
    }

    public boolean isNavigationQuality() {
        return getGpsAccuracyMeters() <= 20.0;
    }

    public double getDistanceTraveledKm() {
        return distanceTraveled / 1000.0;
    }

    public long getTimeSinceLastUpdateSeconds() {
        if (previousLocation == null) return 0;


        if (speed.isStationary()) return 30;

        double speedMs = speed.getMs();
        return speedMs > 0 ? Math.round(distanceTraveled / speedMs) : 30;
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
        allMetadata.put("vehicleId", vehicleId.getValue());
        allMetadata.put("quality", quality.name());
        allMetadata.put("isMoving", !isVehicleStationary());
        allMetadata.put("isSignificantMovement", isSignificantMovement());
        allMetadata.put("movementType", getMovementType().name());
        allMetadata.put("processingPriority", getProcessingPriority());
        allMetadata.put("shouldTriggerRealTime", shouldTriggerRealTimeUpdate());
        allMetadata.put("shouldUpdateETA", shouldUpdateETA());
        allMetadata.put("isNavigationQuality", isNavigationQuality());


        allMetadata.put("lat", location.getLatitude());
        allMetadata.put("lng", location.getLongitude());
        allMetadata.put("accuracy", getGpsAccuracyMeters());
        allMetadata.put("speedKmh", speed.getKmh());
        allMetadata.put("bearingDeg", bearing.getDegrees());
        allMetadata.put("distanceM", distanceTraveled);

        return Map.copyOf(allMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        GPSDataReceivedEvent that = (GPSDataReceivedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("GPSDataReceivedEvent{eventId='%s', vehicleId=%s, location=(%.6f,%.6f), speed=%.1fkm/h, bearing=%.0f°, distance=%.1fm, quality=%s}",
                eventId.substring(0, 8) + "...",
                vehicleId,
                location.getLatitude(),
                location.getLongitude(),
                speed.getKmh(),
                bearing.getDegrees(),
                distanceTraveled,
                quality
        );
    }

    @Getter
    public enum DataQuality {
        EXCELLENT("Excellent", "green"),
        GOOD("Good", "lightgreen"),
        FAIR("Fair", "yellow"),
        POOR("Poor", "orange"),
        UNRELIABLE("Unreliable", "red"),
        UNKNOWN("Unknown", "gray");

        private final String description;
        private final String colorCode;

        DataQuality(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

        public static DataQuality fromAccuracy(double accuracyMeters) {
            if (accuracyMeters <= 5.0) {
                return EXCELLENT;
            } else if (accuracyMeters <= 15.0) {
                return GOOD;
            } else if (accuracyMeters <= 30.0) {
                return FAIR;
            } else if (accuracyMeters <= 100.0) {
                return POOR;
            } else {
                return UNRELIABLE;
            }
        }
    }

    @Getter
    public enum MovementType {
        STATIONARY("Stationary"),
        SLOW("Slow Movement"),
        NORMAL("Normal Movement"),
        FAST("Fast Movement"),
        HIGHWAY("Highway Speed");

        private final String description;

        MovementType(String description) {
            this.description = description;
        }

    }
}