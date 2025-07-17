package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

@Getter
public final class ScheduleDynamicallyAdjustedEvent extends BaseRouteEvent {

    private final DayOfWeek dayOfWeek;
    private final LocalTime originalTime;
    private final LocalTime adjustedTime;
    private final int adjustmentMinutes;
    private final String adjustmentReason;
    private final String adjustedBy;
    private final AdjustmentType adjustmentType;
    private final AdjustmentSeverity severity;
    private final boolean isAutomaticAdjustment;

    private ScheduleDynamicallyAdjustedEvent(RouteId routeId, DayOfWeek dayOfWeek, LocalTime originalTime,
                                             int adjustmentMinutes, String adjustmentReason, String adjustedBy,
                                             AdjustmentType adjustmentType, boolean isAutomaticAdjustment,
                                             String correlationId, Map<String, Object> metadata) {
        super("ScheduleDynamicallyAdjusted", routeId, correlationId, metadata);
        this.dayOfWeek = dayOfWeek;
        this.originalTime = originalTime;
        this.adjustmentMinutes = adjustmentMinutes;
        this.adjustedTime = originalTime.plusMinutes(adjustmentMinutes);
        this.adjustmentReason = adjustmentReason;
        this.adjustedBy = adjustedBy;
        this.adjustmentType = adjustmentType;
        this.isAutomaticAdjustment = isAutomaticAdjustment;
        this.severity = determineSeverity(adjustmentMinutes);
    }

    public static ScheduleDynamicallyAdjustedEvent delay(RouteId routeId, DayOfWeek dayOfWeek, LocalTime originalTime,
                                                         int delayMinutes, String reason, String adjustedBy) {
        return new ScheduleDynamicallyAdjustedEvent(routeId, dayOfWeek, originalTime, delayMinutes, reason,
                adjustedBy, AdjustmentType.DELAY, false, null, null);
    }

    public static ScheduleDynamicallyAdjustedEvent advance(RouteId routeId, DayOfWeek dayOfWeek, LocalTime originalTime,
                                                           int advanceMinutes, String reason, String adjustedBy) {
        return new ScheduleDynamicallyAdjustedEvent(routeId, dayOfWeek, originalTime, -advanceMinutes, reason,
                adjustedBy, AdjustmentType.ADVANCE, false, null, null);
    }

    public static ScheduleDynamicallyAdjustedEvent automatic(RouteId routeId, DayOfWeek dayOfWeek, LocalTime originalTime,
                                                             int adjustmentMinutes, String reason) {
        AdjustmentType type = adjustmentMinutes > 0 ? AdjustmentType.DELAY : AdjustmentType.ADVANCE;
        return new ScheduleDynamicallyAdjustedEvent(routeId, dayOfWeek, originalTime, adjustmentMinutes, reason,
                "SYSTEM", type, true, null, null);
    }

    private AdjustmentSeverity determineSeverity(int adjustmentMinutes) {
        int absMinutes = Math.abs(adjustmentMinutes);
        if (absMinutes >= 30) return AdjustmentSeverity.CRITICAL;
        if (absMinutes >= 15) return AdjustmentSeverity.HIGH;
        if (absMinutes >= 5) return AdjustmentSeverity.MEDIUM;
        return AdjustmentSeverity.LOW;
    }

    public boolean isDelay() { return adjustmentMinutes > 0; }
    public boolean isAdvance() { return adjustmentMinutes < 0; }
    public boolean isSignificantAdjustment() { return Math.abs(adjustmentMinutes) > 10; }
    public boolean requiresPassengerNotification() { return severity.ordinal() >= AdjustmentSeverity.MEDIUM.ordinal(); }

    public enum AdjustmentType {
        DELAY("Задержка"),
        ADVANCE("Опережение"),
        CANCELLATION("Отмена"),
        FREQUENCY_CHANGE("Изменение частоты");

        private final String displayName;

        AdjustmentType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    public enum AdjustmentSeverity {
        LOW("Незначительная"), MEDIUM("Средняя"), HIGH("Высокая"), CRITICAL("Критическая");

        private final String displayName;

        AdjustmentSeverity(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Override
    public String toString() {
        String direction = isDelay() ? "delayed" : "advanced";
        return String.format("ScheduleDynamicallyAdjustedEvent{routeId=%s, %s %s %s by %d min (%s): %s}",
                routeId, dayOfWeek, originalTime, direction, Math.abs(adjustmentMinutes), severity, adjustmentReason);
    }
}