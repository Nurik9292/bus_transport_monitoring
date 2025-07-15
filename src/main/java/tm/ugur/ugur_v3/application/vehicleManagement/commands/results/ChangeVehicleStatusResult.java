
package tm.ugur.ugur_v3.application.vehicleManagement.commands.results;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.CommandResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Builder
public record ChangeVehicleStatusResult(
        VehicleId vehicleId,
        boolean success,
        VehicleStatus previousStatus,
        VehicleStatus newStatus,
        String errorMessage,
        String errorCode,
        Duration processingTime,
        Instant timestamp,
        boolean wasAutomaticChange,
        String changedBy,
        Optional<String> reason,
        Map<String, Object> metadata
) implements CommandResult {



    public static ChangeVehicleStatusResult success(VehicleId vehicleId,
                                                    VehicleStatus previousStatus,
                                                    VehicleStatus newStatus,
                                                    Duration processingTime,
                                                    String changedBy) {
        return ChangeVehicleStatusResult.builder()
                .vehicleId(vehicleId)
                .success(true)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .errorMessage(null)
                .errorCode(null)
                .processingTime(processingTime)
                .timestamp(Instant.now())
                .wasAutomaticChange(false)
                .changedBy(changedBy)
                .reason(Optional.empty())
                .metadata(Map.of(
                        "statusTransition", previousStatus.name() + " -> " + newStatus.name(),
                        "isSuccessful", true,
                        "isCriticalChange", isCriticalStatusChange(previousStatus, newStatus)
                ))
                .build();
    }

    public static ChangeVehicleStatusResult successWithReason(VehicleId vehicleId,
                                                              VehicleStatus previousStatus,
                                                              VehicleStatus newStatus,
                                                              Duration processingTime,
                                                              String changedBy,
                                                              String reason) {
        return ChangeVehicleStatusResult.builder()
                .vehicleId(vehicleId)
                .success(true)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .errorMessage(null)
                .errorCode(null)
                .processingTime(processingTime)
                .timestamp(Instant.now())
                .wasAutomaticChange(false)
                .changedBy(changedBy)
                .reason(Optional.of(reason))
                .metadata(Map.of(
                        "statusTransition", previousStatus.name() + " -> " + newStatus.name(),
                        "isSuccessful", true,
                        "hasReason", true,
                        "reason", reason,
                        "isCriticalChange", isCriticalStatusChange(previousStatus, newStatus)
                ))
                .build();
    }

    public static ChangeVehicleStatusResult automaticSuccess(VehicleId vehicleId,
                                                             VehicleStatus previousStatus,
                                                             VehicleStatus newStatus,
                                                             Duration processingTime,
                                                             String reason) {
        return ChangeVehicleStatusResult.builder()
                .vehicleId(vehicleId)
                .success(true)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .errorMessage(null)
                .errorCode(null)
                .processingTime(processingTime)
                .timestamp(Instant.now())
                .wasAutomaticChange(true)
                .changedBy("SYSTEM")
                .reason(Optional.of(reason))
                .metadata(Map.of(
                        "statusTransition", previousStatus.name() + " -> " + newStatus.name(),
                        "isSuccessful", true,
                        "isAutomatic", true,
                        "automaticReason", reason,
                        "isCriticalChange", isCriticalStatusChange(previousStatus, newStatus)
                ))
                .build();
    }



    public static ChangeVehicleStatusResult failure(VehicleId vehicleId, String errorMessage) {
        return ChangeVehicleStatusResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousStatus(null)
                .newStatus(null)
                .errorMessage(errorMessage)
                .errorCode("STATUS_CHANGE_FAILED")
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .wasAutomaticChange(false)
                .changedBy(null)
                .reason(Optional.empty())
                .metadata(Map.of(
                        "isSuccessful", false,
                        "error", errorMessage
                ))
                .build();
    }

    public static ChangeVehicleStatusResult failure(VehicleId vehicleId,
                                                    String errorMessage,
                                                    String errorCode) {
        return ChangeVehicleStatusResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousStatus(null)
                .newStatus(null)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .wasAutomaticChange(false)
                .changedBy(null)
                .reason(Optional.empty())
                .metadata(Map.of(
                        "isSuccessful", false,
                        "error", errorMessage,
                        "errorCode", errorCode
                ))
                .build();
    }

    public static ChangeVehicleStatusResult invalidTransition(VehicleId vehicleId,
                                                              VehicleStatus currentStatus,
                                                              VehicleStatus requestedStatus,
                                                              String reason) {
        return ChangeVehicleStatusResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousStatus(currentStatus)
                .newStatus(requestedStatus)
                .errorMessage(String.format("Invalid status transition: %s -> %s (%s)",
                        currentStatus.name(), requestedStatus.name(), reason))
                .errorCode("INVALID_STATUS_TRANSITION")
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .wasAutomaticChange(false)
                .changedBy(null)
                .reason(Optional.of(reason))
                .metadata(Map.of(
                        "isSuccessful", false,
                        "currentStatus", currentStatus.name(),
                        "requestedStatus", requestedStatus.name(),
                        "transitionReason", reason,
                        "isInvalidTransition", true
                ))
                .build();
    }

    public static ChangeVehicleStatusResult vehicleNotFound(VehicleId vehicleId) {
        return ChangeVehicleStatusResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousStatus(null)
                .newStatus(null)
                .errorMessage(String.format("Vehicle not found: %s", vehicleId.getValue()))
                .errorCode("VEHICLE_NOT_FOUND")
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .wasAutomaticChange(false)
                .changedBy(null)
                .reason(Optional.empty())
                .metadata(Map.of(
                        "isSuccessful", false,
                        "vehicleNotFound", true
                ))
                .build();
    }

    public boolean isFailure() {
        return !success;
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    public boolean isCriticalChange() {
        if (!success || previousStatus == null || newStatus == null) {
            return false;
        }
        return isCriticalStatusChange(previousStatus, newStatus);
    }

    public boolean isEmergencyStatusChange() {
        if (!success || newStatus == null) {
            return false;
        }
        return newStatus == VehicleStatus.BREAKDOWN ;
    }

    public boolean isOperationalStatusChange() {
        if (!success || previousStatus == null || newStatus == null) {
            return false;
        }
        return (previousStatus == VehicleStatus.AT_DEPOT && newStatus == VehicleStatus.ACTIVE) ||
                (previousStatus == VehicleStatus.ACTIVE && newStatus == VehicleStatus.IN_ROUTE) ||
                (previousStatus == VehicleStatus.IN_ROUTE && newStatus == VehicleStatus.ACTIVE);
    }

    public String getStatusTransition() {
        if (previousStatus == null || newStatus == null) {
            return "N/A";
        }
        return previousStatus.name() + " → " + newStatus.name();
    }

    public String getResultSummary() {
        if (success) {
            return String.format("Status changed: %s (by: %s%s)",
                    getStatusTransition(),
                    changedBy != null ? changedBy : "unknown",
                    wasAutomaticChange ? ", automatic" : "");
        } else {
            return String.format("Status change failed: %s",
                    errorMessage != null ? errorMessage : "unknown error");
        }
    }



    @Override
    public boolean isSuccessful() {
        return success;
    }

    @Override
    public Duration getProcessingTime() {
        return processingTime;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean isCritical() {
        return isCriticalChange() || isEmergencyStatusChange();
    }

    @Override
    public boolean requiresNotification() {
        return isCritical() || isEmergencyStatusChange() || hasError();
    }

    private static boolean isCriticalStatusChange(VehicleStatus from, VehicleStatus to) {

        return to == VehicleStatus.BREAKDOWN ||
               to == VehicleStatus.RETIRED ||
                (from == VehicleStatus.MAINTENANCE && to == VehicleStatus.ACTIVE) ||
                (from == VehicleStatus.ACTIVE && to == VehicleStatus.MAINTENANCE);
    }



    public boolean isSlowProcessing() {
        return processingTime.toMillis() > 100;
    }

    public boolean isFastProcessing() {
        return processingTime.toMillis() <= 50;
    }

    @Override
    public PerformanceCategory getPerformanceCategory() {
        long millis = processingTime.toMillis();
        if (millis <= 25) return PerformanceCategory.FAST;
        if (millis <= 50) return PerformanceCategory.FAST;
        if (millis <= 100) return PerformanceCategory.NORMAL;
        return PerformanceCategory.SLOW;
    }


}