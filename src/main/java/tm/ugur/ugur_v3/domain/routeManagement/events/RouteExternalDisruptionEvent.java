package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.List;
import java.util.Map;

@Getter
public final class RouteExternalDisruptionEvent extends BaseRouteEvent {

    private final DisruptionType disruptionType;
    private final String description;
    private final List<String> affectedSegments;
    private final Timestamp estimatedClearanceTime;
    private final boolean requiresRerouting;
    private final String alternativeRouteId;
    private final DisruptionSeverity severity;

    private RouteExternalDisruptionEvent(RouteId routeId, DisruptionType disruptionType, String description,
                                         List<String> affectedSegments, Timestamp estimatedClearanceTime,
                                         boolean requiresRerouting, String alternativeRouteId, DisruptionSeverity severity,
                                         String correlationId, Map<String, Object> metadata) {
        super("RouteExternalDisruption", routeId, correlationId, metadata);
        this.disruptionType = disruptionType;
        this.description = description;
        this.affectedSegments = affectedSegments != null ? List.copyOf(affectedSegments) : List.of();
        this.estimatedClearanceTime = estimatedClearanceTime;
        this.requiresRerouting = requiresRerouting;
        this.alternativeRouteId = alternativeRouteId;
        this.severity = severity;
    }

    public static RouteExternalDisruptionEvent roadWork(RouteId routeId, String description,
                                                        List<String> affectedSegments,
                                                        Timestamp estimatedClearanceTime) {
        return new RouteExternalDisruptionEvent(routeId, DisruptionType.ROAD_WORK, description,
                affectedSegments, estimatedClearanceTime, true, null,
                DisruptionSeverity.MEDIUM, null, null);
    }

    public static RouteExternalDisruptionEvent accident(RouteId routeId, String description,
                                                        List<String> affectedSegments) {
        return new RouteExternalDisruptionEvent(routeId, DisruptionType.ACCIDENT, description,
                affectedSegments, null, true, null,
                DisruptionSeverity.HIGH, null, null);
    }

    public static RouteExternalDisruptionEvent event(RouteId routeId, String description,
                                                     Timestamp estimatedClearanceTime) {
        return new RouteExternalDisruptionEvent(routeId, DisruptionType.PUBLIC_EVENT, description,
                null, estimatedClearanceTime, false, null,
                DisruptionSeverity.LOW, null, null);
    }

    public boolean isLongTerm() {
        return disruptionType == DisruptionType.ROAD_WORK || disruptionType == DisruptionType.INFRASTRUCTURE_FAILURE;
    }

    public enum DisruptionType {
        ROAD_WORK("Дорожные работы"),
        ACCIDENT("ДТП"),
        PUBLIC_EVENT("Публичное мероприятие"),
        INFRASTRUCTURE_FAILURE("Повреждение инфраструктуры"),
        EMERGENCY_SERVICES("Экстренные службы"),
        PROTEST("Митинг/демонстрация");

        private final String displayName;

        DisruptionType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    public enum DisruptionSeverity {
        LOW("Низкая"), MEDIUM("Средняя"), HIGH("Высокая"), CRITICAL("Критическая");

        private final String displayName;

        DisruptionSeverity(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Override
    public String toString() {
        return String.format("RouteExternalDisruptionEvent{routeId=%s, type=%s, severity=%s, rerouting=%s}",
                routeId, disruptionType, severity, requiresRerouting);
    }
}