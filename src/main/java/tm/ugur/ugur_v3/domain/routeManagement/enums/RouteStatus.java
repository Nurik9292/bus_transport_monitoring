package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum RouteStatus {

    DRAFT("Draft", "Route is being planned", false, false),
    PENDING_APPROVAL("Pending Approval", "Route awaits approval", false, false),
    APPROVED("Approved", "Route approved but not active", false, true),
    ACTIVE("Active", "Route is in active service", true, true),
    SUSPENDED("Suspended", "Route temporarily suspended", false, true),
    MAINTENANCE("Maintenance", "Route under maintenance", false, true),
    INACTIVE("Inactive", "Route not in service", false, true),
    ARCHIVED("Archived", "Route permanently archived", false, false);

    private final String displayName;
    private final String description;
    private final boolean canAcceptVehicles;
    private final boolean canBeModified;

    RouteStatus(String displayName, String description, boolean canAcceptVehicles, boolean canBeModified) {
        this.displayName = displayName;
        this.description = description;
        this.canAcceptVehicles = canAcceptVehicles;
        this.canBeModified = canBeModified;
    }

    public boolean canTransitionTo(RouteStatus newStatus) {
        return switch (this) {
            case DRAFT -> newStatus == PENDING_APPROVAL || newStatus == ARCHIVED;
            case PENDING_APPROVAL -> newStatus == APPROVED || newStatus == DRAFT || newStatus == ARCHIVED;
            case APPROVED -> newStatus == ACTIVE || newStatus == INACTIVE || newStatus == ARCHIVED;
            case ACTIVE -> newStatus == SUSPENDED || newStatus == MAINTENANCE || newStatus == INACTIVE;
            case SUSPENDED -> newStatus == ACTIVE || newStatus == INACTIVE || newStatus == MAINTENANCE;
            case MAINTENANCE -> newStatus == ACTIVE || newStatus == INACTIVE || newStatus == SUSPENDED;
            case INACTIVE -> newStatus == ACTIVE || newStatus == ARCHIVED;
            case ARCHIVED -> false;
        };
    }

    public void validateTransition(RouteStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new BusinessRuleViolationException(
                    "ROUTE_STATUS_CHANGE_NOT_ALLOWED",
                    String.format("Cannot transition from %s to %s", this, newStatus)
            );
        }
    }

    public boolean isOperational() {
        return this == ACTIVE;
    }

    public boolean isInService() {
        return this == ACTIVE || this == SUSPENDED;
    }

    public boolean requiresApproval() {
        return this == PENDING_APPROVAL;
    }

    public boolean isPermanent() {
        return this == ARCHIVED;
    }

    public static Set<RouteStatus> getVehicleAssignableStatuses() {
        return Arrays.stream(values())
                .filter(RouteStatus::isCanAcceptVehicles)
                .collect(Collectors.toSet());
    }

    public static Set<RouteStatus> getOperationalStatuses() {
        return Set.of(ACTIVE, SUSPENDED, MAINTENANCE);
    }
}