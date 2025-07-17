package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.List;
import java.util.Map;

@Getter
public final class SchedulePerformanceAlertEvent extends BaseRouteEvent {

    private final AlertType alertType;
    private final AlertSeverity severity;
    private final List<String> alertMessages;
    private final double currentScheduleAdherence;
    private final int currentMissedTrips;
    private final double currentLoadFactor;
    private final Timestamp alertTriggeredAt;
    private final List<String> affectedMetrics;
    private final List<String> recommendedActions;
    private final boolean requiresImmediateAction;

    private SchedulePerformanceAlertEvent(RouteId routeId, AlertType alertType, AlertSeverity severity,
                                          List<String> alertMessages, double currentScheduleAdherence,
                                          int currentMissedTrips, double currentLoadFactor,
                                          List<String> affectedMetrics, List<String> recommendedActions,
                                          String correlationId, Map<String, Object> metadata) {
        super("SchedulePerformanceAlert", routeId, correlationId, metadata);
        this.alertType = alertType;
        this.severity = severity;
        this.alertMessages = List.copyOf(alertMessages);
        this.currentScheduleAdherence = currentScheduleAdherence;
        this.currentMissedTrips = currentMissedTrips;
        this.currentLoadFactor = currentLoadFactor;
        this.alertTriggeredAt = Timestamp.now();
        this.affectedMetrics = List.copyOf(affectedMetrics);
        this.recommendedActions = List.copyOf(recommendedActions);
        this.requiresImmediateAction = severity.ordinal() >= AlertSeverity.HIGH.ordinal();
    }

    public static SchedulePerformanceAlertEvent lowAdherence(RouteId routeId, double adherence, List<String> messages) {
        return new SchedulePerformanceAlertEvent(routeId, AlertType.LOW_ADHERENCE, determineSeverityFromAdherence(adherence),
                messages, adherence, 0, 0.0, List.of("schedule_adherence"),
                List.of("Investigate delays", "Review schedule feasibility"), null, null);
    }

    public static SchedulePerformanceAlertEvent excessiveMissedTrips(RouteId routeId, int missedTrips, List<String> messages) {
        AlertSeverity severity = missedTrips > 10 ? AlertSeverity.CRITICAL :
                missedTrips > 5 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;
        return new SchedulePerformanceAlertEvent(routeId, AlertType.EXCESSIVE_MISSED_TRIPS, severity,
                messages, 0.0, missedTrips, 0.0, List.of("missed_trips"),
                List.of("Add backup vehicles", "Investigate root causes"), null, null);
    }

    public static SchedulePerformanceAlertEvent overloadAlert(RouteId routeId, double loadFactor, List<String> messages) {
        AlertSeverity severity = loadFactor > 1.5 ? AlertSeverity.CRITICAL :
                loadFactor > 1.3 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;
        return new SchedulePerformanceAlertEvent(routeId, AlertType.CAPACITY_OVERLOAD, severity,
                messages, 0.0, 0, loadFactor, List.of("passenger_load_factor"),
                List.of("Increase frequency", "Deploy larger vehicles"), null, null);
    }

    public static SchedulePerformanceAlertEvent underutilizationAlert(RouteId routeId, double loadFactor, List<String> messages) {
        return new SchedulePerformanceAlertEvent(routeId, AlertType.UNDERUTILIZATION, AlertSeverity.LOW,
                messages, 0.0, 0, loadFactor, List.of("passenger_load_factor"),
                List.of("Reduce frequency", "Consider route optimization"), null, null);
    }

    public static SchedulePerformanceAlertEvent systemicIssue(RouteId routeId, List<String> messages,
                                                              List<String> affectedMetrics, List<String> actions) {
        return new SchedulePerformanceAlertEvent(routeId, AlertType.SYSTEMIC_ISSUE, AlertSeverity.HIGH,
                messages, 0.0, 0, 0.0, affectedMetrics, actions, null, null);
    }

    private static AlertSeverity determineSeverityFromAdherence(double adherence) {
        if (adherence < 50.0) return AlertSeverity.CRITICAL;
        if (adherence < 70.0) return AlertSeverity.HIGH;
        if (adherence < 80.0) return AlertSeverity.MEDIUM;
        return AlertSeverity.LOW;
    }

    public boolean isCapacityRelated() {
        return alertType == AlertType.CAPACITY_OVERLOAD || alertType == AlertType.UNDERUTILIZATION;
    }

    public boolean isScheduleRelated() {
        return alertType == AlertType.LOW_ADHERENCE || alertType == AlertType.EXCESSIVE_MISSED_TRIPS;
    }

    public int getExpectedResolutionHours() {
        return switch (severity) {
            case CRITICAL -> 1;
            case HIGH -> 4;
            case MEDIUM -> 12;
            case LOW -> 24;
        };
    }

    public enum AlertType {
        LOW_ADHERENCE("Низкая пунктуальность"),
        EXCESSIVE_MISSED_TRIPS("Чрезмерное количество пропущенных рейсов"),
        CAPACITY_OVERLOAD("Перегрузка пассажирами"),
        UNDERUTILIZATION("Недостаточная загрузка"),
        SYSTEMIC_ISSUE("Системная проблема"),
        PERFORMANCE_DEGRADATION("Деградация производительности");

        private final String displayName;

        AlertType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    public enum AlertSeverity {
        LOW("Низкая", "🟡"),
        MEDIUM("Средняя", "🟠"),
        HIGH("Высокая", "🔴"),
        CRITICAL("Критическая", "🚨");

        private final String displayName;
        private final String indicator;

        AlertSeverity(String displayName, String indicator) {
            this.displayName = displayName;
            this.indicator = indicator;
        }

        @Override
        public String toString() { return indicator + " " + displayName; }
    }

    @Override
    public String toString() {
        return String.format("SchedulePerformanceAlertEvent{routeId=%s, type=%s, severity=%s, messages=%d, immediate=%s}",
                routeId, alertType, severity, alertMessages.size(), requiresImmediateAction);
    }
}