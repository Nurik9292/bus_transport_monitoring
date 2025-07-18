package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.Map;

/**
 * Данные о пересадках пассажиров
 * TODO: Реализовать в Infrastructure слое при интеграции с системами оплаты/счетчиков
 */

public record PassengerTransferData(
        Map<StopId, Map<RouteId, Integer>> transfersByStop,
        Map<RouteConnection, Integer> routeConnections,
        double averageTransferTime,
        double transferSatisfaction
) {

    public record RouteConnection(
            RouteId fromRoute,
            RouteId toRoute,
            StopId transferStop
    ) {}

    public static PassengerTransferData empty() {
        return new PassengerTransferData(
                Map.of(),
                Map.of(),
                5.0, // 5 minutes average transfer
                4.0  // good satisfaction
        );
    }

    public boolean hasHighTransferVolume() {
        return transfersByStop.values().stream()
                .flatMap(routeMap -> routeMap.values().stream())
                .mapToInt(Integer::intValue)
                .sum() > 1000;
    }

    public int getTotalTransfers() {
        return routeConnections.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public boolean needsTransferOptimization() {
        return averageTransferTime > 10.0 || transferSatisfaction < 3.5;
    }
}