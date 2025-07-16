package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteScheduleId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class SchedulePerformanceAlertEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteScheduleId scheduleId;
    private final RouteId routeId;
    private final double adherence;
    private final int missedTrips;
    private final String alertReason;
    private final Map<String, Object> metadata;

    private SchedulePerformanceAlertEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            double adherence,
            int missedTrips,
            String alertReason,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "SchedulePerformanceAlert";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.adherence = adherence;
        this.missedTrips = missedTrips;
        this.alertReason = alertReason;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static SchedulePerformanceAlertEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            double adherence,
            int missedTrips,
            String alertReason) {
        return new SchedulePerformanceAlertEvent(
                scheduleId,
                routeId,
                adherence,
                missedTrips,
                alertReason,
                null,
                null);
    }
}