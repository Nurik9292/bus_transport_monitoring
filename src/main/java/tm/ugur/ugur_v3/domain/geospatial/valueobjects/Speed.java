package tm.ugur.ugur_v3.domain.geospatial.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Getter
public final class Speed extends ValueObject {

    private static final double KMH_TO_MS = 1.0 / 3.6;
    private static final double MS_TO_KMH = 3.6;
    private static final double KMH_TO_MPH = 0.621371;
    private static final double MPH_TO_KMH = 1.609344;

    private static final double MAX_VEHICLE_SPEED_KMH = 200.0; // Emergency vehicles max
    private static final double MIN_SPEED_KMH = 0.0;

    private final double value;
    private final SpeedUnit unit;

    @Getter
    public enum SpeedUnit {
        KILOMETERS_PER_HOUR("km/h", "KMH"),
        METERS_PER_SECOND("m/s", "MS"),
        MILES_PER_HOUR("mph", "MPH");

        private final String displayName;
        private final String code;

        SpeedUnit(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }
    }

    private Speed(double value, SpeedUnit unit) {
        validateSpeed(value, unit);
        this.value = value;
        this.unit = unit;
    }

    public static Speed ofKmh(double kmh) {
        return new Speed(kmh, SpeedUnit.KILOMETERS_PER_HOUR);
    }

    public static Speed ofMs(double ms) {
        return new Speed(ms, SpeedUnit.METERS_PER_SECOND);
    }

    public static Speed ofMph(double mph) {
        return new Speed(mph, SpeedUnit.MILES_PER_HOUR);
    }

    public static Speed zero() {
        return new Speed(0.0, SpeedUnit.KILOMETERS_PER_HOUR);
    }

    public static Speed walkingSpeed() {
        return new Speed(5.0, SpeedUnit.KILOMETERS_PER_HOUR); // 5 km/h
    }

    public static Speed urbanBusSpeed() {
        return new Speed(25.0, SpeedUnit.KILOMETERS_PER_HOUR); // 25 km/h average
    }

    public static Speed highwayBusSpeed() {
        return new Speed(80.0, SpeedUnit.KILOMETERS_PER_HOUR); // 80 km/h highway
    }

    public double toKmh() {
        return switch (unit) {
            case KILOMETERS_PER_HOUR -> value;
            case METERS_PER_SECOND -> value * MS_TO_KMH;
            case MILES_PER_HOUR -> value * MPH_TO_KMH;
        };
    }

    public double toMs() {
        return switch (unit) {
            case KILOMETERS_PER_HOUR -> value * KMH_TO_MS;
            case METERS_PER_SECOND -> value;
            case MILES_PER_HOUR -> value * MPH_TO_KMH * KMH_TO_MS;
        };
    }

    public double toMph() {
        return switch (unit) {
            case KILOMETERS_PER_HOUR -> value * KMH_TO_MPH;
            case METERS_PER_SECOND -> value * MS_TO_KMH * KMH_TO_MPH;
            case MILES_PER_HOUR -> value;
        };
    }

    public boolean isStationary() {
        return toMs() < 0.1;
    }

    public boolean isWalkingSpeed() {
        double kmh = toKmh();
        return kmh >= 1.0 && kmh <= 7.0;
    }

    public boolean isVehicleSpeed() {
        double kmh = toKmh();
        return kmh >= 5.0 && kmh <= MAX_VEHICLE_SPEED_KMH;
    }

    public boolean isHighSpeed() {
        return toKmh() > 60.0;
    }

    public boolean isReasonableForRoute(double maxRouteSpeedKmh) {
        return toKmh() <= maxRouteSpeedKmh * 1.2;
    }

    // 🔥 MATHEMATICAL OPERATIONS
    public Speed add(Speed other) {
        double resultKmh = this.toKmh() + other.toKmh();
        return Speed.ofKmh(resultKmh);
    }

    public Speed subtract(Speed other) {
        double resultKmh = this.toKmh() - other.toKmh();
        return Speed.ofKmh(Math.max(0.0, resultKmh));
    }

    public Speed multiplyBy(double factor) {
        if (factor < 0) {
            throw new BusinessRuleViolationException("NEGATIVE_SPEED_FACTOR",
                    "Speed multiplication factor cannot be negative: " + factor);
        }
        return new Speed(value * factor, unit);
    }

    public Speed average(Speed other) {
        double avgKmh = (this.toKmh() + other.toKmh()) / 2.0;
        return Speed.ofKmh(avgKmh);
    }

    public boolean isGreaterThan(Speed other) {
        return this.toMs() > other.toMs();
    }

    public boolean isLessThan(Speed other) {
        return this.toMs() < other.toMs();
    }

    public boolean isEqualTo(Speed other) {
        return Math.abs(this.toMs() - other.toMs()) < 0.01; // 0.01 m/s tolerance
    }

    public Speed max(Speed other) {
        return this.isGreaterThan(other) ? this : other;
    }

    public Speed min(Speed other) {
        return this.isLessThan(other) ? this : other;
    }

    public String toDisplayString() {
        BigDecimal rounded = BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP);
        return rounded + " " + unit.getDisplayName();
    }

    public String toDisplayStringKmh() {
        BigDecimal rounded = BigDecimal.valueOf(toKmh())
                .setScale(1, RoundingMode.HALF_UP);
        return rounded + " km/h";
    }

    private void validateSpeed(double value, SpeedUnit unit) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new BusinessRuleViolationException("INVALID_SPEED_VALUE",
                    "Speed value must be a valid number: " + value);
        }

        if (value < MIN_SPEED_KMH) {
            throw new BusinessRuleViolationException("NEGATIVE_SPEED",
                    "Speed cannot be negative: " + value + " " + unit.getDisplayName());
        }

        double kmh = switch (unit) {
            case KILOMETERS_PER_HOUR -> value;
            case METERS_PER_SECOND -> value * MS_TO_KMH;
            case MILES_PER_HOUR -> value * MPH_TO_KMH;
        };

        if (kmh > MAX_VEHICLE_SPEED_KMH) {
            throw new BusinessRuleViolationException("SPEED_EXCEEDS_LIMIT",
                    String.format("Speed exceeds maximum limit: %.1f km/h > %.1f km/h",
                            kmh, MAX_VEHICLE_SPEED_KMH));
        }
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{
                BigDecimal.valueOf(toMs()).setScale(3, RoundingMode.HALF_UP),
                "MS"
        };
    }



    @Override
    public String toString() {
        return String.format("Speed{%.2f %s}", value, unit.getCode());
    }
}