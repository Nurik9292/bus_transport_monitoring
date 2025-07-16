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
public final class SchedulePeriodAddedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;

    private final RouteScheduleId scheduleId;
    private final RouteId routeId;
    private final String periodName;
    private final String startTime;
    private final String endTime;
    private final int headwayMinutes;
    private final String addedBy;
    private final Map<String, Object> metadata;

    private SchedulePeriodAddedEvent(
            RouteScheduleId scheduleId,
            RouteId routeId,
            String periodName,
            String startTime,
            String endTime,
            int headwayMinutes,
            String addedBy,
            String correlationId,
            Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "SchedulePeriodAdded";
        this.occurredAt = Timestamp.now();
        this.aggregateId = scheduleId.getValue();
        this.aggregateType = "RouteSchedule";
        this.version = 1L;
        this.correlationId = correlationId;

        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.periodName = periodName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.headwayMinutes = headwayMinutes;
        this.addedBy = addedBy;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static SchedulePeriodAddedEvent of(
            RouteScheduleId scheduleId,
            RouteId routeId,
            Object period,
            String addedBy) {
        String periodName = "";
        String startTime = "";
        String endTime = "";
        int headwayMinutes = 0;

        if (period != null) {
            try {
                periodName = (String) period.getClass().getMethod("getPeriodName").invoke(period);
                Object start = period.getClass().getMethod("getStartTime").invoke(period);
                Object end = period.getClass().getMethod("getEndTime").invoke(period);
                headwayMinutes = (Integer) period.getClass().getMethod("getHeadwayMinutes").invoke(period);

                startTime = start != null ? start.toString() : "";
                endTime = end != null ? end.toString() : "";
            } catch (Exception e) {
                // Fallback to defaults
            }
        }

        return new SchedulePeriodAddedEvent(scheduleId, routeId, periodName, startTime, endTime,
                headwayMinutes, addedBy, null, null);
    }
}