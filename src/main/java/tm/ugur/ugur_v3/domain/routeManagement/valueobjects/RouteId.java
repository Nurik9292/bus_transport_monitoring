package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public final class RouteId extends EntityId {

    private static final String PREFIX = "ROUTE_";

    private RouteId(String value) {
        super(value);
    }

    public static RouteId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_ROUTE_ID", "RouteId cannot be empty");
        }
        return new RouteId(value.trim().toUpperCase());
    }

    public static RouteId generate() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
        return new RouteId(PREFIX + timestamp + "_" + random);
    }

    public static RouteId fromCode(String routeCode) {
        if (routeCode == null || routeCode.trim().isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_ROUTE_CODE", "Route code cannot be empty");
        }
        String code = routeCode.trim().toUpperCase();
        if (!code.matches("^[A-Z0-9_-]{3,20}$")) {
            throw new BusinessRuleViolationException("INVALID_ROUTE_CODE","Invalid route code format: " + code);
        }
        return new RouteId(PREFIX + code);
    }
}