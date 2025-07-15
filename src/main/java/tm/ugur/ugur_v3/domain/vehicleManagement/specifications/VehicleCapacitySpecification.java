package tm.ugur.ugur_v3.domain.vehicleManagement.specifications;

import tm.ugur.ugur_v3.domain.shared.specifications.Specification;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

public class VehicleCapacitySpecification implements Specification<Vehicle> {

    private final int minimumTotalCapacity;
    private final int minimumSeatedCapacity;
    private final boolean requiresAccessibility;
    private final VehicleType allowedVehicleType;

    public VehicleCapacitySpecification(int minimumTotalCapacity) {
        this(minimumTotalCapacity, 0, false, null);
    }

    public VehicleCapacitySpecification(int minimumTotalCapacity, int minimumSeatedCapacity,
                                        boolean requiresAccessibility, VehicleType allowedVehicleType) {
        this.minimumTotalCapacity = minimumTotalCapacity;
        this.minimumSeatedCapacity = minimumSeatedCapacity;
        this.requiresAccessibility = requiresAccessibility;
        this.allowedVehicleType = allowedVehicleType;
    }

    @Override
    public boolean isSatisfiedBy(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }

        // Check vehicle type if specified
        if (allowedVehicleType != null && vehicle.getVehicleType() != allowedVehicleType) {
            return false;
        }

        // Check total capacity
        if (vehicle.getCapacity().getTotalCapacity() < minimumTotalCapacity) {
            return false;
        }

        // Check seated capacity if specified
        if (minimumSeatedCapacity > 0 && vehicle.getCapacity().getSeatedCapacity() < minimumSeatedCapacity) {
            return false;
        }

        // Check accessibility requirements
        if (requiresAccessibility && !isAccessibilityCapable(vehicle)) {
            return false;
        }

        // Ensure vehicle is suitable for public transport if capacity is high
        if (minimumTotalCapacity >= 30 && !vehicle.getCapacity().isSuitableForPublicTransport()) {
            return false;
        }

        return true;
    }

    private boolean isAccessibilityCapable(Vehicle vehicle) {

        VehicleType type = vehicle.getVehicleType();
        return type == VehicleType.BUS ||
                type == VehicleType.TROLLEY ||
                type == VehicleType.TRAM;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Vehicle capacity >= ").append(minimumTotalCapacity);
        if (minimumSeatedCapacity > 0) {
            desc.append(", seated >= ").append(minimumSeatedCapacity);
        }
        if (requiresAccessibility) {
            desc.append(", with accessibility");
        }
        if (allowedVehicleType != null) {
            desc.append(", type: ").append(allowedVehicleType);
        }
        return desc.toString();
    }

    public static VehicleCapacitySpecification highCapacity() {
        return new VehicleCapacitySpecification(80, 40, false, null);
    }

    public static VehicleCapacitySpecification withAccessibility(int minimumCapacity) {
        return new VehicleCapacitySpecification(minimumCapacity, 0, true, null);
    }

    public static VehicleCapacitySpecification forVehicleType(VehicleType vehicleType, int minimumCapacity) {
        return new VehicleCapacitySpecification(minimumCapacity, 0, false, vehicleType);
    }
}