package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

import java.util.Map;
import java.util.UUID;

@Getter
public final class VehicleStatusChangedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;


    private final VehicleId vehicleId;
    private final VehicleStatus previousStatus;
    private final VehicleStatus newStatus;
    private final String reason;
    private final String changedBy;
    private final String assignedRouteId;
    private final boolean isAutomatic;


    private final Map<String, String> metadata;

    private VehicleStatusChangedEvent(VehicleId vehicleId,
                                      VehicleStatus previousStatus,
                                      VehicleStatus newStatus,
                                      String reason,
                                      String changedBy,
                                      String assignedRouteId,
                                      boolean isAutomatic,
                                      String correlationId,
                                      Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "VehicleStatusChanged";
        this.occurredAt = Timestamp.now();
        this.aggregateId = vehicleId.getValue();
        this.aggregateType = "Vehicle";
        this.version = 1L;
        this.correlationId = correlationId;

        this.vehicleId = vehicleId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
        this.assignedRouteId = assignedRouteId;
        this.isAutomatic = isAutomatic;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static VehicleStatusChangedEvent of(VehicleId vehicleId, VehicleStatus previousStatus,
                                               VehicleStatus newStatus, String reason, String changedBy) {
        return new VehicleStatusChangedEvent(
                vehicleId, previousStatus, newStatus, reason, changedBy,
                null, false, null, null
        );
    }

    public static VehicleStatusChangedEvent of(VehicleId vehicleId, VehicleStatus previousStatus,
                                               VehicleStatus newStatus, String reason, String changedBy,
                                               String assignedRouteId) {
        return new VehicleStatusChangedEvent(
                vehicleId, previousStatus, newStatus, reason, changedBy,
                assignedRouteId, false, null, null
        );
    }

    public static VehicleStatusChangedEvent automatic(VehicleId vehicleId, VehicleStatus previousStatus,
                                                      VehicleStatus newStatus, String reason) {
        return new VehicleStatusChangedEvent(
                vehicleId, previousStatus, newStatus, reason, "SYSTEM",
                null, true, null, null
        );
    }

    public static VehicleStatusChangedEvent of(VehicleId vehicleId, VehicleStatus previousStatus,
                                               VehicleStatus newStatus, String reason, String changedBy,
                                               String assignedRouteId, boolean isAutomatic,
                                               String correlationId, Map<String, String> metadata) {
        return new VehicleStatusChangedEvent(
                vehicleId, previousStatus, newStatus, reason, changedBy,
                assignedRouteId, isAutomatic, correlationId, metadata
        );
    }



    public boolean isCriticalStatusChange() {
        return newStatus == VehicleStatus.BREAKDOWN ||
                newStatus == VehicleStatus.RETIRED ||
                (previousStatus == VehicleStatus.IN_ROUTE && newStatus != VehicleStatus.ACTIVE);
    }

    public boolean requiresImmediateNotification() {
        return newStatus == VehicleStatus.BREAKDOWN ||
                newStatus.requiresImmediateAttention() ||
                (previousStatus.isInService() && !newStatus.isInService());
    }

    public boolean affectsGpsTracking() {
        return previousStatus.isTrackable() != newStatus.isTrackable();
    }

    public boolean affectsRouteAvailability() {
        return previousStatus.isAvailableForAssignment() != newStatus.isAvailableForAssignment();
    }

    public boolean isPlannedStatusChange() {
        return newStatus == VehicleStatus.MAINTENANCE ||
                newStatus == VehicleStatus.RETIRED ||
                reason.toLowerCase().contains("scheduled");
    }

    public boolean isRecoveryStatusChange() {
        return (previousStatus == VehicleStatus.BREAKDOWN && newStatus == VehicleStatus.MAINTENANCE) ||
                (previousStatus == VehicleStatus.MAINTENANCE && newStatus.isInService());
    }

    public int getProcessingPriority() {
        if (newStatus == VehicleStatus.BREAKDOWN) {
            return 1;
        }
        if (isCriticalStatusChange()) {
            return 2;
        }
        if (affectsRouteAvailability()) {
            return 3;
        }
        return 4;
    }

    public String getSeverityLevel() {
        if (newStatus == VehicleStatus.BREAKDOWN) {
            return "CRITICAL";
        }
        if (isCriticalStatusChange()) {
            return "HIGH";
        }
        if (affectsRouteAvailability()) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public String getAuditDescription() {
        return String.format("Vehicle %s status changed from %s to %s. Reason: %s. Changed by: %s%s",
                vehicleId.getValue(),
                previousStatus.getDisplayName(),
                newStatus.getDisplayName(),
                reason,
                changedBy,
                isAutomatic ? " (automatic)" : ""
        );
    }

    public Map<String, Object> toFleetManagementPayload() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("vehicleId", vehicleId.getValue());
        payload.put("previousStatus", previousStatus.name());
        payload.put("newStatus", newStatus.name());
        payload.put("previousDisplayName", previousStatus.getDisplayName());
        payload.put("newDisplayName", newStatus.getDisplayName());
        payload.put("reason", reason);
        payload.put("changedBy", changedBy);
        payload.put("timestamp", occurredAt.getEpochMillis());
        payload.put("isAutomatic", isAutomatic);
        payload.put("isCritical", isCriticalStatusChange());
        payload.put("affectsTracking", affectsGpsTracking());
        payload.put("affectsAvailability", affectsRouteAvailability());
        payload.put("severity", getSeverityLevel());

        if (assignedRouteId != null) {
            payload.put("routeId", assignedRouteId);
        }

        return Map.copyOf(payload);
    }

    public Map<String, Object> toAuditPayload() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("vehicleId", vehicleId.getValue());
        payload.put("previousStatus", previousStatus.name());
        payload.put("newStatus", newStatus.name());
        payload.put("reason", reason);
        payload.put("changedBy", changedBy);
        payload.put("timestamp", occurredAt.toString());
        payload.put("isAutomatic", isAutomatic);
        payload.put("description", getAuditDescription());

        if (correlationId != null) {
            payload.put("correlationId", correlationId);
        }

        if (assignedRouteId != null) {
            payload.put("routeId", assignedRouteId);
        }


        payload.putAll(metadata);

        return Map.copyOf(payload);
    }

    public Map<String, Object> toNotificationPayload() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "vehicle_status_changed");
        payload.put("vehicleId", vehicleId.getValue());
        payload.put("status", newStatus.name());
        payload.put("statusDisplay", newStatus.getDisplayName());
        payload.put("reason", reason);
        payload.put("timestamp", occurredAt.getEpochMillis());
        payload.put("severity", getSeverityLevel());
        payload.put("requiresAttention", requiresImmediateNotification());

        if (assignedRouteId != null) {
            payload.put("routeId", assignedRouteId);
        }

        return Map.copyOf(payload);
    }

    public String toLogString() {
        return String.format("VehicleStatus[%s]: %s→%s by %s (%s)%s%s",
                vehicleId.getValue(),
                previousStatus.name(),
                newStatus.name(),
                changedBy,
                reason,
                isAutomatic ? " AUTO" : "",
                assignedRouteId != null ? " route=" + assignedRouteId : ""
        );
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
        allMetadata.put("previousStatus", previousStatus.name());
        allMetadata.put("newStatus", newStatus.name());
        allMetadata.put("changedBy", changedBy);
        allMetadata.put("isAutomatic", isAutomatic);
        allMetadata.put("isCritical", isCriticalStatusChange());
        allMetadata.put("severity", getSeverityLevel());
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

        VehicleStatusChangedEvent that = (VehicleStatusChangedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("VehicleStatusChangedEvent{eventId='%s', vehicleId=%s, %s→%s, reason='%s', by='%s', timestamp=%s}",
                eventId.substring(0, 8) + "...",
                vehicleId,
                previousStatus,
                newStatus,
                reason,
                changedBy,
                occurredAt
        );
    }
}