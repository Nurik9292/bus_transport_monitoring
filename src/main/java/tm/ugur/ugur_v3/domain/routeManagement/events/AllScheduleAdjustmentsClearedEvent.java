package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.List;
import java.util.Map;

@Getter
public final class AllScheduleAdjustmentsClearedEvent extends BaseRouteEvent {

    private final int totalAdjustmentsCleared;
    private final List<String> clearedAdjustmentIds;
    private final String clearanceReason;
    private final String clearedBy;
    private final ClearanceType clearanceType;
    private final boolean hadActiveAdjustments;
    private final ClearanceScope scope;

    private AllScheduleAdjustmentsClearedEvent(RouteId routeId, int totalAdjustmentsCleared,
                                               List<String> clearedAdjustmentIds, String clearanceReason,
                                               String clearedBy, ClearanceType clearanceType, ClearanceScope scope,
                                               String correlationId, Map<String, Object> metadata) {
        super("AllScheduleAdjustmentsCleared", routeId, correlationId, metadata);
        this.totalAdjustmentsCleared = totalAdjustmentsCleared;
        this.clearedAdjustmentIds = List.copyOf(clearedAdjustmentIds);
        this.clearanceReason = clearanceReason;
        this.clearedBy = clearedBy;
        this.clearanceType = clearanceType;
        this.hadActiveAdjustments = totalAdjustmentsCleared > 0;
        this.scope = scope;
    }

    public static AllScheduleAdjustmentsClearedEvent manual(RouteId routeId, int totalAdjustmentsCleared,
                                                            List<String> clearedAdjustmentIds, String reason,
                                                            String clearedBy) {
        return new AllScheduleAdjustmentsClearedEvent(routeId, totalAdjustmentsCleared, clearedAdjustmentIds,
                reason, clearedBy, ClearanceType.MANUAL, ClearanceScope.ALL, null, null);
    }

    public static AllScheduleAdjustmentsClearedEvent automatic(RouteId routeId, int totalAdjustmentsCleared,
                                                               List<String> clearedAdjustmentIds, String reason) {
        return new AllScheduleAdjustmentsClearedEvent(routeId, totalAdjustmentsCleared, clearedAdjustmentIds,
                reason, "SYSTEM", ClearanceType.AUTOMATIC, ClearanceScope.ALL, null, null);
    }

    public static AllScheduleAdjustmentsClearedEvent scheduleReset(RouteId routeId, int totalAdjustmentsCleared,
                                                                   List<String> clearedAdjustmentIds) {
        return new AllScheduleAdjustmentsClearedEvent(routeId, totalAdjustmentsCleared, clearedAdjustmentIds,
                "Schedule reset to baseline", "SYSTEM", ClearanceType.RESET,
                ClearanceScope.ALL, null, null);
    }

    public static AllScheduleAdjustmentsClearedEvent maintenance(RouteId routeId, int totalAdjustmentsCleared,
                                                                 List<String> clearedAdjustmentIds) {
        return new AllScheduleAdjustmentsClearedEvent(routeId, totalAdjustmentsCleared, clearedAdjustmentIds,
                "Maintenance mode activated", "SYSTEM", ClearanceType.MAINTENANCE,
                ClearanceScope.ALL, null, null);
    }

    public boolean hadSignificantAdjustments() { return totalAdjustmentsCleared >= 5; }
    public boolean requiresPassengerNotification() { return hadSignificantAdjustments() && clearanceType != ClearanceType.AUTOMATIC; }

    public enum ClearanceType {
        MANUAL("Ручная очистка"),
        AUTOMATIC("Автоматическая очистка"),
        RESET("Сброс расписания"),
        MAINTENANCE("Техническое обслуживание"),
        EMERGENCY("Экстренная очистка");

        private final String displayName;

        ClearanceType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    public enum ClearanceScope {
        ALL("Все корректировки"),
        TODAY_ONLY("Только на сегодня"),
        FUTURE_ONLY("Только будущие"),
        EXPIRED_ONLY("Только истекшие");

        private final String displayName;

        ClearanceScope(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Override
    public String toString() {
        return String.format("AllScheduleAdjustmentsClearedEvent{routeId=%s, cleared=%d adjustments, type=%s, scope=%s, by='%s'}",
                routeId, totalAdjustmentsCleared, clearanceType, scope, clearedBy);
    }
}
