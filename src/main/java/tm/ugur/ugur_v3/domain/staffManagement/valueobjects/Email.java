package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import java.util.Objects;

public record Email(String value) {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";

    public Email(String value) {
        if (!isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        this.value = value.toLowerCase().trim();
    }

    public boolean isValid() {
        return isValidEmail(this.value);
    }

    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Email other)) return false;
        return Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
