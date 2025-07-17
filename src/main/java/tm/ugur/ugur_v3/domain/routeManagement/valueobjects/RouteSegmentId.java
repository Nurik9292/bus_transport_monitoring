package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

import java.util.UUID;

@Getter
public final class RouteSegmentId extends EntityId {

    private static final String PREFIX = "SEGMENT_";

    private RouteSegmentId(String value) {
        super(value);
    }

    public static RouteSegmentId of(String value) {
        return new RouteSegmentId(value);
    }

    public static RouteSegmentId generate() {
        return new RouteSegmentId(PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }
}