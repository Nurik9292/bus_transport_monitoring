package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.List;
import java.util.Map;

@Getter
public final class RouteServiceInterruptedEvent extends BaseRouteEvent {

    private final InterruptionType interruptionType;
    private final String reason;
    private final Timestamp expectedResumption;
    private final List<String> affectedStops;
    private final boolean isComplete;
    private final String reportedBy;
    private final EmergencyLevel emergencyLevel;

    private RouteServiceInterruptedEvent(RouteId routeId, InterruptionType interruptionType, String reason,
                                         Timestamp expectedResumption, List<String> affectedStops,
                                         boolean isComplete, String reportedBy, EmergencyLevel emergencyLevel,
                                         String correlationId, Map<String, Object> metadata) {
        super("RouteServiceInterrupted", routeId, correlationId, metadata);
        this.interruptionType = interruptionType;
        this.reason = reason;
        this.expectedResumption = expectedResumption;
        this.affectedStops = affectedStops != null ? List.copyOf(affectedStops) : List.of();
        this.isComplete = isComplete;
        this.reportedBy = reportedBy;
        this.emergencyLevel = emergencyLevel;
    }

    public static RouteServiceInterruptedEvent complete(RouteId routeId, String reason,
                                                        Timestamp expectedResumption, String reportedBy) {
        return new RouteServiceInterruptedEvent(routeId, InterruptionType.COMPLETE_SUSPENSION, reason,
                expectedResumption, null, true, reportedBy,
                EmergencyLevel.HIGH, null, null);
    }

    public static RouteServiceInterruptedEvent partial(RouteId routeId, String reason,
                                                       List<String> affectedStops, String reportedBy) {
        return new RouteServiceInterruptedEvent(routeId, InterruptionType.PARTIAL_SUSPENSION, reason,
                null, affectedStops, false, reportedBy,
                EmergencyLevel.MEDIUM, null, null);
    }

    public static RouteServiceInterruptedEvent delay(RouteId routeId, String reason,
                                                     Timestamp expectedResumption, String reportedBy) {
        return new RouteServiceInterruptedEvent(routeId, InterruptionType.DELAY, reason,
                expectedResumption, null, false, reportedBy,
                EmergencyLevel.LOW, null, null);
    }

    public boolean requiresPassengerNotification() {
        return isComplete || emergencyLevel == EmergencyLevel.HIGH;
    }

    public boolean requiresAlternativeService() {
        return isComplete || affectedStops.size() > 5;
    }

    public enum InterruptionType {
        COMPLETE_SUSPENSION("Полная приостановка"),
        PARTIAL_SUSPENSION("Частичная приостановка"),
        DELAY("Задержка"),
        REROUTING("Изменение маршрута"),
        FREQUENCY_REDUCTION("Сокращение частоты");

        private final String displayName;

        InterruptionType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    public enum EmergencyLevel {
        LOW("Низкий"), MEDIUM("Средний"), HIGH("Высокий"), CRITICAL("Критический");

        private final String displayName;

        EmergencyLevel(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Override
    public String toString() {
        return String.format("RouteServiceInterruptedEvent{routeId=%s, type=%s, complete=%s, level=%s, reason='%s'}",
                routeId, interruptionType, isComplete, emergencyLevel, reason);
    }
}
