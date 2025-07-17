package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

@Getter
public final class ScheduleAdjustmentRemovedEvent extends BaseRouteEvent {

    private final DayOfWeek dayOfWeek;
    private final LocalTime originalTime;
    private final int removedAdjustmentMinutes;
    private final String adjustmentId;
    private final String removalReason;
    private final String removedBy;
    private final boolean wasAutomaticAdjustment;
    private final RemovalType removalType;

    private ScheduleAdjustmentRemovedEvent(RouteId routeId, DayOfWeek dayOfWeek, LocalTime originalTime,
                                           int removedAdjustmentMinutes, String adjustmentId, String removalReason,
                                           String removedBy, boolean wasAutomaticAdjustment, RemovalType removalType,
                                           String correlationId, Map<String, Object> metadata) {
        super("ScheduleAdjustmentRemoved", routeId, correlationId, metadata);
        this.dayOfWeek = dayOfWeek;
        this.originalTime = originalTime;
        this.removedAdjustmentMinutes = removedAdjustmentMinutes;
        this.adjustmentId = adjustmentId;
        this.removalReason = removalReason;
        this.removedBy = removedBy;
        this.wasAutomaticAdjustment = wasAutomaticAdjustment;
        this.removalType = removalType;
    }

    public static ScheduleAdjustmentRemovedEvent manual(RouteId routeId, DayOfWeek dayOfWeek, LocalTime originalTime,
                                                        int removedAdjustmentMinutes, String adjustmentId,
                                                        String reason, String removedBy) {
        return new ScheduleAdjustmentRemovedEvent(routeId, dayOfWeek, originalTime, removedAdjustmentMinutes,
                adjustmentId, reason, removedBy, false, RemovalType.MANUAL, null, null);
    }

    public static ScheduleAdjustmentRemovedEvent automatic(RouteId routeId, DayOfWeek dayOfWeek, LocalTime originalTime,
                                                           int removedAdjustmentMinutes, String adjustmentId, String reason) {
        return new ScheduleAdjustmentRemovedEvent(routeId, dayOfWeek, originalTime, removedAdjustmentMinutes,
                adjustmentId, reason, "SYSTEM", true, RemovalType.AUTOMATIC, null, null);
    }

    public static ScheduleAdjustmentRemovedEvent expired(RouteId routeId, DayOfWeek dayOfWeek, LocalTime originalTime,
                                                         int removedAdjustmentMinutes, String adjustmentId) {
        return new ScheduleAdjustmentRemovedEvent(routeId, dayOfWeek, originalTime, removedAdjustmentMinutes,
                adjustmentId, "Adjustment expired", "SYSTEM", true,
                RemovalType.EXPIRED, null, null);
    }

    public boolean wasDelayAdjustment() { return removedAdjustmentMinutes > 0; }
    public boolean wasAdvanceAdjustment() { return removedAdjustmentMinutes < 0; }
    public boolean wasSignificantAdjustment() { return Math.abs(removedAdjustmentMinutes) > 10; }

    public enum RemovalType {
        MANUAL("Ручное удаление"),
        AUTOMATIC("Автоматическое удаление"),
        EXPIRED("Истек срок действия"),
        SUPERSEDED("Заменено новой корректировкой"),
        CANCELLED("Отменено");

        private final String displayName;

        RemovalType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Override
    public String toString() {
        String adjustmentType = wasDelayAdjustment() ? "delay" : "advance";
        return String.format("ScheduleAdjustmentRemovedEvent{routeId=%s, %s %s %s adjustment (%d min), type=%s, by='%s'}",
                routeId, dayOfWeek, originalTime, adjustmentType, Math.abs(removedAdjustmentMinutes),
                removalType, removedBy);
    }
}