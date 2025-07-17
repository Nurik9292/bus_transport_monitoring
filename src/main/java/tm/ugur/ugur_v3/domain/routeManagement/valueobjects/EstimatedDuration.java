package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;


import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.time.Duration;

@Getter
public final class EstimatedDuration extends ValueObject {

    private static final int MAX_DURATION_HOURS = 24;
    private static final int MIN_DURATION_SECONDS = 1;

    private final Duration duration;

    private EstimatedDuration(Duration duration) {
        this.duration = duration;
    }

    public static EstimatedDuration ofSeconds(int seconds) {
        validateSeconds(seconds);
        return new EstimatedDuration(Duration.ofSeconds(seconds));
    }

    public static EstimatedDuration ofMinutes(int minutes) {
        validateMinutes(minutes);
        return new EstimatedDuration(Duration.ofMinutes(minutes));
    }

    public static EstimatedDuration ofHours(double hours) {
        validateHours(hours);
        return new EstimatedDuration(Duration.ofSeconds((long) (hours * 3600)));
    }

    public static EstimatedDuration zero() {
        return new EstimatedDuration(Duration.ZERO);
    }

    public int getSeconds() {
        return (int) duration.getSeconds();
    }

    public int getMinutes() {
        return (int) duration.toMinutes();
    }

    public double getHours() {
        return duration.getSeconds() / 3600.0;
    }

    public EstimatedDuration add(EstimatedDuration other) {
        return new EstimatedDuration(this.duration.plus(other.duration));
    }

    public EstimatedDuration subtract(EstimatedDuration other) {
        Duration result = this.duration.minus(other.duration);
        if (result.isNegative()) {
            throw new BusinessRuleViolationException("NEGATIVE_DURATION", "Duration cannot be negative");
        }
        return new EstimatedDuration(result);
    }

    public EstimatedDuration multiply(double factor) {
        if (factor < 0) {
            throw new BusinessRuleViolationException("NEGATIVE_FACTOR", "Factor cannot be negative");
        }
        return new EstimatedDuration(duration.multipliedBy((long) (factor * 1000)).dividedBy(1000));
    }

    public boolean isLongerThan(EstimatedDuration other) {
        return this.duration.compareTo(other.duration) > 0;
    }

    public boolean isShorterThan(EstimatedDuration other) {
        return this.duration.compareTo(other.duration) < 0;
    }

    private static void validateSeconds(int seconds) {
        if (seconds < MIN_DURATION_SECONDS) {
            throw new BusinessRuleViolationException("DURATION_TOO_SHORT",
                    "Duration must be at least " + MIN_DURATION_SECONDS + " second");
        }
        if (seconds > MAX_DURATION_HOURS * 3600) {
            throw new BusinessRuleViolationException("DURATION_TOO_LONG",
                    "Duration cannot exceed " + MAX_DURATION_HOURS + " hours");
        }
    }

    private static void validateMinutes(int minutes) {
        validateSeconds(minutes * 60);
    }

    private static void validateHours(double hours) {
        validateSeconds((int) (hours * 3600));
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{duration};
    }

    @Override
    public String toString() {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}