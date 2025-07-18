package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;


import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.time.DayOfWeek;
import java.util.Map;

/**
 * Фактические данные о производительности
 * TODO: Реализовать в Infrastructure слое при интеграции с системами мониторинга
 */
public record ActualPerformanceData(
        RouteId routeId,
        double scheduleAdherence,
        double averageDelay,
        int completedTrips,
        int cancelledTrips,
        Map<DayOfWeek, Double> weeklyPerformance,
        double passengerSatisfaction,
        Timestamp dataTimestamp
) {

    public static ActualPerformanceData empty(RouteId routeId) {
        return new ActualPerformanceData(
                routeId,
                100.0, // perfect adherence
                0.0,   // no delays
                0,     // no completed trips
                0,     // no cancelled trips
                Map.of(),
                5.0,   // perfect satisfaction
                Timestamp.now()
        );
    }

    public boolean isPerformingWell() {
        return scheduleAdherence >= 85.0 &&
                averageDelay <= 5.0 &&
                passengerSatisfaction >= 4.0;
    }

    public double getCancellationRate() {
        int totalTrips = completedTrips + cancelledTrips;
        return totalTrips > 0 ? (double) cancelledTrips / totalTrips : 0.0;
    }

    public boolean needsImprovement() {
        return scheduleAdherence < 75.0 ||
                averageDelay > 10.0 ||
                getCancellationRate() > 0.05;
    }
}
