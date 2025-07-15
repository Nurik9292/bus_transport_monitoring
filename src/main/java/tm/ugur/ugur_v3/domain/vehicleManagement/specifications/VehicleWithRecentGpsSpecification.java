package tm.ugur.ugur_v3.domain.vehicleManagement.specifications;

import tm.ugur.ugur_v3.domain.shared.specifications.Specification;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;

public class VehicleWithRecentGpsSpecification implements Specification<Vehicle> {

    private final Timestamp cutoffTime;
    private final boolean requireTrackableStatus;
    private final boolean requireHighAccuracy;

    public VehicleWithRecentGpsSpecification() {
        this(Timestamp.now().minusMillis(5 * 60 * 1000), true, false);
    }

    public VehicleWithRecentGpsSpecification(Timestamp cutoffTime, boolean requireTrackableStatus,
                                             boolean requireHighAccuracy) {
        this.cutoffTime = cutoffTime;
        this.requireTrackableStatus = requireTrackableStatus;
        this.requireHighAccuracy = requireHighAccuracy;
    }

    @Override
    public boolean isSatisfiedBy(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }

        if (requireTrackableStatus && !vehicle.isTrackable()) {
            return false;
        }

        Timestamp lastUpdate = vehicle.getLastLocationUpdate();
        if (lastUpdate == null || lastUpdate.isBefore(cutoffTime)) {
            return false;
        }

        // Check GPS accuracy if required
        if (requireHighAccuracy && vehicle.getCurrentLocation() != null) {
            double accuracy = vehicle.getCurrentLocation().getAccuracy();
            if (accuracy <= 0 || accuracy > 50.0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return String.format("Vehicle with GPS data after %s%s%s",
                cutoffTime,
                requireTrackableStatus ? " (trackable status required)" : "",
                requireHighAccuracy ? " (high accuracy required)" : "");
    }

    public static VehicleWithRecentGpsSpecification withinMinutes(int minutes) {
        Timestamp cutoff = Timestamp.now().minusMillis(minutes * 60 * 1000L);
        return new VehicleWithRecentGpsSpecification(cutoff, true, false);
    }

    public static VehicleWithRecentGpsSpecification recentHighAccuracy(int minutes) {
        Timestamp cutoff = Timestamp.now().minusMillis(minutes * 60 * 1000L);
        return new VehicleWithRecentGpsSpecification(cutoff, true, true);
    }

    public static Specification<Vehicle> withStaleGps(int minutes) {
        return new VehicleWithRecentGpsSpecification(
                Timestamp.now().minusMillis(minutes * 60 * 1000L), true, false
        ).not();
    }
}