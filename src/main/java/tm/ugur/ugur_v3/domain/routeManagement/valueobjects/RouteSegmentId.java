package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

import java.util.UUID;

@Getter
public final class RouteSegmentId extends EntityId {

    private static final String PREFIX = "SEGMENT_";

    private RouteSegmentId(String value) {
        super(value);
    }

    public static RouteSegmentId generate() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return new RouteSegmentId(PREFIX + uuid);
    }

    public static RouteSegmentId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_ROUTE_SEGMENT_ID", "RouteSegmentId cannot be empty");
        }
        return new RouteSegmentId(value.trim().toUpperCase());
    }
}