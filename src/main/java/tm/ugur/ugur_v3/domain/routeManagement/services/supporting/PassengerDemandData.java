package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;


/**
 * Данные о пассажиропотоке для оптимизации маршрутов
 * TODO: Реализовать в Infrastructure слое при интеграции с аналитикой
 */
public record PassengerDemandData(
        RouteId routeId,
        Map<StopId, Integer> stopDemand,
        Map<LocalTime, Integer> hourlyDemand,
        Map<DayOfWeek, Integer> weeklyPattern,
        double averageLoadFactor,
        int peakHourPassengers,
        int offPeakPassengers
) {

    public static PassengerDemandData empty(RouteId routeId) {
        return new PassengerDemandData(
                routeId,
                Map.of(),
                Map.of(),
                Map.of(),
                0.0,
                0,
                0
        );
    }

    public boolean hasHighDemand() {
        return averageLoadFactor > 0.8;
    }

    public int getTotalDailyPassengers() {
        return peakHourPassengers + offPeakPassengers;
    }
}