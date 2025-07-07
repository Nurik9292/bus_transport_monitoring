package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import java.util.UUID;

public class RouteId {

    private final String value;

    private RouteId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("RouteId cannot be null or empty");
        }
        this.value = value;
    }

    public static RouteId of(String value) {
        return new RouteId(value);
    }

    public static RouteId of(Long value) {
        return new RouteId(String.valueOf(value));
    }

    public static RouteId generate() {
        return new RouteId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return value;
    }
}
