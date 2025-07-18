package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

/**
 * Погодные условия для адаптации расписания
 * TODO: Реализовать в Infrastructure слое при интеграции с Weather API
 */
public record WeatherConditions(
        GeoCoordinate location,
        WeatherType weatherType,
        double temperature,
        double precipitation,
        double windSpeed,
        double visibility,
        WeatherSeverity severity,
        Timestamp forecastTime
) {

    public enum WeatherType {
        CLEAR, CLOUDY, RAIN, SNOW, FOG, STORM, EXTREME
    }

    @Getter
    public enum WeatherSeverity {
        NORMAL(1.0),
        MINOR(1.1),
        MODERATE(1.2),
        SEVERE(1.4),
        EXTREME(1.8);

        private final double impactFactor;

        WeatherSeverity(double impactFactor) {
            this.impactFactor = impactFactor;
        }

    }

    public static WeatherConditions normal(GeoCoordinate location) {
        return new WeatherConditions(
                location,
                WeatherType.CLEAR,
                20.0, // temperature
                0.0,  // no precipitation
                5.0,  // light wind
                10.0, // good visibility
                WeatherSeverity.NORMAL,
                Timestamp.now()
        );
    }

    public boolean requiresScheduleAdjustment() {
        return severity.ordinal() >= WeatherSeverity.MODERATE.ordinal();
    }

    public boolean isDangerous() {
        return severity == WeatherSeverity.EXTREME;
    }

    public double getDelayFactor() {
        return severity.getImpactFactor();
    }
}