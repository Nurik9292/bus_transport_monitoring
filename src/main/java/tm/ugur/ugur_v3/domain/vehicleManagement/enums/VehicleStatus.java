package tm.ugur.ugur_v3.domain.vehicleManagement.enums;

import lombok.Getter;

@Getter
public enum VehicleStatus {

    ACTIVE("Активен", true),

    IN_ROUTE("На маршруте", true),

    INACTIVE("Не активен", false),

    AT_DEPOT("В депо", false),

    MAINTENANCE("Техобслуживание", false),

    BREAKDOWN("Поломка", false),

    RETIRED("Списан", false);

    private final String displayName;
    private final boolean availableForTracking;

    VehicleStatus(String displayName, boolean availableForTracking) {
        this.displayName = displayName;
        this.availableForTracking = availableForTracking;
    }

    public boolean canTransitionTo(VehicleStatus newStatus) {
        return !switch (this) {
            case ACTIVE -> newStatus != RETIRED;
            case IN_ROUTE -> newStatus == ACTIVE || newStatus == AT_DEPOT || newStatus == BREAKDOWN;
            case AT_DEPOT -> newStatus == ACTIVE || newStatus == MAINTENANCE || newStatus == RETIRED;
            case MAINTENANCE -> newStatus == ACTIVE || newStatus == AT_DEPOT || newStatus == RETIRED;
            case BREAKDOWN -> newStatus == MAINTENANCE || newStatus == RETIRED;
            case RETIRED, INACTIVE -> false;
        };
    }

    public boolean isTrackable() {
        return availableForTracking;
    }

    public boolean isAvailableForAssignment() {
        return this == ACTIVE || this == AT_DEPOT;
    }

    public boolean isInService() {
        return this == ACTIVE || this == IN_ROUTE;
    }

    public int getProcessingPriority() {
        return switch (this) {
            case BREAKDOWN -> 1;
            case IN_ROUTE -> 2;
            case ACTIVE -> 3;
            case AT_DEPOT -> 4;
            case MAINTENANCE -> 5;
            case RETIRED -> 6;
            case INACTIVE -> 7;
        };
    }

    public int getGpsUpdateIntervalSeconds() {
        return switch (this) {
            case BREAKDOWN -> 10;
            case IN_ROUTE -> 30;
            case ACTIVE -> 60;
            case AT_DEPOT -> 300;
            case MAINTENANCE, RETIRED, INACTIVE -> 0;
        };
    }


    public boolean requiresImmediateAttention() {
        return this == BREAKDOWN;
    }
}
