package tm.ugur.ugur_v3.domain.geospatial.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public final class Bearing extends ValueObject {


    private static final double DEGREES_PER_CIRCLE = 360.0;
    private static final double RADIANS_PER_DEGREE = Math.PI / 180.0;
    private static final double DEGREES_PER_RADIAN = 180.0 / Math.PI;


    private static final double COMPASS_TOLERANCE = 11.25;

    private final double degrees;


    @Getter
    public enum CompassDirection {
        NORTH("N", 0.0, "North"),
        NORTHEAST("NE", 45.0, "Northeast"),
        EAST("E", 90.0, "East"),
        SOUTHEAST("SE", 135.0, "Southeast"),
        SOUTH("S", 180.0, "South"),
        SOUTHWEST("SW", 225.0, "Southwest"),
        WEST("W", 270.0, "West"),
        NORTHWEST("NW", 315.0, "Northwest");

        private final String code;
        private final double degrees;
        private final String displayName;

        CompassDirection(String code, double degrees, String displayName) {
            this.code = code;
            this.degrees = degrees;
            this.displayName = displayName;
        }
    }

    private Bearing(double degrees) {
        this.degrees = normalizeDegrees(degrees);
    }


    public static Bearing ofDegrees(double degrees) {
        return new Bearing(degrees);
    }

    public static Bearing ofRadians(double radians) {
        return new Bearing(radians * DEGREES_PER_RADIAN);
    }

    public static Bearing north() {
        return new Bearing(0.0);
    }

    public static Bearing east() {
        return new Bearing(90.0);
    }

    public static Bearing south() {
        return new Bearing(180.0);
    }

    public static Bearing west() {
        return new Bearing(270.0);
    }

    public static Bearing northeast() {
        return new Bearing(45.0);
    }

    public static Bearing southeast() {
        return new Bearing(135.0);
    }

    public static Bearing southwest() {
        return new Bearing(225.0);
    }

    public static Bearing northwest() {
        return new Bearing(315.0);
    }


    public CompassDirection getCompassDirection() {
        for (CompassDirection direction : CompassDirection.values()) {
            if (isWithinTolerance(direction.getDegrees(), COMPASS_TOLERANCE)) {
                return direction;
            }
        }


        if (isWithinTolerance(360.0, COMPASS_TOLERANCE)) {
            return CompassDirection.NORTH;
        }


        return findClosestCompassDirection();
    }

    public String getCompassCode() {
        return getCompassDirection().getCode();
    }

    public String getCompassDisplayName() {
        return getCompassDirection().getDisplayName();
    }


    public double toRadians() {
        return degrees * RADIANS_PER_DEGREE;
    }

    public Bearing add(double additionalDegrees) {
        return new Bearing(degrees + additionalDegrees);
    }

    public Bearing subtract(double degreesToSubtract) {
        return new Bearing(degrees - degreesToSubtract);
    }

    public Bearing reverse() {
        return new Bearing(degrees + 180.0);
    }


    public double angleTo(Bearing other) {
        double diff = other.degrees - this.degrees;
        return normalizeAngleDifference(diff);
    }

    public double absoluteAngleTo(Bearing other) {
        return Math.abs(angleTo(other));
    }

    public boolean isOpposite(Bearing other, double toleranceDegrees) {
        double diff = Math.abs(angleTo(other));
        return Math.abs(diff - 180.0) <= toleranceDegrees;
    }

    public boolean isSimilar(Bearing other, double toleranceDegrees) {
        return absoluteAngleTo(other) <= toleranceDegrees;
    }


    public boolean isNortherly() {
        return (degrees >= 315.0 && degrees <= 360.0) || (degrees >= 0.0 && degrees <= 45.0);
    }

    public boolean isSoutherly() {
        return degrees >= 135.0 && degrees <= 225.0;
    }

    public boolean isEasterly() {
        return degrees >= 45.0 && degrees <= 135.0;
    }

    public boolean isWesterly() {
        return degrees >= 225.0 && degrees <= 315.0;
    }

    public boolean isCardinalDirection() {
        return isWithinTolerance(0.0, 5.0) ||
                isWithinTolerance(90.0, 5.0) ||
                isWithinTolerance(180.0, 5.0) ||
                isWithinTolerance(270.0, 5.0);
    }


    public boolean isForwardDirection(Bearing routeDirection, double toleranceDegrees) {
        return isSimilar(routeDirection, toleranceDegrees);
    }

    public boolean isBackwardDirection(Bearing routeDirection, double toleranceDegrees) {
        return isSimilar(routeDirection.reverse(), toleranceDegrees);
    }

    public boolean isPerpendicular(Bearing other, double toleranceDegrees) {
        double diff = absoluteAngleTo(other);
        return Math.abs(diff - 90.0) <= toleranceDegrees;
    }


    public TurnDirection getTurnDirection(Bearing targetBearing) {
        double angleDiff = angleTo(targetBearing);

        if (Math.abs(angleDiff) <= 15.0) {
            return TurnDirection.STRAIGHT;
        } else if (angleDiff > 0 && angleDiff <= 180.0) {
            return angleDiff <= 90.0 ? TurnDirection.SLIGHT_RIGHT : TurnDirection.SHARP_RIGHT;
        } else {
            double absAngle = Math.abs(angleDiff);
            return absAngle <= 90.0 ? TurnDirection.SLIGHT_LEFT : TurnDirection.SHARP_LEFT;
        }
    }

    @Getter
    public enum TurnDirection {
        STRAIGHT("Straight", 0),
        SLIGHT_LEFT("Slight Left", -1),
        SHARP_LEFT("Sharp Left", -2),
        SLIGHT_RIGHT("Slight Right", 1),
        SHARP_RIGHT("Sharp Right", 2);

        private final String displayName;
        private final int turnValue;

        TurnDirection(String displayName, int turnValue) {
            this.displayName = displayName;
            this.turnValue = turnValue;
        }
    }


    public Bearing interpolate(Bearing target, double factor) {
        if (factor <= 0.0) return this;
        if (factor >= 1.0) return target;

        double angleDiff = angleTo(target);
        double interpolatedAngle = degrees + (angleDiff * factor);
        return new Bearing(interpolatedAngle);
    }


    public static Bearing average(Bearing... bearings) {
        if (bearings.length == 0) {
            throw new BusinessRuleViolationException("EMPTY_BEARING_LIST",
                    "Cannot calculate average of empty bearing list");
        }

        if (bearings.length == 1) {
            return bearings[0];
        }


        double sumSin = 0.0;
        double sumCos = 0.0;

        for (Bearing bearing : bearings) {
            double radians = bearing.toRadians();
            sumSin += Math.sin(radians);
            sumCos += Math.cos(radians);
        }

        double avgRadians = Math.atan2(sumSin / bearings.length, sumCos / bearings.length);
        return Bearing.ofRadians(avgRadians);
    }


    private boolean isWithinTolerance(double targetDegrees, double tolerance) {
        double normalizedTarget = normalizeDegrees(targetDegrees);
        double diff = Math.abs(degrees - normalizedTarget);


        if (diff > 180.0) {
            diff = 360.0 - diff;
        }

        return diff <= tolerance;
    }

    private CompassDirection findClosestCompassDirection() {
        CompassDirection closest = CompassDirection.NORTH;
        double minDiff = Double.MAX_VALUE;

        for (CompassDirection direction : CompassDirection.values()) {
            double diff = Math.abs(angleTo(Bearing.ofDegrees(direction.getDegrees())));
            if (diff < minDiff) {
                minDiff = diff;
                closest = direction;
            }
        }

        return closest;
    }

    private static double normalizeDegrees(double degrees) {
        if (Double.isNaN(degrees) || Double.isInfinite(degrees)) {
            throw new BusinessRuleViolationException("INVALID_BEARING_VALUE",
                    "Bearing must be a valid number: " + degrees);
        }


        double normalized = degrees % DEGREES_PER_CIRCLE;
        if (normalized < 0) {
            normalized += DEGREES_PER_CIRCLE;
        }

        return normalized;
    }

    private static double normalizeAngleDifference(double angleDiff) {

        while (angleDiff > 180.0) {
            angleDiff -= 360.0;
        }
        while (angleDiff < -180.0) {
            angleDiff += 360.0;
        }
        return angleDiff;
    }


    public String toDisplayString() {
        BigDecimal rounded = BigDecimal.valueOf(degrees)
                .setScale(1, RoundingMode.HALF_UP);
        return rounded + "°";
    }

    public String toDisplayStringWithCompass() {
        BigDecimal rounded = BigDecimal.valueOf(degrees)
                .setScale(1, RoundingMode.HALF_UP);
        return rounded + "° (" + getCompassCode() + ")";
    }

    public String toCompassString() {
        return getCompassCode() + " (" + getCompassDisplayName() + ")";
    }


    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{
                BigDecimal.valueOf(degrees).setScale(3, RoundingMode.HALF_UP)
        };
    }


    @Override
    public String toString() {
        return String.format("Bearing{%.1f°}", degrees);
    }
}