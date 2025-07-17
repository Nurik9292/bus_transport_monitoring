package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.enums.ScheduleType;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteScheduleId;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class RouteScheduleCreatedEvent extends BaseRouteEvent {

    private final RouteScheduleId scheduleId;
    private final String scheduleName;
    private final ScheduleType scheduleType;
    private final Timestamp effectiveFrom;
    private final Timestamp effectiveTo;
    private final String createdBy;

    private RouteScheduleCreatedEvent(RouteScheduleId scheduleId, RouteId routeId, String scheduleName,
                                      ScheduleType scheduleType, Timestamp effectiveFrom, Timestamp effectiveTo,
                                      String createdBy, String correlationId, Map<String, Object> metadata) {
        super("RouteScheduleCreated", routeId, correlationId, metadata);
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.scheduleType = scheduleType;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdBy = createdBy;
    }

    public static RouteScheduleCreatedEvent of(RouteScheduleId scheduleId, RouteId routeId,
                                               String scheduleName, ScheduleType scheduleType,
                                               Timestamp effectiveFrom, Timestamp effectiveTo,
                                               String createdBy) {
        return new RouteScheduleCreatedEvent(scheduleId, routeId, scheduleName, scheduleType,
                effectiveFrom, effectiveTo, createdBy, null, null);
    }

    @Override
    public String toString() {
        return String.format("RouteScheduleCreatedEvent{scheduleId=%s, routeId=%s, name='%s', type=%s}",
                scheduleId, routeId, scheduleName, scheduleType);
    }
}