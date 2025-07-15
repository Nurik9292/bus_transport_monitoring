package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

@Getter
public final class Speed extends ValueObject {

    private static final double MIN_SPEED = 0.0;
    private static final double MAX_URBAN_SPEED_KMH = 60.0;
    private static final double MAX_HIGHWAY_SPEED_KMH = 120.0;
    private static final double MAX_VEHICLE_SPEED_KMH = 150.0;

    private static final double STATIONARY_THRESHOLD = 1.0;
    private static final double SLOW_SPEED_THRESHOLD = 10.0;
    private static final double NORMAL_SPEED_THRESHOLD = 40.0;
    private static final double FAST_SPEED_THRESHOLD = 80.0;

    private static final double MS_TO_KMH = 3.6;
    private static final double KMH_TO_MS = 1.0 / 3.6;

    private final double kmh;
    private Speed(double kmh) {
        this.kmh = kmh;
        validate();
    }

    public static Speed ofKmh(double kmh) {
        return new Speed(kmh);
    }

    public static Speed ofMs(double ms) {
        if (ms < 0) {
            throw new BusinessRuleViolationException(
                    "Speed in m/s cannot be negative",
                    "INVALID_SPEED_MS"
            );
        }

        return new Speed(ms * MS_TO_KMH);
    }

    public static Speed fromGpsApi(double gpsSpeedMs) {
        // GPS speed can sometimes be slightly negative due to GPS noise
        // Clamp to zero for very small negative values
        double adjustedSpeed = Math.max(0.0, gpsSpeedMs);
        return ofMs(adjustedSpeed);
    }

    public static Speed zero() {
        return new Speed(0.0);
    }

    public static Speed maxAllowedFor(VehicleType vehicleType) {
        double maxSpeed = switch (vehicleType) {
            case BUS, TROLLEY, TRAM -> 60.0;
            case MINIBUS -> 80.0;
        };

        return new Speed(maxSpeed);
    }

    @Override
    protected void validate() {
        if (kmh < MIN_SPEED) {
            throw new BusinessRuleViolationException(
                    String.format("Speed cannot be negative, got: %.2f km/h", kmh),
                    "INVALID_SPEED_NEGATIVE"
            );
        }

        if (kmh > MAX_VEHICLE_SPEED_KMH) {
            throw new BusinessRuleViolationException(
                    String.format("Speed exceeds maximum allowed limit of %.1f km/h, got: %.2f km/h",
                            MAX_VEHICLE_SPEED_KMH, kmh),
                    "INVALID_SPEED_TOO_HIGH"
            );
        }
    }

    public boolean isStationary() {
        return kmh <= STATIONARY_THRESHOLD;
    }

    public boolean isMoving() {
        return !isStationary();
    }

    public boolean isSlow() {
        return kmh >= STATIONARY_THRESHOLD && kmh < SLOW_SPEED_THRESHOLD;
    }

    public boolean isNormal() {
        return kmh >= SLOW_SPEED_THRESHOLD && kmh < NORMAL_SPEED_THRESHOLD;
    }

    public boolean isFast() {
        return kmh >= NORMAL_SPEED_THRESHOLD && kmh < FAST_SPEED_THRESHOLD;
    }

    public boolean isVeryFast() {
        return kmh >= FAST_SPEED_THRESHOLD;
    }

    public boolean exceedsUrbanLimit() {
        return kmh > MAX_URBAN_SPEED_KMH;
    }

    public boolean exceedsHighwayLimit() {
        return kmh > MAX_HIGHWAY_SPEED_KMH;
    }

    public boolean isAppropriateFor(VehicleType vehicleType) {
        Speed maxAllowed = maxAllowedFor(vehicleType);
        return kmh <= maxAllowed.kmh;
    }

    public double distanceInMeters(long durationSeconds) {
        if (durationSeconds < 0) {
            return 0.0;
        }

        double metersPerSecond = kmh * KMH_TO_MS;
        return metersPerSecond * durationSeconds;
    }

    public double timeToTravelMeters(double distanceMeters) {
        if (isStationary() || distanceMeters <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        double metersPerSecond = kmh * KMH_TO_MS;
        return distanceMeters / metersPerSecond;
    }

    public SpeedCategory getCategory() {
        if (isStationary()) {
            return SpeedCategory.STATIONARY;
        } else if (isSlow()) {
            return SpeedCategory.SLOW;
        } else if (isNormal()) {
            return SpeedCategory.NORMAL;
        } else if (isFast()) {
            return SpeedCategory.FAST;
        } else {
            return SpeedCategory.VERY_FAST;
        }
    }

    public boolean isGreaterThan(Speed other) {
        return this.kmh > other.kmh;
    }

    public boolean isLessThan(Speed other) {
        return this.kmh < other.kmh;
    }

    public Speed averageWith(Speed other) {
        return new Speed((this.kmh + other.kmh) / 2.0);
    }

    public Speed difference(Speed other) {
        return new Speed(Math.abs(this.kmh - other.kmh));
    }

    public double getMs() {
        return kmh * KMH_TO_MS;
    }

    public int getRoundedKmh() {
        return (int) Math.round(kmh);
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{kmh};
    }

    @Override
    public String toString() {
        return String.format("Speed{%.1f km/h}", kmh);
    }

    public String getDisplayFormat() {
        return String.format("%.0f km/h", kmh);
    }

    public String getGpsApiFormat() {
        return String.format("%.2f m/s", getMs());
    }

    public String getColorCode() {
        return getCategory().getColorCode();
    }

    @Getter
    public enum SpeedCategory {
        STATIONARY("Stationary", "gray"),
        SLOW("Slow", "blue"),
        NORMAL("Normal", "green"),
        FAST("Fast", "orange"),
        VERY_FAST("Very Fast", "red");

        private final String description;
        private final String colorCode;

        SpeedCategory(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

    }


}