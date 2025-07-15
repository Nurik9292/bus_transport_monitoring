package tm.ugur.ugur_v3.domain.vehicleManagement.specifications;

import tm.ugur.ugur_v3.domain.shared.specifications.Specification;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

public class VehicleMaintenanceDueSpecification implements Specification<Vehicle> {

    private final Timestamp currentTime;
    private final boolean includeOverdue;
    private final boolean includeUpcoming;
    private final int upcomingDaysThreshold;

    public VehicleMaintenanceDueSpecification() {
        this(Timestamp.now(), true, false, 0);
    }

    public VehicleMaintenanceDueSpecification(Timestamp currentTime, boolean includeOverdue,
                                              boolean includeUpcoming, int upcomingDaysThreshold) {
        this.currentTime = currentTime;
        this.includeOverdue = includeOverdue;
        this.includeUpcoming = includeUpcoming;
        this.upcomingDaysThreshold = upcomingDaysThreshold;
    }

    @Override
    public boolean isSatisfiedBy(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }

        // Always include vehicles in MAINTENANCE status
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            return true;
        }

        // Always include vehicles with BREAKDOWN status (emergency maintenance)
        if (vehicle.getStatus() == VehicleStatus.BREAKDOWN) {
            return true;
        }

        // Check scheduled maintenance dates
        Timestamp nextMaintenanceDate = vehicle.getNextMaintenanceDate();
        if (nextMaintenanceDate != null) {

            if (includeOverdue && nextMaintenanceDate.isBefore(currentTime)) {
                return true; // Overdue maintenance
            }

            if (includeUpcoming && upcomingDaysThreshold > 0) {
                Timestamp upcomingThreshold = currentTime.plusMillis(upcomingDaysThreshold * 24 * 60 * 60 * 1000L);
                if (nextMaintenanceDate.isBefore(upcomingThreshold)) {
                    return true; // Upcoming maintenance
                }
            }
        }

        // Check mileage-based maintenance requirements
        return isMaintenanceRequiredByMileage(vehicle);
    }

    private boolean isMaintenanceRequiredByMileage(Vehicle vehicle) {
        if (vehicle.getLastMaintenanceDate() == null) {
            // No previous maintenance recorded - check total mileage
            return vehicle.getTotalDistanceKm() > getMaxDistanceWithoutMaintenance(vehicle.getVehicleType());
        }

        // Calculate distance since last maintenance
        // This would require tracking odometer at last maintenance
        // For now, use a simple heuristic based on total distance
        double totalDistance = vehicle.getTotalDistanceKm();
        double maintenanceInterval = getMaintenanceIntervalKm(vehicle.getVehicleType());

        return (totalDistance % maintenanceInterval) > (maintenanceInterval * 0.9); // 90% of interval
    }

    private double getMaxDistanceWithoutMaintenance(VehicleType vehicleType) {
        return switch (vehicleType) {
            case BUS, TROLLEY -> 15000.0; // 15,000 km for heavy-duty vehicles
            case TRAM -> 20000.0; // 20,000 km for rail vehicles
            case MINIBUS -> 12000.0; // 12,000 km for smaller vehicles

        };
    }

    private double getMaintenanceIntervalKm(VehicleType vehicleType) {
        return switch (vehicleType) {
            case BUS, TROLLEY -> 5000.0; // Every 5,000 km
            case TRAM -> 8000.0; // Every 8,000 km
            case MINIBUS -> 4000.0; // Every 4,000 km

        };
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("Vehicle maintenance due");
        if (includeOverdue) desc.append(" (including overdue)");
        if (includeUpcoming) desc.append(" (including upcoming within ").append(upcomingDaysThreshold).append(" days)");
        return desc.toString();
    }

    public static VehicleMaintenanceDueSpecification overdue() {
        return new VehicleMaintenanceDueSpecification(Timestamp.now(), true, false, 0);
    }

    public static VehicleMaintenanceDueSpecification upcoming(int daysAhead) {
        return new VehicleMaintenanceDueSpecification(Timestamp.now(), false, true, daysAhead);
    }

    public static VehicleMaintenanceDueSpecification overdueAndUpcoming(int daysAhead) {
        return new VehicleMaintenanceDueSpecification(Timestamp.now(), true, true, daysAhead);
    }
}





