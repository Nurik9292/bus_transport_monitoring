package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public final class RouteScheduleId extends EntityId {

    private static final String PREFIX = "SCHEDULE_";

    private RouteScheduleId(String value) {
        super(value);
    }

    public static RouteScheduleId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULED_ID", "RouteScheduleId cannot be empty");
        }
        return new RouteScheduleId(value.trim().toUpperCase());
    }

    public static RouteScheduleId generate() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
        return new RouteScheduleId(PREFIX + timestamp + "_" + random);
    }
}



