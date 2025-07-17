package tm.ugur.ugur_v3.domain.geospatial.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public final class Distance extends ValueObject {


    private static final double METERS_PER_KILOMETER = 1000.0;
    private static final double METERS_PER_MILE = 1609.344;
    private static final double METERS_PER_FOOT = 0.3048;
    private static final double METERS_PER_NAUTICAL_MILE = 1852.0;


    private static final double MAX_ROUTE_DISTANCE_METERS = 500_000.0;
    private static final double MIN_DISTANCE_METERS = 0.0;

    private final double meters;
    private final DistanceUnit unit;

    @Getter
    public enum DistanceUnit {
        METERS("m", "Meters"),
        KILOMETERS("km", "Kilometers"),
        MILES("mi", "Miles"),
        FEET("ft", "Feet"),
        NAUTICAL_MILES("nmi", "Nautical Miles");

        private final String symbol;
        private final String displayName;

        DistanceUnit(String symbol, String displayName) {
            this.symbol = symbol;
            this.displayName = displayName;
        }
    }

    private Distance(double value, DistanceUnit unit) {
        this.unit = unit;
        this.meters = convertToMeters(value, unit);
        validateDistance(this.meters);
    }


    public static Distance ofMeters(double meters) {
        return new Distance(meters, DistanceUnit.METERS);
    }

    public static Distance ofKilometers(double kilometers) {
        return new Distance(kilometers, DistanceUnit.KILOMETERS);
    }

    public static Distance ofMiles(double miles) {
        return new Distance(miles, DistanceUnit.MILES);
    }

    public static Distance ofFeet(double feet) {
        return new Distance(feet, DistanceUnit.FEET);
    }

    public static Distance ofNauticalMiles(double nauticalMiles) {
        return new Distance(nauticalMiles, DistanceUnit.NAUTICAL_MILES);
    }


    public static Distance zero() {
        return new Distance(0.0, DistanceUnit.METERS);
    }

    public static Distance oneMeter() {
        return new Distance(1.0, DistanceUnit.METERS);
    }

    public static Distance oneKilometer() {
        return new Distance(1.0, DistanceUnit.KILOMETERS);
    }


    public static Distance stopProximity() {
        return new Distance(50.0, DistanceUnit.METERS);
    }

    public static Distance routeDeviation() {
        return new Distance(100.0, DistanceUnit.METERS);
    }

    public static Distance cityBlock() {
        return new Distance(100.0, DistanceUnit.METERS);
    }

    public static Distance shortRoute() {
        return new Distance(5.0, DistanceUnit.KILOMETERS);
    }

    public static Distance longRoute() {
        return new Distance(50.0, DistanceUnit.KILOMETERS);
    }


    public double toMeters() {
        return meters;
    }

    public double toKilometers() {
        return meters / METERS_PER_KILOMETER;
    }

    public double toMiles() {
        return meters / METERS_PER_MILE;
    }

    public double toFeet() {
        return meters / METERS_PER_FOOT;
    }

    public double toNauticalMiles() {
        return meters / METERS_PER_NAUTICAL_MILE;
    }

    public double getValue() {
        return convertFromMeters(meters, unit);
    }


    public boolean isWithinWalkingDistance() {
        return meters <= 800.0;
    }

    public boolean isShortDistance() {
        return meters <= 1000.0;
    }

    public boolean isMediumDistance() {
        return meters > 1000.0 && meters <= 10000.0;
    }

    public boolean isLongDistance() {
        return meters > 10000.0;
    }

    public boolean isNegligible() {
        return meters < 1.0;
    }

    public boolean isSignificant() {
        return meters >= 5.0;
    }

    public boolean isWithinTolerance(Distance other, Distance tolerance) {
        return Math.abs(this.meters - other.meters) <= tolerance.meters;
    }


    public Distance add(Distance other) {
        return Distance.ofMeters(this.meters + other.meters);
    }

    public Distance subtract(Distance other) {
        double result = this.meters - other.meters;
        return Distance.ofMeters(Math.max(0.0, result));
    }

    public Distance multiplyBy(double factor) {
        if (factor < 0) {
            throw new BusinessRuleViolationException("NEGATIVE_DISTANCE_FACTOR",
                    "Distance multiplication factor cannot be negative: " + factor);
        }
        return Distance.ofMeters(meters * factor);
    }

    public Distance divideBy(double divisor) {
        if (divisor <= 0) {
            throw new BusinessRuleViolationException("INVALID_DISTANCE_DIVISOR",
                    "Distance divisor must be positive: " + divisor);
        }
        return Distance.ofMeters(meters / divisor);
    }

    public Distance average(Distance other) {
        return Distance.ofMeters((this.meters + other.meters) / 2.0);
    }


    public boolean isGreaterThan(Distance other) {
        return this.meters > other.meters;
    }

    public boolean isLessThan(Distance other) {
        return this.meters < other.meters;
    }

    public boolean isEqualTo(Distance other) {
        return Math.abs(this.meters - other.meters) < 0.001;
    }

    public Distance max(Distance other) {
        return this.isGreaterThan(other) ? this : other;
    }

    public Distance min(Distance other) {
        return this.isLessThan(other) ? this : other;
    }

    public int compareTo(Distance other) {
        return Double.compare(this.meters, other.meters);
    }


    public java.time.Duration estimateTravelTime(Speed speed) {
        if (speed.isStationary()) {
            throw new BusinessRuleViolationException("ZERO_SPEED",
                    "Cannot calculate travel time with zero speed");
        }

        double timeInSeconds = meters / speed.toMs();
        return java.time.Duration.ofMillis((long)(timeInSeconds * 1000));
    }

    public java.time.Duration estimateWalkingTime() {
        Speed walkingSpeed = Speed.walkingSpeed();
        return estimateTravelTime(walkingSpeed);
    }

    public java.time.Duration estimateBusTime() {
        Speed busSpeed = Speed.urbanBusSpeed();
        return estimateTravelTime(busSpeed);
    }


    public static Distance sum(Distance... distances) {
        double totalMeters = 0.0;
        for (Distance distance : distances) {
            totalMeters += distance.meters;
        }
        return Distance.ofMeters(totalMeters);
    }

    public static Distance average(Distance... distances) {
        if (distances.length == 0) {
            throw new BusinessRuleViolationException("EMPTY_DISTANCE_LIST",
                    "Cannot calculate average of empty distance list");
        }

        double totalMeters = 0.0;
        for (Distance distance : distances) {
            totalMeters += distance.meters;
        }

        return Distance.ofMeters(totalMeters / distances.length);
    }

    public static Distance max(Distance... distances) {
        if (distances.length == 0) {
            throw new BusinessRuleViolationException("EMPTY_DISTANCE_LIST",
                    "Cannot find maximum of empty distance list");
        }

        Distance maximum = distances[0];
        for (int i = 1; i < distances.length; i++) {
            if (distances[i].isGreaterThan(maximum)) {
                maximum = distances[i];
            }
        }
        return maximum;
    }

    public static Distance min(Distance... distances) {
        if (distances.length == 0) {
            throw new BusinessRuleViolationException("EMPTY_DISTANCE_LIST",
                    "Cannot find minimum of empty distance list");
        }

        Distance minimum = distances[0];
        for (int i = 1; i < distances.length; i++) {
            if (distances[i].isLessThan(minimum)) {
                minimum = distances[i];
            }
        }
        return minimum;
    }


    public String toSmartDisplayString() {
        if (meters < 1.0) {
            return String.format("%.0f cm", meters * 100);
        } else if (meters < 1000.0) {
            return String.format("%.1f m", meters);
        } else if (meters < 10000.0) {
            return String.format("%.2f km", toKilometers());
        } else {
            return String.format("%.1f km", toKilometers());
        }
    }

    public String toDisplayString() {
        BigDecimal value = BigDecimal.valueOf(getValue())
                .setScale(getScaleForUnit(), RoundingMode.HALF_UP);
        return value + " " + unit.getSymbol();
    }

    public String toDisplayStringInUnit(DistanceUnit displayUnit) {
        double value = convertFromMeters(meters, displayUnit);
        BigDecimal rounded = BigDecimal.valueOf(value)
                .setScale(getScaleForUnit(displayUnit), RoundingMode.HALF_UP);
        return rounded + " " + displayUnit.getSymbol();
    }


    private double convertToMeters(double value, DistanceUnit unit) {
        return switch (unit) {
            case METERS -> value;
            case KILOMETERS -> value * METERS_PER_KILOMETER;
            case MILES -> value * METERS_PER_MILE;
            case FEET -> value * METERS_PER_FOOT;
            case NAUTICAL_MILES -> value * METERS_PER_NAUTICAL_MILE;
        };
    }

    private double convertFromMeters(double meters, DistanceUnit unit) {
        return switch (unit) {
            case METERS -> meters;
            case KILOMETERS -> meters / METERS_PER_KILOMETER;
            case MILES -> meters / METERS_PER_MILE;
            case FEET -> meters / METERS_PER_FOOT;
            case NAUTICAL_MILES -> meters / METERS_PER_NAUTICAL_MILE;
        };
    }

    private int getScaleForUnit() {
        return getScaleForUnit(this.unit);
    }

    private int getScaleForUnit(DistanceUnit unit) {
        return switch (unit) {
            case METERS -> 1;
            case KILOMETERS -> 3;
            case MILES -> 3;
            case FEET -> 0;
            case NAUTICAL_MILES -> 3;
        };
    }


    private void validateDistance(double meters) {
        if (Double.isNaN(meters) || Double.isInfinite(meters)) {
            throw new BusinessRuleViolationException("INVALID_DISTANCE_VALUE",
                    "Distance value must be a valid number: " + meters);
        }

        if (meters < MIN_DISTANCE_METERS) {
            throw new BusinessRuleViolationException("NEGATIVE_DISTANCE",
                    "Distance cannot be negative: " + meters + " meters");
        }

        if (meters > MAX_ROUTE_DISTANCE_METERS) {
            throw new BusinessRuleViolationException("DISTANCE_EXCEEDS_LIMIT",
                    String.format("Distance exceeds maximum limit: %.1f km > %.1f km",
                            meters / 1000.0, MAX_ROUTE_DISTANCE_METERS / 1000.0));
        }
    }


    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{
                BigDecimal.valueOf(meters).setScale(3, RoundingMode.HALF_UP)
        };
    }


    @Override
    public String toString() {
        return String.format("Distance{%.3f m}", meters);
    }
}