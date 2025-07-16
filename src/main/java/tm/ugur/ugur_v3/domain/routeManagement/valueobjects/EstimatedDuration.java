package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

@Getter
public final class EstimatedDuration extends ValueObject {

    private final long seconds;

    private transient Double minutes;
    private transient Double hours;

    private EstimatedDuration(long seconds) {
        if (seconds < 0) {
            throw new BusinessRuleViolationException("ESTIMATE_DURATION_CANNOT_NEGATIVE", "Duration cannot be negative: " + seconds);
        }
        if (seconds > 86400 * 7) {
            throw new BusinessRuleViolationException("ESTIMATE_DURATION_TOO_LONG", "Duration too long: " + seconds + " seconds");
        }
        this.seconds = seconds;
    }

    public static EstimatedDuration ofSeconds(long seconds) {
        return new EstimatedDuration(seconds);
    }

    public static EstimatedDuration ofMinutes(double minutes) {
        return new EstimatedDuration((long) (minutes * 60));
    }

    public static EstimatedDuration ofHours(double hours) {
        return new EstimatedDuration((long) (hours * 3600));
    }

    public static EstimatedDuration zero() {
        return new EstimatedDuration(0);
    }

    public double getMinutes() {
        if (minutes == null) {
            minutes = seconds / 60.0;
        }
        return minutes;
    }

    public double getHours() {
        if (hours == null) {
            hours = seconds / 3600.0;
        }
        return hours;
    }

    public EstimatedDuration add(EstimatedDuration other) {
        return new EstimatedDuration(this.seconds + other.seconds);
    }

    public EstimatedDuration subtract(EstimatedDuration other) {
        if (this.seconds < other.seconds) {
            throw new BusinessRuleViolationException("ESTIMATE_DURATION_CANNOT_SUBTRACT", "Cannot subtract larger duration");
        }
        return new EstimatedDuration(this.seconds - other.seconds);
    }

    public EstimatedDuration multiply(double factor) {
        if (factor < 0) {
            throw new BusinessRuleViolationException("ESTIMATE_DURATION_MULTIPLY_NEGATIVE", "Cannot multiply by negative factor");
        }
        return new EstimatedDuration((long) (this.seconds * factor));
    }

    public EstimatedDuration divide(double divisor) {
        if (divisor <= 0) {
            throw new BusinessRuleViolationException("ESTIMATE_DURATION_DIVIDE_ZERO", "Cannot divide by zero or negative number");
        }
        return new EstimatedDuration((long) (this.seconds / divisor));
    }

    public boolean isGreaterThan(EstimatedDuration other) {
        return this.seconds > other.seconds;
    }

    public boolean isLessThan(EstimatedDuration other) {
        return this.seconds < other.seconds;
    }

    public boolean isZero() {
        return this.seconds == 0;
    }

    @Override
    protected void validate() {
        // Validation done in constructor
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{seconds};
    }

    @Override
    public String toString() {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return String.format("%.1f min", getMinutes());
        } else {
            return String.format("%.2f h", getHours());
        }
    }
}