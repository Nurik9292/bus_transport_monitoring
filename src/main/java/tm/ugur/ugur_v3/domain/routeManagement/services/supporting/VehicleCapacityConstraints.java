package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

import java.util.Map;

/**
 * Ограничения по вместимости транспортных средств
 * TODO: Реализовать в Infrastructure слое при интеграции с Vehicle Management
 */
public record VehicleCapacityConstraints(
        int minCapacity,
        int maxCapacity,
        int availableVehicles,
        Map<VehicleType, Integer> capacityByType,
        double utilizationTarget
) {

    public static VehicleCapacityConstraints standard() {
        return new VehicleCapacityConstraints(
                30,  // min capacity
                80,  // max capacity
                10,  // available vehicles
                Map.of(
                        VehicleType.BUS, 60,
                        VehicleType.TROLLEY, 50
                ),
                0.75 // 75% utilization target
        );
    }

    public boolean canAccommodate(int passengerCount) {
        return passengerCount <= maxCapacity;
    }

    public int getRecommendedCapacity() {
        return (int) (maxCapacity * utilizationTarget);
    }
}