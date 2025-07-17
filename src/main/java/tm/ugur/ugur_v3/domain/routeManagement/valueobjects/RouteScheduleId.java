package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

import java.util.UUID;

@Getter
public final class RouteScheduleId extends EntityId {

    private static final String PREFIX = "SCHEDULE_";

    private RouteScheduleId(String value) {
        super(value);
    }

    public static RouteScheduleId of(String value) {
        return new RouteScheduleId(value);
    }

    public static RouteScheduleId generate() {
        return new RouteScheduleId(PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }
}



