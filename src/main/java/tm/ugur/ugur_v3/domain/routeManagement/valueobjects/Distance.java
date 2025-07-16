package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

@Getter
public final class Distance extends ValueObject {

    private final double meters;

    private transient Double kilometers;
    private transient Double miles;

    private Distance(double meters) {
        if (meters < 0) {
            throw new BusinessRuleViolationException("DISTANCE_NEGATIVE_METERS", "Distance cannot be negative: " + meters);
        }
        if (meters > 1_000_000) {
            throw new BusinessRuleViolationException("DISTANCE_TOO_LARGE_METERS", "Distance too large: " + meters + " meters");
        }
        this.meters = meters;
    }

    public static Distance ofMeters(double meters) {
        return new Distance(meters);
    }

    public static Distance ofKilometers(double kilometers) {
        return new Distance(kilometers * 1000.0);
    }

    public static Distance ofMiles(double miles) {
        return new Distance(miles * 1609.344);
    }

    public static Distance zero() {
        return new Distance(0.0);
    }

    public double getKilometers() {
        if (kilometers == null) {
            kilometers = meters / 1000.0;
        }
        return kilometers;
    }

    public double getMiles() {
        if (miles == null) {
            miles = meters / 1609.344;
        }
        return miles;
    }

    public Distance add(Distance other) {
        return new Distance(this.meters + other.meters);
    }

    public Distance subtract(Distance other) {
        if (this.meters < other.meters) {
            throw new BusinessRuleViolationException("DISTANCE_CANNOT_SUBTRACT", "Cannot subtract larger distance");
        }
        return new Distance(this.meters - other.meters);
    }

    public Distance multiply(double factor) {
        if (factor < 0) {
            throw new BusinessRuleViolationException("DISTANCE_MULTIPLY_NEGATIVE", "Cannot multiply by negative factor");
        }
        return new Distance(this.meters * factor);
    }

    public Distance divide(double divisor) {
        if (divisor <= 0) {
            throw new BusinessRuleViolationException("DISTANCE_DIVIDE_ZERO_NEGATIVE", "Cannot divide by zero or negative number");
        }
        return new Distance(this.meters / divisor);
    }

    public boolean isGreaterThan(Distance other) {
        return this.meters > other.meters;
    }

    public boolean isLessThan(Distance other) {
        return this.meters < other.meters;
    }

    public boolean isZero() {
        return this.meters == 0.0;
    }

    @Override
    protected void validate() {
        // Validation done in constructor
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{meters};
    }

    @Override
    public String toString() {
        if (meters < 1000) {
            return String.format("%.1f m", meters);
        } else {
            return String.format("%.2f km", getKilometers());
        }
    }
}