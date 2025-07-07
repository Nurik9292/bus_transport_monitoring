package tm.ugur.ugur_v3.domain.stopManagement.valueobjects;

import java.util.UUID;

public class StopId {

    private final String value;

    private StopId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StopId cannot be null or empty");
        }
        this.value = value;
    }

    public static StopId of(String value) {
        return new StopId(value);
    }

    public static StopId of(Long value) {
        return new StopId(String.valueOf(value));
    }

    public static StopId generate() {
        return new StopId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return value;
    }
}
