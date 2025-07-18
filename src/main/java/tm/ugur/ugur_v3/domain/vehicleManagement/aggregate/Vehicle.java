package tm.ugur.ugur_v3.domain.vehicleManagement.aggregate;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.entities.AggregateRoot;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidGeoCoordinateException;

import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.LicensePlate;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Capacity;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Bearing;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.events.VehicleCreatedEvent;
import tm.ugur.ugur_v3.domain.vehicleManagement.events.VehicleLocationUpdatedEvent;
import tm.ugur.ugur_v3.domain.vehicleManagement.events.VehicleStatusChangedEvent;

@Getter
public class Vehicle extends AggregateRoot<VehicleId> {

    private final LicensePlate licensePlate;
    private final VehicleType vehicleType;
    private final Capacity capacity;
    private final String model;

    private VehicleStatus status;
    private String assignedRouteId;

    private GeoCoordinate currentLocation;
    private GeoCoordinate previousLocation;
    private Speed currentSpeed;
    private Bearing currentBearing;
    private Timestamp lastLocationUpdate;

    private long odometer;
    private Timestamp lastMaintenanceDate;
    private Timestamp nextMaintenanceDate;

    private transient boolean locationChanged = false;

    public Vehicle(VehicleId id, String licensePlate, VehicleType vehicleType,
                   Integer capacity, String model) {
        super(id);

        this.licensePlate = LicensePlate.of(licensePlate);
        this.vehicleType = validateVehicleType(vehicleType);
        this.capacity = Capacity.fromTotal(validateCapacity(capacity));
        this.model = validateModel(model);

        this.status = VehicleStatus.AT_DEPOT;
        this.odometer = 0;
        this.currentSpeed = Speed.zero();
        this.currentBearing = Bearing.north();
        this.lastLocationUpdate = Timestamp.now();

        addDomainEvent(VehicleCreatedEvent.of(id, licensePlate, vehicleType, capacity, model, null, "SYSTEM"));
    }

    public Vehicle(VehicleId id, Long version, Timestamp createdAt, Timestamp updatedAt,
                   LicensePlate licensePlate, VehicleType vehicleType, Capacity capacity,
                   String model, VehicleStatus status, String assignedRouteId,
                   GeoCoordinate currentLocation, GeoCoordinate previousLocation,
                   Speed currentSpeed, Bearing currentBearing, Timestamp lastLocationUpdate,
                   Long odometer, Timestamp lastMaintenanceDate, Timestamp nextMaintenanceDate) {
        super(id, version, createdAt, updatedAt);

        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.model = model;
        this.status = status;
        this.assignedRouteId = assignedRouteId;
        this.currentLocation = currentLocation;
        this.previousLocation = previousLocation;
        this.currentSpeed = currentSpeed;
        this.currentBearing = currentBearing;
        this.lastLocationUpdate = lastLocationUpdate;
        this.odometer = odometer != null ? odometer : 0L;
        this.lastMaintenanceDate = lastMaintenanceDate;
        this.nextMaintenanceDate = nextMaintenanceDate;
    }

    public void updateLocation(GeoCoordinate newLocation, Speed speed, Bearing bearing) {
        validateLocationUpdate(newLocation, speed, bearing);

        if (!isSignificantLocationChange(newLocation)) {
            return;
        }

        this.previousLocation = this.currentLocation;
        this.currentLocation = newLocation;
        this.currentSpeed = speed;
        this.currentBearing = bearing;
        this.lastLocationUpdate = Timestamp.now();
        this.locationChanged = true;

        if (previousLocation != null) {
            updateOdometer();
        }

        // Publish location update event
        addDomainEvent(VehicleLocationUpdatedEvent.of(
                getId(),
                newLocation,
                previousLocation,
                speed.getMs(),
                bearing.getDegrees(),
                assignedRouteId,
                status,
                null,
                null
        ));


        if (isInRoute()) {
            checkStopArrival();
        }

        markAsModified();
    }


    public void updateLocationFromGpsApi(double latitude, double longitude, double accuracy,
                                         double speedMs, double bearingDegrees) {
        GeoCoordinate location = GeoCoordinate.of(latitude, longitude, accuracy);
        Speed speed = Speed.fromGpsApi(speedMs);
        Bearing bearing = Bearing.fromGpsApi(bearingDegrees);

        updateLocation(location, speed, bearing);
    }

    public void changeStatus(VehicleStatus newStatus, String reason, String changedBy) {
        validateStatusChange(newStatus, reason, changedBy);

        VehicleStatus oldStatus = this.status;
        this.status = newStatus;

        handleStatusChangeEffects(oldStatus, newStatus);

        addDomainEvent(VehicleStatusChangedEvent.of(
                getId(),
                oldStatus,
                newStatus,
                reason,
                changedBy,
                assignedRouteId
        ));

        markAsModified();
    }

    public void assignToRoute(String routeId) {
        validateRouteAssignment(routeId);

        this.assignedRouteId = routeId;

        if (status == VehicleStatus.ACTIVE || status == VehicleStatus.AT_DEPOT) {
            changeStatus(VehicleStatus.IN_ROUTE, "Assigned to route " + routeId, "SYSTEM");
        }

        markAsModified();
    }

    public void unassignFromRoute(String reason) {
        if (assignedRouteId == null) {
            throw new BusinessRuleViolationException(
                    "Vehicle is not assigned to any route",
                    "VEHICLE_NOT_ASSIGNED"
            );
        }

        String oldRouteId = this.assignedRouteId;
        this.assignedRouteId = null;

        if (status == VehicleStatus.IN_ROUTE) {
            changeStatus(VehicleStatus.ACTIVE, reason, "SYSTEM");
        }

        markAsModified();
    }

    public void scheduleMaintenance(Timestamp maintenanceDate, String reason) {
        if (maintenanceDate == null) {
            throw new BusinessRuleViolationException(
                    "Maintenance date cannot be null",
                    "INVALID_MAINTENANCE_DATE"
            );
        }

        this.nextMaintenanceDate = maintenanceDate;

        if (maintenanceDate.isBefore(Timestamp.now())) {
            changeStatus(VehicleStatus.MAINTENANCE, reason, "SYSTEM");
        }

        markAsModified();
    }

    public void completeMaintenance(String completedBy) {
        if (status != VehicleStatus.MAINTENANCE) {
            throw new BusinessRuleViolationException(
                    "Vehicle is not in maintenance status",
                    "VEHICLE_NOT_IN_MAINTENANCE"
            );
        }

        this.lastMaintenanceDate = Timestamp.now();
        this.nextMaintenanceDate = null; // Clear scheduled maintenance

        changeStatus(VehicleStatus.ACTIVE, "Maintenance completed", completedBy);
        markAsModified();
    }


    private boolean isSignificantLocationChange(GeoCoordinate newLocation) {
        if (currentLocation == null) {
            return true;
        }

        double distanceMeters = currentLocation.distanceTo(newLocation);

        double minimumDistance = switch (status) {
            case IN_ROUTE -> 5.0;
            case ACTIVE -> 10.0;
            case AT_DEPOT -> 20.0;
            default -> 50.0;
        };

        return distanceMeters >= minimumDistance;
    }

    private void updateOdometer() {
        if (previousLocation != null && currentLocation != null) {
            double distanceMeters = previousLocation.distanceTo(currentLocation);
            this.odometer += Math.round(distanceMeters);
        }
    }

    private void checkStopArrival() {
        // This would integrate with route/stop data
        // For now, we check if vehicle is moving slowly (potential stop)
        if (currentSpeed != null && currentSpeed.isStationary()) {
            // Could trigger stop arrival event here
            // Would need stop location data to determine actual arrival
        }
    }

    private void handleStatusChangeEffects(VehicleStatus oldStatus, VehicleStatus newStatus) {

        if (newStatus == VehicleStatus.MAINTENANCE || newStatus == VehicleStatus.BREAKDOWN) {
            if (assignedRouteId != null) {
                String routeId = this.assignedRouteId;
                this.assignedRouteId = null;
            }
        }

        if (newStatus == VehicleStatus.BREAKDOWN) {
            // Could trigger breakdown emergency event here
        }
    }


    private void validateLocationUpdate(GeoCoordinate newLocation, Speed speed, Bearing bearing) {
        if (newLocation == null) {
            throw new InvalidGeoCoordinateException("Location cannot be null");
        }

        if (!status.isTrackable()) {
            throw new BusinessRuleViolationException(
                    "GPS tracking is not allowed for status: " + status,
                    "LOCATION_UPDATE_NOT_ALLOWED"
            );
        }

        if (speed == null) {
            throw new BusinessRuleViolationException(
                    "Speed cannot be null",
                    "INVALID_SPEED"
            );
        }

        if (bearing == null) {
            throw new BusinessRuleViolationException(
                    "Bearing cannot be null",
                    "INVALID_BEARING"
            );
        }
    }

    private void validateStatusChange(VehicleStatus newStatus, String reason, String changedBy) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        // ✅ ИСПРАВЛЕНО: Инвертирована логика
        if (!status.canTransitionTo(newStatus)) { // ✅ ПРАВИЛЬНО!
            throw new BusinessRuleViolationException(
                    "INVALID_STATUS_TRANSITION",
                    String.format("Cannot transition from %s to %s", status, newStatus)
            );
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Status change reason cannot be null or empty");
        }

        if (changedBy == null || changedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Changed by cannot be null or empty");
        }

        // Enhanced business rules
        validateStatusChangeAuthorization(newStatus, changedBy);
        validateStatusChangeBusinessRules(newStatus);
    }

    private void validateRouteAssignment(String routeId) {
        if (routeId == null || routeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Route ID cannot be null or empty");
        }

        if (!status.isAvailableForAssignment()) {
            throw new BusinessRuleViolationException(
                    "Vehicle status " + status + " does not allow route assignment",
                    "ROUTE_ASSIGNMENT_NOT_ALLOWED"
            );
        }

        if (assignedRouteId != null) {
            throw new BusinessRuleViolationException(
                    "Vehicle is already assigned to route: " + assignedRouteId,
                    "VEHICLE_ALREADY_ASSIGNED"
            );
        }
    }

    private VehicleType validateVehicleType(VehicleType vehicleType) {
        if (vehicleType == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null");
        }
        return vehicleType;
    }

    private Integer validateCapacity(Integer capacity) {
        if (capacity == null || capacity <= 0 || capacity > 400) {
            throw new IllegalArgumentException("Capacity must be between 1 and 400: " + capacity);
        }
        return capacity;
    }

    private String validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be null or empty");
        }
        if (model.length() > 50) {
            throw new IllegalArgumentException("Model name too long: " + model.length());
        }
        return model.trim();
    }


    public boolean isInRoute() {
        return assignedRouteId != null && status == VehicleStatus.IN_ROUTE;
    }

    public boolean isTrackable() {
        return status.isTrackable();
    }

    public boolean requiresAttention() {
        return status.requiresImmediateAttention() || needsMaintenance();
    }

    public boolean needsMaintenance() {
        return status == VehicleStatus.MAINTENANCE ||
                (nextMaintenanceDate != null && Timestamp.now().isAfter(nextMaintenanceDate));
    }

    public int getGpsUpdateInterval() {
        return status.getGpsUpdateIntervalSeconds();
    }

    public boolean isAvailableForAssignment() {
        return assignedRouteId == null && status.isAvailableForAssignment();
    }

    public double getCurrentUtilization(int currentPassengers) {
        return capacity.calculateUtilization(currentPassengers);
    }

    public boolean isMoving() {
        return currentSpeed != null && currentSpeed.isMoving();
    }

    public double getTotalDistanceKm() {
        return odometer / 1000.0;
    }

    public double getCurrentSpeedKmh() {
        return currentSpeed != null ? currentSpeed.getKmh() : 0.0;
    }

    public double getCurrentBearingDegrees() {
        return currentBearing != null ? currentBearing.getDegrees() : 0.0;
    }

    public boolean hasRecentGpsData() {
        if (lastLocationUpdate == null) return false;

        long minutesSinceUpdate = java.time.Duration.between(
                lastLocationUpdate.toInstant(),
                Timestamp.now().toInstant()
        ).toMinutes();

        return minutesSinceUpdate <= 5;
    }


    private void validateStatusChangeAuthorization(VehicleStatus newStatus, String changedBy) {
        if (isCriticalStatusChange(newStatus)) {
            if (!changedBy.startsWith("ADMIN_") && !changedBy.equals("SYSTEM")) {
                throw new BusinessRuleViolationException(
                        "UNAUTHORIZED_CRITICAL_STATUS_CHANGE",
                        String.format("Status change to %s requires admin authorization (changed by: %s)",
                                newStatus.name(), changedBy)
                );
            }
        }

        if (isEmergencyStatusChange(newStatus)) {
            if (!changedBy.equals("EMERGENCY_SYSTEM") && !changedBy.startsWith("DISPATCHER_")) {
                throw new BusinessRuleViolationException(
                        "UNAUTHORIZED_EMERGENCY_STATUS_CHANGE",
                        String.format("Emergency status change to %s requires dispatcher authorization",
                                newStatus.name())
                );
            }
        }
    }

    private void validateStatusChangeBusinessRules(VehicleStatus newStatus) {
        switch (newStatus) {
            case IN_ROUTE -> {
                if (assignedRouteId == null) {
                    throw new BusinessRuleViolationException(
                            "ROUTE_REQUIRED_FOR_IN_ROUTE",
                            "Vehicle must have assigned route before transitioning to IN_ROUTE status"
                    );
                }
                if (currentLocation == null) {
                    throw new BusinessRuleViolationException(
                            "LOCATION_REQUIRED_FOR_IN_ROUTE",
                            "Vehicle must have current location before starting route"
                    );
                }
            }
            case ACTIVE -> {
                if (needsMaintenance()) {
                    throw new BusinessRuleViolationException(
                            "MAINTENANCE_REQUIRED_BEFORE_ACTIVATION",
                            "Vehicle requires maintenance before activation"
                    );
                }
                if (isLowFuel()) {
                    throw new BusinessRuleViolationException(
                            "FUEL_CHECK_REQUIRED",
                            "Vehicle fuel level must be checked before activation"
                    );
                }
            }
            case MAINTENANCE -> {
                if (status == VehicleStatus.IN_ROUTE) {
                    throw new BusinessRuleViolationException(
                            "CANNOT_MAINTENANCE_WHILE_IN_ROUTE",
                            "Vehicle cannot enter maintenance while in route. Complete route first"
                    );
                }
                if (hasPassengers()) {
                    throw new BusinessRuleViolationException(
                            "CANNOT_MAINTENANCE_WITH_PASSENGERS",
                            "Vehicle cannot enter maintenance with passengers on board"
                    );
                }
            }

            case RETIRED -> {
                validateRetirementEligibility();
            }
        }
    }

    private boolean isCriticalStatusChange(VehicleStatus newStatus) {
        return newStatus == VehicleStatus.RETIRED ||
                newStatus == VehicleStatus.INACTIVE;
    }

    private boolean isEmergencyStatusChange(VehicleStatus newStatus) {
        return newStatus == VehicleStatus.BREAKDOWN;
    }

    private boolean isLowFuel() {
        // В реальной системе это будет читаться из telemetry
        // Пока заглушка
        return false;
    }

    private boolean hasPassengers() {
        // В реальной системе это будет читаться из passenger counting system
        // Пока заглушка
        return false;
    }

    private void logEmergencyStatusChange() {
        // В реальной системе здесь будет отправка alert в emergency services
        // Пока заглушка
    }

    private void validateRetirementEligibility() {
        if (status == VehicleStatus.IN_ROUTE) {
            throw new BusinessRuleViolationException(
                    "CANNOT_RETIRE_WHILE_IN_ROUTE",
                    "Vehicle cannot be retired while in route"
            );
        }

        if (assignedRouteId != null) {
            throw new BusinessRuleViolationException(
                    "CANNOT_RETIRE_WITH_ASSIGNED_ROUTE",
                    "Vehicle cannot be retired while assigned to route"
            );
        }

        // Additional retirement checks can be added here
        // - Outstanding maintenance issues
        // - Financial obligations
        // - Regulatory requirements
    }
}