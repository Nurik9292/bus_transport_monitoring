package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.List;

/**
 * Дорожные условия для адаптации расписания
 * TODO: Реализовать в Infrastructure слое при интеграции с Traffic API
 */
public record TrafficConditions(
        GeoCoordinate area,
        TrafficLevel trafficLevel,
        double averageSpeed,
        List<TrafficIncident> incidents,
        double congestionFactor,
        Timestamp lastUpdated
) {

    @Getter
    public enum TrafficLevel {
        FREE_FLOW(1.0),
        LIGHT(1.1),
        MODERATE(1.3),
        HEAVY(1.6),
        STANDSTILL(2.5);

        private final double delayFactor;

        TrafficLevel(double delayFactor) {
            this.delayFactor = delayFactor;
        }

    }

    public record TrafficIncident(
            String incidentId,
            IncidentType type,
            GeoCoordinate location,
            String description,
            IncidentSeverity severity,
            Timestamp startTime,
            Timestamp estimatedEndTime
    ) {
        public enum IncidentType {
            ACCIDENT, CONSTRUCTION, EVENT, BREAKDOWN, WEATHER_RELATED
        }

        public enum IncidentSeverity {
            MINOR, MODERATE, MAJOR, CRITICAL
        }

        public boolean isActive() {
            Timestamp now = Timestamp.now();
            return !now.isBefore(startTime) &&
                    (estimatedEndTime == null || !now.isAfter(estimatedEndTime));
        }
    }

    public static TrafficConditions normal(GeoCoordinate area) {
        return new TrafficConditions(
                area,
                TrafficLevel.FREE_FLOW,
                50.0, // km/h
                List.of(),
                1.0,
                Timestamp.now()
        );
    }

    public boolean requiresScheduleAdjustment() {
        return trafficLevel.ordinal() >= TrafficLevel.MODERATE.ordinal() || !incidents.isEmpty();
    }

    public boolean hasMajorIncidents() {
        return incidents.stream().anyMatch(incident ->
                incident.severity() == TrafficIncident.IncidentSeverity.MAJOR ||
                        incident.severity() == TrafficIncident.IncidentSeverity.CRITICAL);
    }

    public double getTotalDelayFactor() {
        double baseDelay = trafficLevel.getDelayFactor();
        double incidentDelay = incidents.stream()
                .filter(TrafficIncident::isActive)
                .mapToDouble(incident -> switch (incident.severity()) {
                    case MINOR -> 0.1;
                    case MODERATE -> 0.2;
                    case MAJOR -> 0.4;
                    case CRITICAL -> 0.8;
                })
                .sum();
        return baseDelay + incidentDelay;
    }
}