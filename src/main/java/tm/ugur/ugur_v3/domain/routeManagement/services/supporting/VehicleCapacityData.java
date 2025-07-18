package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.util.Map;

/**
 * Данные о вместимости транспортных средств
 * TODO: Реализовать в Infrastructure слое при интеграции с Vehicle Management
 */
public record VehicleCapacityData(
        Map<VehicleId, Integer> vehicleCapacities,
        Map<VehicleId, Integer> currentOccupancy,
        Map<VehicleType, Integer> averageCapacity,
        int totalFleetCapacity,
        double averageUtilization
) {

    public static VehicleCapacityData empty() {
        return new VehicleCapacityData(
                Map.of(),
                Map.of(),
                Map.of(
                        VehicleType.BUS, 60,
                        VehicleType.TROLLEY, 50
                ),
                0,
                0.0
        );
    }

    public boolean hasAvailableCapacity() {
        return averageUtilization < 0.9;
    }

    public int getAvailableCapacity() {
        int totalOccupancy = currentOccupancy.values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(0, totalFleetCapacity - totalOccupancy);
    }
}