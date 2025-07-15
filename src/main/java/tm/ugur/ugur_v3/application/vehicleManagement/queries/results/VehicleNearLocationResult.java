package tm.ugur.ugur_v3.application.vehicleManagement.queries.results;

import lombok.Builder;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Builder
public record VehicleNearLocationResult(
        VehicleId vehicleId,
        Vehicle vehicle,
        double distanceMeters,
        double bearingDegrees,
        double currentSpeedKmh,
        VehicleStatus status,
        GeoCoordinate location,
        Timestamp lastLocationUpdate,
        boolean isMoving
) {



    public static VehicleNearLocationResult of(Vehicle vehicle, GeoCoordinate searchLocation) {
        GeoCoordinate vehicleLocation = vehicle.getCurrentLocation();

        if (vehicleLocation == null) {
            return withoutDistance(
                    vehicle.getId(),
                    vehicle,
                    vehicle.getCurrentSpeedKmh(),
                    vehicle.getStatus(),
                    vehicle.getLastLocationUpdate(),
                    vehicle.isMoving()
            );
        }

        double distance = vehicleLocation.distanceTo(searchLocation);
        double bearing = vehicleLocation.bearingTo(searchLocation);

        return VehicleNearLocationResult.builder()
                .vehicleId(vehicle.getId())
                .vehicle(vehicle)
                .distanceMeters(distance)
                .bearingDegrees(bearing)
                .currentSpeedKmh(vehicle.getCurrentSpeedKmh())
                .status(vehicle.getStatus())
                .location(vehicleLocation)
                .lastLocationUpdate(vehicle.getLastLocationUpdate())
                .isMoving(vehicle.isMoving())
                .build();
    }

    public static VehicleNearLocationResult withoutDistance(VehicleId vehicleId,
                                                            Vehicle vehicle,
                                                            double currentSpeedKmh,
                                                            VehicleStatus status,
                                                            Timestamp lastLocationUpdate,
                                                            boolean isMoving) {
        return VehicleNearLocationResult.builder()
                .vehicleId(vehicleId)
                .vehicle(vehicle)
                .distanceMeters(Double.MAX_VALUE)
                .bearingDegrees(0.0)
                .currentSpeedKmh(currentSpeedKmh)
                .status(status)
                .location(null)
                .lastLocationUpdate(lastLocationUpdate)
                .isMoving(isMoving)
                .build();
    }

    public static VehicleNearLocationResult withoutCalculatedDistance(VehicleId vehicleId,
                                                                      Vehicle vehicle,
                                                                      double currentSpeedKmh,
                                                                      VehicleStatus status,
                                                                      GeoCoordinate location,
                                                                      Timestamp lastLocationUpdate,
                                                                      boolean isMoving) {
        return VehicleNearLocationResult.builder()
                .vehicleId(vehicleId)
                .vehicle(vehicle)
                .distanceMeters(0.0)
                .bearingDegrees(0.0)
                .currentSpeedKmh(currentSpeedKmh)
                .status(status)
                .location(location)
                .lastLocationUpdate(lastLocationUpdate)
                .isMoving(isMoving)
                .build();
    }



    public boolean hasValidLocation() {
        return location != null;
    }

    public boolean hasDistance() {
        return distanceMeters != Double.MAX_VALUE && distanceMeters >= 0;
    }

    public double getDistanceKm() {
        return hasDistance() ? distanceMeters / 1000.0 : Double.MAX_VALUE;
    }

    public boolean isWithinDistance(double maxDistanceMeters) {
        return hasDistance() && distanceMeters <= maxDistanceMeters;
    }

    public boolean isVeryClose() {
        return hasDistance() && distanceMeters <= 100.0;
    }

    public boolean isNearby() {
        return hasDistance() && distanceMeters <= 1000.0;
    }

    public boolean isFarAway() {
        return hasDistance() && distanceMeters > 5000.0;
    }

    public boolean isActive() {
        return status == VehicleStatus.ACTIVE || status == VehicleStatus.IN_ROUTE;
    }

    public boolean isAvailable() {
        return status == VehicleStatus.ACTIVE && !isMoving;
    }

    public boolean hasRecentLocation() {
        if (lastLocationUpdate == null) return false;

        long minutesSinceUpdate = java.time.Duration.between(
                lastLocationUpdate.toInstant(),
                java.time.Instant.now()
        ).toMinutes();

        return minutesSinceUpdate <= 10;
    }

    public String getLocationDescription() {
        if (!hasValidLocation()) {
            return "Location unknown";
        }

        if (!hasDistance()) {
            return String.format("At (%.6f, %.6f)", location.getLatitude(), location.getLongitude());
        }

        return String.format("%.0fm away at (%.6f, %.6f)",
                distanceMeters,
                location.getLatitude(),
                location.getLongitude());
    }

    public String getMovementDescription() {
        if (!isMoving) {
            return "Stationary";
        }

        if (currentSpeedKmh <= 0) {
            return "Stopped";
        }

        return String.format("Moving at %.1f km/h", currentSpeedKmh);
    }

    public String getProximityCategory() {
        if (!hasDistance()) {
            return "UNKNOWN_DISTANCE";
        }

        if (isVeryClose()) return "VERY_CLOSE";
        if (isNearby()) return "NEARBY";
        if (isFarAway()) return "FAR";
        return "MODERATE_DISTANCE";
    }



    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Vehicle %s (%s)",
                vehicleId.getValue(),
                status.name()));

        if (hasDistance()) {
            summary.append(String.format(" - %.0fm away", distanceMeters));
        }

        if (isMoving) {
            summary.append(String.format(" - moving %.1f km/h", currentSpeedKmh));
        } else {
            summary.append(" - stationary");
        }

        return summary.toString();
    }

    public String getDetailedInfo() {
        return String.format("Vehicle[id=%s, status=%s, distance=%.0fm, speed=%.1fkm/h, moving=%s, location=%s]",
                vehicleId.getValue(),
                status.name(),
                hasDistance() ? distanceMeters : -1,
                currentSpeedKmh,
                isMoving ? "yes" : "no",
                hasValidLocation() ? location.toString() : "unknown");
    }



    public boolean isCloserThan(VehicleNearLocationResult other) {
        if (!this.hasDistance() || !other.hasDistance()) {
            return false;
        }
        return this.distanceMeters < other.distanceMeters;
    }

    public boolean isFasterThan(VehicleNearLocationResult other) {
        return this.currentSpeedKmh > other.currentSpeedKmh;
    }

    public boolean isMoreRecentThan(VehicleNearLocationResult other) {
        if (this.lastLocationUpdate == null || other.lastLocationUpdate == null) {
            return false;
        }
        return this.lastLocationUpdate.isAfter(other.lastLocationUpdate);
    }



    public java.util.Map<String, Object> toMap() {
        return java.util.Map.of(
                "vehicleId", vehicleId.getValue(),
                "status", status.name(),
                "distanceMeters", hasDistance() ? distanceMeters : -1,
                "distanceKm", hasDistance() ? getDistanceKm() : -1,
                "bearingDegrees", bearingDegrees,
                "currentSpeedKmh", currentSpeedKmh,
                "isMoving", isMoving,
                "proximityCategory", getProximityCategory(),
                "hasValidLocation", hasValidLocation(),
                "hasRecentLocation", hasRecentLocation()
        );
    }
}