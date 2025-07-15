package tm.ugur.ugur_v3.application.vehicleManagement.commands;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.exceptions.InvalidStatusTransitionException;
import tm.ugur.ugur_v3.domain.vehicleManagement.exceptions.VehicleStatusValidationException;

@Log4j2
@Component
public class VehicleStatusTransitionValidator {


    public void validateTransition(VehicleStatus fromStatus, VehicleStatus toStatus, String reason) {
        if (fromStatus.canTransitionTo(toStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot transition from %s to %s", fromStatus, toStatus)
            );
        }

        validateTransitionReason(fromStatus, toStatus, reason);
        validateTransitionTiming(fromStatus, toStatus);

        log.debug("Status transition validated: {} -> {} (reason: {})", fromStatus, toStatus, reason);
    }

    private void validateTransitionReason(VehicleStatus fromStatus, VehicleStatus toStatus, String reason) {
        if (isCriticalTransition(fromStatus, toStatus) && reason.length() < 10) {
            throw new VehicleStatusValidationException(
                    "Critical status transitions require detailed justification (minimum 10 characters)"
            );
        }

        if (reason.toLowerCase().contains("test") && toStatus == VehicleStatus.BREAKDOWN) {
            throw new VehicleStatusValidationException(
                    "Test transitions to BREAKDOWN status are not allowed"
            );
        }
    }

    private void validateTransitionTiming(VehicleStatus fromStatus, VehicleStatus toStatus) {
        // Example: Don't allow frequent status changes
        // This would require tracking recent status changes

        // Example: Maintenance should not be scheduled during peak hours
        if (toStatus == VehicleStatus.MAINTENANCE) {
            // Could add business logic for maintenance scheduling constraints
        }
    }

    private boolean isCriticalTransition(VehicleStatus fromStatus, VehicleStatus toStatus) {
        return toStatus == VehicleStatus.BREAKDOWN ||
                toStatus == VehicleStatus.RETIRED ||
                (fromStatus == VehicleStatus.IN_ROUTE && toStatus != VehicleStatus.ACTIVE);
    }
}
