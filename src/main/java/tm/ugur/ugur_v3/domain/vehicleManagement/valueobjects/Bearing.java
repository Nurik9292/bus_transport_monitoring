package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;

@Getter
public final class Bearing extends ValueObject {


    private static final double MIN_BEARING = 0.0;
    private static final double MAX_BEARING = 360.0;


    private static final double NORTH_MIN = 337.5;
    private static final double NORTH_MAX = 22.5;
    private static final double NORTHEAST_MIN = 22.5;
    private static final double NORTHEAST_MAX = 67.5;
    private static final double EAST_MIN = 67.5;
    private static final double EAST_MAX = 112.5;
    private static final double SOUTHEAST_MIN = 112.5;
    private static final double SOUTHEAST_MAX = 157.5;
    private static final double SOUTH_MIN = 157.5;
    private static final double SOUTH_MAX = 202.5;
    private static final double SOUTHWEST_MIN = 202.5;
    private static final double SOUTHWEST_MAX = 247.5;
    private static final double WEST_MIN = 247.5;
    private static final double WEST_MAX = 292.5;
    private static final double NORTHWEST_MIN = 292.5;
    private static final double NORTHWEST_MAX = 337.5;


    private static final double SLIGHT_TURN_THRESHOLD = 15.0;
    private static final double MODERATE_TURN_THRESHOLD = 45.0;
    private static final double SHARP_TURN_THRESHOLD = 90.0;
    private static final double U_TURN_THRESHOLD = 135.0;


    private final double degrees;

    private Bearing(double degrees) {
        this.degrees = normalizeBearing(degrees);
        validate();
    }

    public static Bearing ofDegrees(double degrees) {
        return new Bearing(degrees);
    }

    public static Bearing fromGpsApi(double gpsCourse) {
        return new Bearing(gpsCourse);
    }

    public static Bearing ofRadians(double radians) {
        double degrees = Math.toDegrees(radians);
        return new Bearing(degrees);
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

    private static double normalizeBearing(double degrees) {
        double normalized = degrees % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        return normalized;
    }

    @Override
    protected void validate() {


        if (degrees < MIN_BEARING || degrees >= MAX_BEARING) {
            throw new BusinessRuleViolationException(
                    String.format("Normalized bearing out of range: %.2f°", degrees),
                    "INVALID_BEARING_RANGE"
            );
        }
    }

    public CompassDirection getCompassDirection() {
        if (degrees >= NORTH_MIN || degrees < NORTH_MAX) {
            return CompassDirection.NORTH;
        } else if (degrees >= NORTHEAST_MIN && degrees < NORTHEAST_MAX) {
            return CompassDirection.NORTHEAST;
        } else if (degrees >= EAST_MIN && degrees < EAST_MAX) {
            return CompassDirection.EAST;
        } else if (degrees >= SOUTHEAST_MIN && degrees < SOUTHEAST_MAX) {
            return CompassDirection.SOUTHEAST;
        } else if (degrees >= SOUTH_MIN && degrees < SOUTH_MAX) {
            return CompassDirection.SOUTH;
        } else if (degrees >= SOUTHWEST_MIN && degrees < SOUTHWEST_MAX) {
            return CompassDirection.SOUTHWEST;
        } else if (degrees >= WEST_MIN && degrees < WEST_MAX) {
            return CompassDirection.WEST;
        } else {
            return CompassDirection.NORTHWEST;
        }
    }

    public double differenceTo(Bearing other) {
        double diff = Math.abs(this.degrees - other.degrees);
        return Math.min(diff, 360.0 - diff);
    }

    public double relativeBearingTo(Bearing other) {
        double diff = other.degrees - this.degrees;

        if (diff > 180.0) {
            diff -= 360.0;
        } else if (diff < -180.0) {
            diff += 360.0;
        }

        return diff;
    }

    public TurnType getTurnTypeTo(Bearing other) {
        double diff = Math.abs(relativeBearingTo(other));

        if (diff < SLIGHT_TURN_THRESHOLD) {
            return TurnType.STRAIGHT;
        } else if (diff < MODERATE_TURN_THRESHOLD) {
            return TurnType.SLIGHT_TURN;
        } else if (diff < SHARP_TURN_THRESHOLD) {
            return TurnType.MODERATE_TURN;
        } else if (diff < U_TURN_THRESHOLD) {
            return TurnType.SHARP_TURN;
        } else {
            return TurnType.U_TURN;
        }
    }

    public boolean isOppositeTo(Bearing other) {
        double diff = differenceTo(other);
        return Math.abs(diff - 180.0) < 10.0;
    }

    public boolean isSimilarTo(Bearing other, double toleranceDegrees) {
        return differenceTo(other) <= toleranceDegrees;
    }

    public Bearing perpendicular() {
        return new Bearing(degrees + 90.0);
    }

    public Bearing opposite() {
        return new Bearing(degrees + 180.0);
    }

    public Bearing plus(double additionalDegrees) {
        return new Bearing(degrees + additionalDegrees);
    }

    public Bearing minus(double subtractedDegrees) {
        return new Bearing(degrees - subtractedDegrees);
    }

    public boolean isNorthbound() {
        return degrees >= 315.0 || degrees <= 45.0;
    }

    public boolean isSouthbound() {
        return degrees >= 135.0 && degrees <= 225.0;
    }

    public boolean isEastbound() {
        return degrees >= 45.0 && degrees <= 135.0;
    }

    public boolean isWestbound() {
        return degrees >= 225.0 && degrees <= 315.0;
    }

    public boolean isConsistentWith(Bearing previous, double maxDeviation) {
        return differenceTo(previous) <= maxDeviation;
    }

    public boolean isValidVehicleBearing(Bearing previousBearing, double maxChangePerSecond, long timeDeltaSeconds) {
        if (previousBearing == null || timeDeltaSeconds <= 0) {
            return true;
        }

        double maxAllowedChange = maxChangePerSecond * timeDeltaSeconds;
        double actualChange = differenceTo(previousBearing);

        return actualChange <= maxAllowedChange;
    }

    public double getRadians() {
        return Math.toRadians(degrees);
    }

    public int getRoundedDegrees() {
        return (int) Math.round(degrees);
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{Math.round(degrees * 10.0) / 10.0};
    }

    @Override
    public String toString() {
        return String.format("Bearing{%.1f°}", degrees);
    }

    public String getDisplayFormat() {
        return String.format("%.0f° %s", degrees, getCompassDirection().getAbbreviation());
    }

    public String getGpsApiFormat() {
        return String.format("%.1f", degrees);
    }

    public String getDirectionSymbol() {
        return getCompassDirection().getSymbol();
    }

    @Getter
    public enum CompassDirection {
        NORTH("North", "N", "↑"),
        NORTHEAST("Northeast", "NE", "↗"),
        EAST("East", "E", "→"),
        SOUTHEAST("Southeast", "SE", "↘"),
        SOUTH("South", "S", "↓"),
        SOUTHWEST("Southwest", "SW", "↙"),
        WEST("West", "W", "←"),
        NORTHWEST("Northwest", "NW", "↖");

        private final String name;
        private final String abbreviation;
        private final String symbol;

        CompassDirection(String name, String abbreviation, String symbol) {
            this.name = name;
            this.abbreviation = abbreviation;
            this.symbol = symbol;
        }

    }

    @Getter
    public enum TurnType {
        STRAIGHT("Straight", "green"),
        SLIGHT_TURN("Slight Turn", "yellow"),
        MODERATE_TURN("Moderate Turn", "orange"),
        SHARP_TURN("Sharp Turn", "red"),
        U_TURN("U-Turn", "purple");

        private final String description;
        private final String colorCode;

        TurnType(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

    }
}