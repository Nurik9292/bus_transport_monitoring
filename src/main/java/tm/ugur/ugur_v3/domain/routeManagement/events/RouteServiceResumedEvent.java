package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.Map;

@Getter
public final class RouteServiceResumedEvent extends BaseRouteEvent {

    private final Timestamp interruptionStartTime;
    private final String resolutionDescription;
    private final boolean isFullyRestored;
    private final String resumedBy;
    private final int totalInterruptionMinutes;

    private RouteServiceResumedEvent(RouteId routeId, Timestamp interruptionStartTime,
                                     String resolutionDescription, boolean isFullyRestored,
                                     String resumedBy, String correlationId, Map<String, Object> metadata) {
        super("RouteServiceResumed", routeId, correlationId, metadata);
        this.interruptionStartTime = interruptionStartTime;
        this.resolutionDescription = resolutionDescription;
        this.isFullyRestored = isFullyRestored;
        this.resumedBy = resumedBy;
        this.totalInterruptionMinutes = calculateInterruptionDuration();
    }

    private int calculateInterruptionDuration() {
        if (interruptionStartTime == null) return 0;
        return (int) ((occurredAt.getEpochMillis() - interruptionStartTime.getEpochMillis()) / 60000);
    }

    public static RouteServiceResumedEvent of(RouteId routeId, Timestamp interruptionStartTime,
                                              String resolutionDescription, String resumedBy) {
        return new RouteServiceResumedEvent(routeId, interruptionStartTime, resolutionDescription,
                true, resumedBy, null, null);
    }

    public static RouteServiceResumedEvent partial(RouteId routeId, Timestamp interruptionStartTime,
                                                   String resolutionDescription, String resumedBy) {
        return new RouteServiceResumedEvent(routeId, interruptionStartTime, resolutionDescription,
                false, resumedBy, null, null);
    }

    public boolean wasLongInterruption() { return totalInterruptionMinutes > 60; }
    public boolean wasShortInterruption() { return totalInterruptionMinutes <= 15; }

    @Override
    public String toString() {
        return String.format("RouteServiceResumedEvent{routeId=%s, duration=%dmin, full=%s, by='%s'}",
                routeId, totalInterruptionMinutes, isFullyRestored, resumedBy);
    }
}