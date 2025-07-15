package tm.ugur.ugur_v3.application.vehicleManagement.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.Command;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.time.Instant;

@Builder
public record ChangeVehicleStatusCommand(
        @NotNull(message = "Vehicle ID cannot be null")
        @Valid
        VehicleId vehicleId,

        @NotNull(message = "New status cannot be null")
        VehicleStatus newStatus,

        @NotEmpty(message = "Reason cannot be empty")
        @Size(min = 5, max = 200, message = "Reason must be between 5 and 200 characters")
        String reason,

        @NotEmpty(message = "Changed by cannot be empty")
        @Size(max = 100, message = "Changed by cannot exceed 100 characters")
        String changedBy,

        @NotNull(message = "Timestamp cannot be null")
        @PastOrPresent(message = "Timestamp cannot be in the future")
        Instant timestamp
) implements Command {

    public static ChangeVehicleStatusCommand create(VehicleId vehicleId,
                                                    VehicleStatus newStatus,
                                                    String reason,
                                                    String changedBy) {
        return ChangeVehicleStatusCommand.builder()
                .vehicleId(vehicleId)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(changedBy)
                .timestamp(Instant.now())
                .build();
    }

    public static ChangeVehicleStatusCommand automatic(VehicleId vehicleId,
                                                       VehicleStatus newStatus,
                                                       String reason) {
        return ChangeVehicleStatusCommand.builder()
                .vehicleId(vehicleId)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy("SYSTEM")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public CommandPriority getPriority() {
        if (newStatus == VehicleStatus.BREAKDOWN || newStatus == VehicleStatus.MAINTENANCE) {
            return CommandPriority.HIGH;
        }
        return CommandPriority.NORMAL;
    }

    public boolean isAutomaticChange() {
        return "SYSTEM".equals(changedBy);
    }

    public boolean isCriticalStatusChange() {
        return newStatus == VehicleStatus.BREAKDOWN ||
                newStatus == VehicleStatus.MAINTENANCE ||
                newStatus == VehicleStatus.INACTIVE;
    }
}