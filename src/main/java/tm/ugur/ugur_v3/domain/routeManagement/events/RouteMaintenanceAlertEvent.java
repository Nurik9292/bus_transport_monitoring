package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class RouteMaintenanceAlertEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteId routeId;
    private final String maintenanceNotes;
    private final double currentOnTimePerformance;
    private final double currentAverageSpeed;
    private final MaintenanceUrgency urgency;
    private final Timestamp recommendedMaintenanceDate;
    private final String[] affectedSystems;
    private final Map<String, Object> metadata;

    private RouteMaintenanceAlertEvent(RouteId routeId, String maintenanceNotes,
                                       double currentOnTimePerformance, double currentAverageSpeed,
                                       MaintenanceUrgency urgency, Timestamp recommendedMaintenanceDate,
                                       String[] affectedSystems, String correlationId,
                                       Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RouteMaintenanceAlert";
        this.occurredAt = Timestamp.now();
        this.aggregateId = routeId.getValue();
        this.aggregateType = "Route";
        this.version = 1L;
        this.correlationId = correlationId;

        this.routeId = routeId;
        this.maintenanceNotes = maintenanceNotes;
        this.currentOnTimePerformance = currentOnTimePerformance;
        this.currentAverageSpeed = currentAverageSpeed;
        this.urgency = urgency != null ? urgency : determineUrgency(currentOnTimePerformance);
        this.recommendedMaintenanceDate = recommendedMaintenanceDate != null ?
                recommendedMaintenanceDate : calculateMaintenanceDate(this.urgency);
        this.affectedSystems = affectedSystems != null ? affectedSystems : new String[]{};
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RouteMaintenanceAlertEvent of(RouteId routeId, String maintenanceNotes,
                                                double currentOnTimePerformance, double currentAverageSpeed) {
        return new RouteMaintenanceAlertEvent(routeId, maintenanceNotes, currentOnTimePerformance,
                currentAverageSpeed, null, null, null, null, null);
    }

    public static RouteMaintenanceAlertEvent of(RouteId routeId, String maintenanceNotes,
                                                double currentOnTimePerformance, double currentAverageSpeed,
                                                MaintenanceUrgency urgency) {
        return new RouteMaintenanceAlertEvent(routeId, maintenanceNotes, currentOnTimePerformance,
                currentAverageSpeed, urgency, null, null, null, null);
    }

    private static MaintenanceUrgency determineUrgency(double onTimePerformance) {
        if (onTimePerformance < 50.0) return MaintenanceUrgency.CRITICAL;
        if (onTimePerformance < 70.0) return MaintenanceUrgency.HIGH;
        if (onTimePerformance < 85.0) return MaintenanceUrgency.MEDIUM;
        return MaintenanceUrgency.LOW;
    }

    private static Timestamp calculateMaintenanceDate(MaintenanceUrgency urgency) {
        long hoursFromNow = switch (urgency) {
            case CRITICAL -> 4;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 168;
        };
        return Timestamp.now().plusHours(hoursFromNow);
    }

    public enum MaintenanceUrgency {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}