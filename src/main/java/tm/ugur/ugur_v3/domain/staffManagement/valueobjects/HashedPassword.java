package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import java.util.Objects;

public final class HashedPassword {

    private final String hashedValue;

    private HashedPassword(String hashedValue) {
        this.hashedValue = Objects.requireNonNull(hashedValue, "Hashed password cannot be null");
    }

    public static HashedPassword fromPlain(PlainPassword plainPassword) {
        String hashed = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                plainPassword.value(),
                org.springframework.security.crypto.bcrypt.BCrypt.gensalt()
        );
        return new HashedPassword(hashed);
    }

    public static HashedPassword fromHash(String hashedValue) {
        return new HashedPassword(hashedValue);
    }

    public boolean matches(PlainPassword plainPassword) {
        return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(
                plainPassword.value(),
                this.hashedValue
        );
    }

    public String getHashedValue() {
        return hashedValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HashedPassword other)) return false;
        return Objects.equals(this.hashedValue, other.hashedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashedValue);
    }

    @Override
    public String toString() {
        return "[HASHED]";
    }
}