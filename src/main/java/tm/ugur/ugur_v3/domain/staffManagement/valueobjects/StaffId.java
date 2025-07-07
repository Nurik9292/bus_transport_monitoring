package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import java.util.Objects;
import java.util.UUID;

public final class StaffId {

    private final String value;

    private StaffId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StaffId cannot be null or empty");
        }
        this.value = value;
    }

    public static StaffId of(String value) {
        return new StaffId(value);
    }

    public static StaffId of(Long value) {
        return new StaffId(String.valueOf(value));
    }

    public static StaffId generate() {
        return new StaffId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    public boolean isSuperAdmin() {
        return "super-admin-id".equals(this.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StaffId other)) return false;
        return Objects.equals(this.value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "StaffId(" + value + ")";
    }
}
