package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.Map;


/**
 * Данные о пассажирах в реальном времени
 * TODO: Реализовать в Infrastructure слое при интеграции с системами счетчиков
 */
public record RealTimePassengerData(
        RouteId routeId,
        Map<StopId, Integer> waitingPassengers,
        Map<StopId, Integer> boardingPassengers,
        Map<StopId, Integer> alightingPassengers,
        int totalActivePassengers,
        double currentLoadFactor,
        Timestamp lastUpdated
) {

    public static RealTimePassengerData empty(RouteId routeId) {
        return new RealTimePassengerData(
                routeId,
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                0.0,
                Timestamp.now()
        );
    }

    public boolean isOvercrowded() {
        return currentLoadFactor > 1.0;
    }

    public boolean hasHighDemand() {
        return waitingPassengers.values().stream().mapToInt(Integer::intValue).sum() > 50;
    }

    public int getTotalWaitingPassengers() {
        return waitingPassengers.values().stream().mapToInt(Integer::intValue).sum();
    }
}