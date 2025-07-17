package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.List;
import java.util.Map;

@Getter
public final class RouteMaintenanceAlertEvent extends BaseRouteEvent {

    private final String maintenanceNotes;
    private final double onTimePerformance;
    private final double averageSpeed;
    private final List<String> issues;
    private final AlertSeverity severity;
    private final boolean requiresImmediateAction;

    private RouteMaintenanceAlertEvent(RouteId routeId, String maintenanceNotes, double onTimePerformance,
                                       double averageSpeed, List<String> issues, AlertSeverity severity,
                                       String correlationId, Map<String, Object> metadata) {
        super("RouteMaintenanceAlert", routeId, correlationId, metadata);
        this.maintenanceNotes = maintenanceNotes;
        this.onTimePerformance = onTimePerformance;
        this.averageSpeed = averageSpeed;
        this.issues = issues != null ? List.copyOf(issues) : List.of();
        this.severity = severity;
        this.requiresImmediateAction = severity == AlertSeverity.CRITICAL || severity == AlertSeverity.HIGH;
    }

    public static RouteMaintenanceAlertEvent of(RouteId routeId, String maintenanceNotes,
                                                double onTimePerformance, double averageSpeed) {
        AlertSeverity severity = determineSeverity(onTimePerformance, averageSpeed);
        return new RouteMaintenanceAlertEvent(routeId, maintenanceNotes, onTimePerformance,
                averageSpeed, null, severity, null, null);
    }

    public static RouteMaintenanceAlertEvent of(RouteId routeId, String maintenanceNotes,
                                                double onTimePerformance, double averageSpeed,
                                                List<String> issues) {
        AlertSeverity severity = determineSeverity(onTimePerformance, averageSpeed);
        return new RouteMaintenanceAlertEvent(routeId, maintenanceNotes, onTimePerformance,
                averageSpeed, issues, severity, null, null);
    }

    private static AlertSeverity determineSeverity(double onTimePerformance, double averageSpeed) {
        if (onTimePerformance < 50.0 || averageSpeed < 10.0) return AlertSeverity.CRITICAL;
        if (onTimePerformance < 70.0 || averageSpeed < 15.0) return AlertSeverity.HIGH;
        if (onTimePerformance < 80.0 || averageSpeed < 20.0) return AlertSeverity.MEDIUM;
        return AlertSeverity.LOW;
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @Override
    public String toString() {
        return String.format("RouteMaintenanceAlertEvent{routeId=%s, severity=%s, onTime=%.1f%%, speed=%.1f}",
                routeId, severity, onTimePerformance, averageSpeed);
    }
}