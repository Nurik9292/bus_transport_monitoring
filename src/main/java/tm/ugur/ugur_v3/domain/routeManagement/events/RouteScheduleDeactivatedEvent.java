package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteScheduleId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.Map;

@Getter
public final class RouteScheduleDeactivatedEvent extends BaseRouteEvent {

    private final RouteScheduleId scheduleId;
    private final String scheduleName;
    private final Timestamp deactivatedAt;
    private final String deactivatedBy;
    private final String deactivationReason;
    private final boolean isTemporary;
    private final Timestamp expectedReactivation;
    private final DeactivationType deactivationType;

    private RouteScheduleDeactivatedEvent(RouteId routeId, RouteScheduleId scheduleId, String scheduleName,
                                          String deactivatedBy, String deactivationReason, boolean isTemporary,
                                          Timestamp expectedReactivation, DeactivationType deactivationType,
                                          String correlationId, Map<String, Object> metadata) {
        super("RouteScheduleDeactivated", routeId, correlationId, metadata);
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.deactivatedAt = Timestamp.now();
        this.deactivatedBy = deactivatedBy;
        this.deactivationReason = deactivationReason;
        this.isTemporary = isTemporary;
        this.expectedReactivation = expectedReactivation;
        this.deactivationType = deactivationType;
    }

    public static RouteScheduleDeactivatedEvent permanent(RouteId routeId, RouteScheduleId scheduleId,
                                                          String scheduleName, String deactivatedBy, String reason) {
        return new RouteScheduleDeactivatedEvent(routeId, scheduleId, scheduleName, deactivatedBy, reason,
                false, null, DeactivationType.PERMANENT, null, null);
    }

    public static RouteScheduleDeactivatedEvent temporary(RouteId routeId, RouteScheduleId scheduleId,
                                                          String scheduleName, String deactivatedBy, String reason,
                                                          Timestamp expectedReactivation) {
        return new RouteScheduleDeactivatedEvent(routeId, scheduleId, scheduleName, deactivatedBy, reason,
                true, expectedReactivation, DeactivationType.TEMPORARY, null, null);
    }

    public static RouteScheduleDeactivatedEvent maintenance(RouteId routeId, RouteScheduleId scheduleId,
                                                            String scheduleName, String reason) {
        return new RouteScheduleDeactivatedEvent(routeId, scheduleId, scheduleName, "SYSTEM", reason,
                true, null, DeactivationType.MAINTENANCE, null, null);
    }

    public boolean requiresAlternativeService() {
        return deactivationType == DeactivationType.PERMANENT || deactivationType == DeactivationType.EMERGENCY;
    }

    public enum DeactivationType {
        TEMPORARY("Временная"),
        PERMANENT("Постоянная"),
        MAINTENANCE("Техобслуживание"),
        EMERGENCY("Экстренная"),
        SEASONAL("Сезонная");

        private final String displayName;

        DeactivationType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Override
    public String toString() {
        return String.format("RouteScheduleDeactivatedEvent{routeId=%s, scheduleId=%s, type=%s, temporary=%s, reason='%s'}",
                routeId, scheduleId, deactivationType, isTemporary, deactivationReason);
    }
}
