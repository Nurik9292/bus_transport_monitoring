package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import java.util.Objects;

public record PlainPassword(String value) {

    public PlainPassword {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (!PasswordPolicy.isValid(value)) {
            throw new IllegalArgumentException("Password does not meet policy requirements");
        }
    }

    public HashedPassword hash() {
        return HashedPassword.fromPlain(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlainPassword(String other))) return false;
        return Objects.equals(this.value, other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
