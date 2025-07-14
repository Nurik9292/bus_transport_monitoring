package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.util.regex.Pattern;

public final class PlainPassword extends ValueObject {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    private static final Pattern NO_WHITESPACE_PATTERN = Pattern.compile("^\\S*$");

    private final String value;

    private PlainPassword(String value) {
        if (value == null) {
            throw new WeakPasswordException("Password cannot be null");
        }
        this.value = value;
        validate();
    }

    public static PlainPassword of(String value) {
        return new PlainPassword(value);
    }

    @Override
    protected void validate() {
        if (value.length() < MIN_LENGTH) {
            throw new WeakPasswordException("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (value.length() > MAX_LENGTH) {
            throw new WeakPasswordException("Password must not exceed " + MAX_LENGTH + " characters");
        }

        if (!UPPERCASE_PATTERN.matcher(value).matches()) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }

        if (!LOWERCASE_PATTERN.matcher(value).matches()) {
            throw new WeakPasswordException("Password must contain at least one lowercase letter");
        }

        if (!DIGIT_PATTERN.matcher(value).matches()) {
            throw new WeakPasswordException("Password must contain at least one digit");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(value).matches()) {
            throw new WeakPasswordException("Password must contain at least one special character");
        }

        if (!NO_WHITESPACE_PATTERN.matcher(value).matches()) {
            throw new WeakPasswordException("Password must not contain whitespace characters");
        }
    }

    public int getStrengthScore() {
        int score = 0;

        if (value.length() >= 12) score += 25;
        else if (value.length() >= 10) score += 15;
        else if (value.length() >= 8) score += 10;

        if (UPPERCASE_PATTERN.matcher(value).matches()) score += 15;
        if (LOWERCASE_PATTERN.matcher(value).matches()) score += 15;
        if (DIGIT_PATTERN.matcher(value).matches()) score += 15;
        if (SPECIAL_CHAR_PATTERN.matcher(value).matches()) score += 15;

        long uniqueChars = value.chars().distinct().count();
        if (uniqueChars >= value.length() * 0.8) score += 15;

        return Math.min(score, 100);
    }

    public HashedPassword hash() {
        return HashedPassword.fromPlainPassword(this);
    }

    public String getValue() {
        return value;
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{value.length(), getStrengthScore()};
    }

    @Override
    public String toString() {
        return "PlainPassword{length=" + value.length() + ", strength=" + getStrengthScore() + "}";
    }
}