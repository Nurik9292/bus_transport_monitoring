package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.util.Map;

public final class VehicleExceptions {



    public static InvalidLocationUpdateException invalidLocation(VehicleId vehicleId,
                                                                 double lat, double lng,
                                                                 String reason) {
        return new InvalidLocationUpdateException(
                String.format("Invalid location update: %s (lat=%.6f, lng=%.6f)", reason, lat, lng),
                vehicleId
        );
    }

    public static InvalidGpsDataException invalidGpsAccuracy(VehicleId vehicleId,
                                                             double lat, double lng,
                                                             double accuracy,
                                                             double maxAccuracy) {
        return new InvalidGpsDataException(
                String.format("GPS accuracy too low: %.1fm (max: %.1fm)", accuracy, maxAccuracy),
                vehicleId, lat, lng, accuracy
        );
    }

    public static InvalidLocationUpdateException locationJumpTooLarge(VehicleId vehicleId,
                                                                      double distanceKm,
                                                                      double maxAllowedKm) {
        return new InvalidLocationUpdateException(
                String.format("Location jump too large: %.2fkm (max: %.2fkm)", distanceKm, maxAllowedKm),
                vehicleId
        );
    }



    public static InvalidStatusChangeException invalidStatusTransition(VehicleId vehicleId,
                                                                       VehicleStatus from,
                                                                       VehicleStatus to,
                                                                       String reason) {
        return new InvalidStatusChangeException(
                String.format("Invalid status transition from %s to %s: %s", from.name(), to.name(), reason),
                vehicleId, from, to
        );
    }

    public static InvalidStatusChangeException statusChangeNotAuthorized(VehicleId vehicleId,
                                                                         VehicleStatus newStatus,
                                                                         String requiredRole) {
        return new InvalidStatusChangeException(
                String.format("Status change to %s requires %s authorization", newStatus.name(), requiredRole),
                vehicleId, null, newStatus
        );
    }



    public static BusinessRuleViolationException routeAssignmentNotAllowed(VehicleId vehicleId,
                                                                           VehicleStatus currentStatus) {
        return new BusinessRuleViolationException(
                "ROUTE_ASSIGNMENT_NOT_ALLOWED",
                String.format("Vehicle %s in status %s cannot be assigned to route",
                        vehicleId.getValue(), currentStatus.name())
        );
    }

    public static BusinessRuleViolationException vehicleAlreadyAssigned(VehicleId vehicleId,
                                                                        String currentRouteId) {
        return new BusinessRuleViolationException(
                "VEHICLE_ALREADY_ASSIGNED",
                String.format("Vehicle %s is already assigned to route %s",
                        vehicleId.getValue(), currentRouteId)
        );
    }

    public static BusinessRuleViolationException maintenanceRequired(VehicleId vehicleId, String reason) {
        return new BusinessRuleViolationException(
                "MAINTENANCE_REQUIRED",
                String.format("Vehicle %s requires maintenance: %s", vehicleId.getValue(), reason)
        );
    }

    public static BusinessRuleViolationException capacityExceeded(VehicleId vehicleId,
                                                                  int currentPassengers,
                                                                  int maxCapacity) {
        return new BusinessRuleViolationException(
                "CAPACITY_EXCEEDED",
                String.format("Vehicle %s capacity exceeded: %d passengers (max: %d)",
                        vehicleId.getValue(), currentPassengers, maxCapacity)
        );
    }

    public static BusinessRuleViolationException invalidSpeedForStatus(VehicleId vehicleId,
                                                                       double speedKmh,
                                                                       VehicleStatus status) {
        return new BusinessRuleViolationException(
                "INVALID_SPEED_FOR_STATUS",
                String.format("Vehicle %s speed %.1f km/h is invalid for status %s",
                        vehicleId.getValue(), speedKmh, status.name())
        );
    }

    public static BusinessRuleViolationException emergencyProtocolViolation(VehicleId vehicleId,
                                                                            String protocol) {
        return new BusinessRuleViolationException(
                "EMERGENCY_PROTOCOL_VIOLATION",
                String.format("Vehicle %s violated emergency protocol: %s",
                        vehicleId.getValue(), protocol)
        );
    }



    public static VehicleOperationTimeoutException operationTimeout(VehicleId vehicleId,
                                                                    String operation,
                                                                    java.time.Duration timeout) {
        return new VehicleOperationTimeoutException(operation, timeout, vehicleId);
    }

    public static VehicleOperationTimeoutException gpsUpdateTimeout(VehicleId vehicleId,
                                                                    java.time.Duration timeout) {
        return new VehicleOperationTimeoutException("GPS_UPDATE", timeout, vehicleId);
    }

    public static VehicleOperationTimeoutException statusChangeTimeout(VehicleId vehicleId,
                                                                       java.time.Duration timeout) {
        return new VehicleOperationTimeoutException("STATUS_CHANGE", timeout, vehicleId);
    }



    public static BusinessRuleViolationException businessRuleViolation(String ruleName,
                                                                       String message,
                                                                       VehicleId vehicleId,
                                                                       Map<String, Object> context) {

        String enhancedMessage = String.format("Vehicle %s: %s", vehicleId.getValue(), message);


        Map<String, Object> fullContext = new java.util.HashMap<>(context);
        fullContext.put("vehicleId", vehicleId.getValue());
        fullContext.put("errorCategory", "VEHICLE_BUSINESS_RULE");

        return new BusinessRuleViolationException(ruleName, enhancedMessage, Map.copyOf(fullContext));
    }

    public static BusinessRuleViolationException safetyCriticalViolation(VehicleId vehicleId,
                                                                         String safetyRule,
                                                                         String details) {
        return new BusinessRuleViolationException(
                "SAFETY_CRITICAL_VIOLATION",
                String.format("SAFETY CRITICAL: Vehicle %s violated %s - %s",
                        vehicleId.getValue(), safetyRule, details),
                Map.of(
                        "vehicleId", vehicleId.getValue(),
                        "safetyRule", safetyRule,
                        "severity", "CRITICAL",
                        "requiresImmediate", true
                )
        );
    }

    public static BusinessRuleViolationException complianceViolation(VehicleId vehicleId,
                                                                     String regulation,
                                                                     String violation) {
        return new BusinessRuleViolationException(
                "COMPLIANCE_VIOLATION",
                String.format("Vehicle %s compliance violation: %s (%s)",
                        vehicleId.getValue(), violation, regulation),
                Map.of(
                        "vehicleId", vehicleId.getValue(),
                        "regulation", regulation,
                        "violationType", "COMPLIANCE",
                        "requiresReporting", true
                )
        );
    }



    public static boolean isSafetyCritical(Throwable exception) {
        if (exception instanceof BusinessRuleViolationException brve) {
            return "SAFETY_CRITICAL_VIOLATION".equals(brve.getRuleName()) ||
                    brve.getContext().containsKey("severity") &&
                            "CRITICAL".equals(brve.getContext().get("severity"));
        }
        return false;
    }

    public static boolean requiresImmediateAttention(Throwable exception) {
        if (exception instanceof VehicleManagementException vme) {
            return "PERFORMANCE_ERROR".equals(vme.getErrorCategory()) ||
                    "SAFETY_ERROR".equals(vme.getErrorCategory());
        }

        return isSafetyCritical(exception);
    }

    private VehicleExceptions() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}