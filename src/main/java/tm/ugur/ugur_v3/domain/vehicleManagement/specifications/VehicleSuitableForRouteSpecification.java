package tm.ugur.ugur_v3.domain.vehicleManagement.specifications;

import tm.ugur.ugur_v3.domain.shared.specifications.Specification;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

public class VehicleSuitableForRouteSpecification implements Specification<Vehicle> {

    private final int minimumCapacity;
    private final VehicleType preferredVehicleType;
    private final boolean requireRecentGps;
    private final boolean excludeMaintenanceDue;

    public VehicleSuitableForRouteSpecification(int minimumCapacity) {
        this(minimumCapacity, null, true, true);
    }

    public VehicleSuitableForRouteSpecification(int minimumCapacity, VehicleType preferredVehicleType,
                                                boolean requireRecentGps, boolean excludeMaintenanceDue) {
        this.minimumCapacity = minimumCapacity;
        this.preferredVehicleType = preferredVehicleType;
        this.requireRecentGps = requireRecentGps;
        this.excludeMaintenanceDue = excludeMaintenanceDue;
    }

    @Override
    public boolean isSatisfiedBy(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }

        // Check availability for assignment
        if (!vehicle.isAvailableForAssignment()) {
            return false;
        }

        // Check capacity requirements
        VehicleCapacitySpecification capacitySpec = new VehicleCapacitySpecification(minimumCapacity);
        if (!capacitySpec.isSatisfiedBy(vehicle)) {
            return false;
        }

        // Check vehicle type preference
        if (preferredVehicleType != null && vehicle.getVehicleType() != preferredVehicleType) {
            return false;
        }

        if (requireRecentGps) {
            VehicleWithRecentGpsSpecification gpsSpec = new VehicleWithRecentGpsSpecification();
            if (!gpsSpec.isSatisfiedBy(vehicle)) {
                return false;
            }
        }

        if (excludeMaintenanceDue) {
            VehicleMaintenanceDueSpecification maintenanceSpec = VehicleMaintenanceDueSpecification.overdue();
            if (maintenanceSpec.isSatisfiedBy(vehicle)) {
                return false; // Exclude vehicles needing maintenance
            }
        }


        return isOperationallyReady(vehicle);
    }

    private boolean isOperationallyReady(Vehicle vehicle) {
        // Vehicle should not require immediate attention
        if (vehicle.requiresAttention()) {
            return false;
        }

        // Vehicle should have reasonable GPS update interval
        if (vehicle.getGpsUpdateInterval() > 300) { // More than 5 minutes is too long
            return false;
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("Vehicle suitable for route assignment");
        desc.append(" (capacity >= ").append(minimumCapacity).append(")");
        if (preferredVehicleType != null) {
            desc.append(" (type: ").append(preferredVehicleType).append(")");
        }
        if (requireRecentGps) {
            desc.append(" (with recent GPS)");
        }
        if (excludeMaintenanceDue) {
            desc.append(" (no maintenance due)");
        }
        return desc.toString();
    }

    public static VehicleSuitableForRouteSpecification standard(int minimumCapacity) {
        return new VehicleSuitableForRouteSpecification(minimumCapacity, null, true, true);
    }

    public static VehicleSuitableForRouteSpecification forVehicleType(VehicleType vehicleType, int minimumCapacity) {
        return new VehicleSuitableForRouteSpecification(minimumCapacity, vehicleType, true, true);
    }

    public static VehicleSuitableForRouteSpecification emergency(int minimumCapacity) {
        return new VehicleSuitableForRouteSpecification(minimumCapacity, null, false, false);
    }
}