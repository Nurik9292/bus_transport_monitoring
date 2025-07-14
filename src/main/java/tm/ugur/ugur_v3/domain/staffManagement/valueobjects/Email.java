package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.util.regex.Pattern;

public final class Email extends ValueObject {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final int MAX_LENGTH = 254;

    private final String value;

    private Email(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidEmailException("Email cannot be null or empty");
        }
        this.value = value.trim().toLowerCase();
        validate();
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    protected void validate() {
        if (value.length() > MAX_LENGTH) {
            throw new InvalidEmailException("Email too long: " + value.length() + " characters");
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException("Invalid email format: " + value);
        }
    }

    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }

    public String getLocalPart() {
        return value.substring(0, value.indexOf('@'));
    }

    public boolean isCorporateEmail(String corporateDomain) {
        return getDomain().equals(corporateDomain);
    }

    public String getValue() {
        return value;
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{value};
    }

    @Override
    public String toString() {
        return value;
    }
}