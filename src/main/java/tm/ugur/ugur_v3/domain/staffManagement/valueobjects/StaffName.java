package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import java.util.Objects;

public record StaffName(String value) {

    public StaffName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Staff name cannot be empty");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("Staff name cannot exceed 100 characters");
        }
        if (value.length() < 2) {
            throw new IllegalArgumentException("Staff name must be at least 2 characters");
        }
        this.value = value.trim();
    }

    public boolean isValid() {
        return value.length() >= 2 && value.length() <= 100;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StaffName(String other))) return false;
        return Objects.equals(this.value, other);
    }

    @Override
    public String toString() {
        return value;
    }
}
