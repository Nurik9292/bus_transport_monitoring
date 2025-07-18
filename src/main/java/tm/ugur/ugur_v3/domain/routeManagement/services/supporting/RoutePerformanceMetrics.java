package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;


/**
 * Метрики производительности маршрута
 * TODO: Реализовать в Infrastructure слое при интеграции с мониторингом
 */
public record RoutePerformanceMetrics(
        RouteId routeId,
        double onTimePerformance,
        double averageDelay,
        int totalTrips,
        int completedTrips,
        double passengerSatisfaction,
        double fuelEfficiency,
        Timestamp lastUpdated
) {

    public static RoutePerformanceMetrics empty(RouteId routeId) {
        return new RoutePerformanceMetrics(
                routeId,
                100.0, // on-time performance
                0.0,   // average delay
                0,     // total trips
                0,     // completed trips
                5.0,   // passenger satisfaction (1-5 scale)
                0.0,   // fuel efficiency
                Timestamp.now()
        );
    }

    public boolean isPerformingWell() {
        return onTimePerformance >= 85.0 && passengerSatisfaction >= 4.0;
    }

    public double getCompletionRate() {
        return totalTrips > 0 ? (double) completedTrips / totalTrips : 0.0;
    }
}