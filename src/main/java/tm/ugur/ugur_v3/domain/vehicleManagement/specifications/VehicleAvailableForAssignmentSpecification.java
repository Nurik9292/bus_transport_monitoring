package tm.ugur.ugur_v3.domain.vehicleManagement.specifications;

import tm.ugur.ugur_v3.domain.shared.specifications.Specification;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

public class VehicleAvailableForAssignmentSpecification implements Specification<Vehicle> {

    private static final double MIN_FUEL_LEVEL = 20.0;

    private static final long MAX_GPS_AGE_MINUTES = 10;

    private final Integer minimumCapacity;
    private final boolean requireRecentGpsUpdate;

    public VehicleAvailableForAssignmentSpecification() {
        this(null, true);
    }

    public VehicleAvailableForAssignmentSpecification(Integer minimumCapacity,
                                                      boolean requireRecentGpsUpdate) {
        this.minimumCapacity = minimumCapacity;
        this.requireRecentGpsUpdate = requireRecentGpsUpdate;
    }

    @Override
    public boolean isSatisfiedBy(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }

        if (isStatusSuitableForAssignment(vehicle)) {
            return false;
        }

        if (isAlreadyAssigned(vehicle)) {
            return false;
        }

        if (hasSufficientFuel(vehicle)) {
            return false;
        }

        if (requiresMaintenance(vehicle)) {
            return false;
        }

        if (meetCapacityRequirement(vehicle)) {
            return false;
        }

        if (requireRecentGpsUpdate && hasRecentGpsUpdate(vehicle)) {
            return false;
        }

        return true;
    }

    private boolean isStatusSuitableForAssignment(Vehicle vehicle) {
        VehicleStatus status = vehicle.getStatus();
        return !status.isAvailableForAssignment();
    }

    public boolean isAlreadyAssigned(Vehicle vehicle) {
        return vehicle.getAssignedRouteId() != null;
    }

    private boolean hasSufficientFuel(Vehicle vehicle) {
        Double fuelLevel = vehicle.getFuelLevel();
        return fuelLevel == null || !(fuelLevel >= MIN_FUEL_LEVEL);
    }

    private boolean requiresMaintenance(Vehicle vehicle) {
        return vehicle.needsMaintenance();
    }

    private boolean meetCapacityRequirement(Vehicle vehicle) {
        if (minimumCapacity == null) {
            return false;
        }

        Integer vehicleCapacity = vehicle.getCapacity();
        return vehicleCapacity == null || vehicleCapacity < minimumCapacity;
    }

    private boolean hasRecentGpsUpdate(Vehicle vehicle) {
        if (vehicle.getLastLocationUpdate() == null) {
            return true;
        }

        long ageMinutes = java.time.Duration.between(
                vehicle.getLastLocationUpdate().toInstant(),
                java.time.Instant.now()
        ).toMinutes();

        return ageMinutes > MAX_GPS_AGE_MINUTES;
    }

    public static VehicleAvailableForAssignmentSpecification withMinimumCapacity(int capacity) {
        return new VehicleAvailableForAssignmentSpecification(capacity, true);
    }

    public static VehicleAvailableForAssignmentSpecification withoutGpsRequirement() {
        return new VehicleAvailableForAssignmentSpecification(null, false);
    }

    public static VehicleAvailableForAssignmentSpecification forEmergencyAssignment() {
        return new VehicleAvailableForAssignmentSpecification(null, false) {
            @Override
            public boolean isSatisfiedBy(Vehicle vehicle) {
                return vehicle != null &&
                        vehicle.getStatus().isInService() &&
                        !isAlreadyAssigned(vehicle);
            }
        };
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("VehicleAvailableForAssignment");

        if (minimumCapacity != null) {
            desc.append("(capacity>=").append(minimumCapacity).append(")");
        }

        if (requireRecentGpsUpdate) {
            desc.append("(recentGPS)");
        }

        return desc.toString();
    }

    public String getUnavailabilityReason(Vehicle vehicle) {
        if (vehicle == null) {
            return "Vehicle is null";
        }

        if (isStatusSuitableForAssignment(vehicle)) {
            return "Vehicle status is " + vehicle.getStatus() + " (not suitable for assignment)";
        }

        if (isAlreadyAssigned(vehicle)) {
            return "Vehicle is already assigned to route " + vehicle.getAssignedRouteId();
        }

        if (hasSufficientFuel(vehicle)) {
            return "Insufficient fuel level: " + vehicle.getFuelLevel() + "% (minimum " + MIN_FUEL_LEVEL + "%)";
        }

        if (requiresMaintenance(vehicle)) {
            return "Vehicle requires maintenance";
        }

        if (meetCapacityRequirement(vehicle)) {
            return "Vehicle capacity " + vehicle.getCapacity() + " is below required " + minimumCapacity;
        }

        if (requireRecentGpsUpdate && hasRecentGpsUpdate(vehicle)) {
            return "GPS data is stale (last update: " + vehicle.getLastLocationUpdate() + ")";
        }

        return "Vehicle is available";
    }
}