package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

import java.util.UUID;

public class VehicleId {

    private final String value;

    private VehicleId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("VehicleId cannot be null or empty");
        }
        this.value = value;
    }

    public static VehicleId of(String value) {
        return new VehicleId(value);
    }

    public static VehicleId of(Long value) {
        return new VehicleId(String.valueOf(value));
    }

    public static VehicleId generate() {
        return new VehicleId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return value;
    }
}
