package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

import java.util.Map;
import java.util.UUID;

@Getter
public final class VehicleCreatedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final VehicleId vehicleId;
    private final String licensePlate;
    private final VehicleType vehicleType;
    private final Integer capacity;
    private final String model;
    private final Integer yearManufactured;
    private final String createdBy;

    private final Map<String, String> metadata;

    private VehicleCreatedEvent(VehicleId vehicleId, String licensePlate, VehicleType vehicleType,
                                Integer capacity, String model, Integer yearManufactured,
                                String createdBy, String correlationId, Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "VehicleCreated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = vehicleId.getValue();
        this.aggregateType = "Vehicle";
        this.version = 1L;
        this.correlationId = correlationId;

        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.model = model;
        this.yearManufactured = yearManufactured;
        this.createdBy = createdBy;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static VehicleCreatedEvent of(VehicleId vehicleId, String licensePlate, VehicleType vehicleType) {
        return new VehicleCreatedEvent(
                vehicleId, licensePlate, vehicleType, null, null, null,
                "SYSTEM", null, null
        );
    }

    public static VehicleCreatedEvent of(VehicleId vehicleId, String licensePlate, VehicleType vehicleType,
                                         Integer capacity, String model, Integer yearManufactured,
                                         String createdBy) {
        return new VehicleCreatedEvent(
                vehicleId, licensePlate, vehicleType, capacity, model, yearManufactured,
                createdBy, null, null
        );
    }

    public static VehicleCreatedEvent of(VehicleId vehicleId, String licensePlate, VehicleType vehicleType,
                                         Integer capacity, String model, Integer yearManufactured,
                                         String createdBy, String correlationId, Map<String, String> metadata) {
        return new VehicleCreatedEvent(
                vehicleId, licensePlate, vehicleType, capacity, model, yearManufactured,
                createdBy, correlationId, metadata
        );
    }

    public boolean isPublicTransportVehicle() {
        return vehicleType == VehicleType.BUS ||
                vehicleType == VehicleType.TROLLEY ||
                vehicleType == VehicleType.TRAM ||
                vehicleType == VehicleType.MINIBUS;
    }

    public boolean requiresGpsTracking() {
        return isPublicTransportVehicle();
    }

    public boolean shouldIncludeInFleetAnalytics() {
        return true;
    }

    public int getProcessingPriority() {
        return isPublicTransportVehicle() ? 1 : 2;
    }

    public boolean requiresImmediateNotification() {
        return isPublicTransportVehicle();
    }

    public long getEstimatedSetupTimeMinutes() {
        return switch (vehicleType) {
            case BUS, TROLLEY -> 30;
            case TRAM -> 45;
            case MINIBUS -> 15;
        };
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
        allMetadata.put("vehicleType", vehicleType.name());
        allMetadata.put("licensePlate", licensePlate);
        allMetadata.put("isPublicTransport", isPublicTransportVehicle());
        allMetadata.put("requiresGpsTracking", requiresGpsTracking());
        allMetadata.put("processingPriority", getProcessingPriority());
        allMetadata.put("estimatedSetupTimeMinutes", getEstimatedSetupTimeMinutes());
        allMetadata.put("createdBy", createdBy);

        if (capacity != null) {
            allMetadata.put("capacity", capacity);
        }
        if (model != null) {
            allMetadata.put("model", model);
        }
        if (yearManufactured != null) {
            allMetadata.put("yearManufactured", yearManufactured);
        }

        return Map.copyOf(allMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        VehicleCreatedEvent that = (VehicleCreatedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("VehicleCreatedEvent{eventId='%s', vehicleId=%s, licensePlate='%s', type=%s, by='%s', timestamp=%s}",
                eventId.substring(0, 8) + "...",
                vehicleId,
                licensePlate,
                vehicleType,
                createdBy,
                occurredAt
        );
    }
}