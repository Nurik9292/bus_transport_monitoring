package tm.ugur.ugur_v3.application.vehicleManagement.validation;

import tm.ugur.ugur_v3.application.shared.validation.ValidationResult;
import tm.ugur.ugur_v3.application.shared.validation.rules.BusinessRule;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.UpdateVehicleLocationCommand;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.ChangeVehicleStatusCommand;

public final class VehicleBusinessRules {


    public static BusinessRule<UpdateVehicleLocationCommand> gpsDataMustBeRecent(java.time.Duration maxAge) {
        return command -> {
            if (!command.isGpsDataRecent(maxAge)) {
                return ValidationResult.invalid(
                        String.format("GPS data is too old. Age: %d minutes, max allowed: %d minutes",
                                java.time.Duration.between(command.timestamp(), java.time.Instant.now()).toMinutes(),
                                maxAge.toMinutes()),
                        "GPS_DATA_TOO_OLD"
                );
            }
            return ValidationResult.valid();
        };
    }

    public static BusinessRule<UpdateVehicleLocationCommand> gpsAccuracyMustBeAcceptable(double maxAccuracyMeters) {
        return command -> {
            double accuracy = command.location().getAccuracy();
            if (accuracy > maxAccuracyMeters) {
                return ValidationResult.invalid(
                        String.format("GPS accuracy too low: %.1fm (max allowed: %.1fm)",
                                accuracy, maxAccuracyMeters),
                        "GPS_ACCURACY_TOO_LOW"
                );
            }
            return ValidationResult.valid();
        };
    }

    public static BusinessRule<UpdateVehicleLocationCommand> speedMustBeReasonable() {
        return command -> {
            if (command.speedKmh() != null) {
                double speed = command.speedKmh();

                if (speed > 250.0) {
                    return ValidationResult.invalid(
                            String.format("Speed too high: %.1f km/h (max reasonable: 250 km/h)", speed),
                            "SPEED_TOO_HIGH"
                    );
                }

                if (speed < 0) {
                    return ValidationResult.invalid(
                            String.format("Speed cannot be negative: %.1f km/h", speed),
                            "SPEED_NEGATIVE"
                    );
                }
            }
            return ValidationResult.valid();
        };
    }

    public static BusinessRule<UpdateVehicleLocationCommand> locationMustBeReasonable() {
        return command -> {
            double lat = command.location().getLatitude();
            double lng = command.location().getLongitude();

            if (Math.abs(lat) < 0.001 && Math.abs(lng) < 0.001) {
                return ValidationResult.invalid(
                        "GPS coordinates at null island (0,0), likely GPS error",
                        "COORDINATES_NULL_ISLAND"
                );
            }

            if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
                return ValidationResult.invalid(
                        String.format("GPS coordinates out of valid range: lat=%.6f, lng=%.6f", lat, lng),
                        "COORDINATES_OUT_OF_RANGE"
                );
            }

            return ValidationResult.valid();
        };
    }


    public static BusinessRule<ChangeVehicleStatusCommand> reasonMustBeDescriptive() {
        return command -> {
            String reason = command.reason().trim();

            if (reason.length() < 5) {
                return ValidationResult.invalid(
                        "Status change reason must be at least 5 characters long",
                        "REASON_TOO_SHORT"
                );
            }

            String lowerReason = reason.toLowerCase();
            String[] genericReasons = {"test", "update", "change", "fix", "n/a", "none", "unknown", "temp"};

            for (String generic : genericReasons) {
                if (lowerReason.equals(generic) || lowerReason.equals(generic + ".")) {
                    return ValidationResult.invalid(
                            String.format("Status change reason must be specific. '%s' is too generic", reason),
                            "REASON_TOO_GENERIC"
                    );
                }
            }

            return ValidationResult.valid();
        };
    }

    public static BusinessRule<ChangeVehicleStatusCommand> criticalChangesMustBeAuthorized() {
        return command -> {
            if (command.isCriticalStatusChange() && !command.isAutomaticChange()) {
                String changedBy = command.changedBy().toLowerCase();

                if (!changedBy.contains("supervisor") &&
                        !changedBy.contains("manager") &&
                        !changedBy.contains("admin")) {

                    return ValidationResult.invalid(
                            "Critical status changes require supervisor authorization",
                            "INSUFFICIENT_AUTHORIZATION"
                    );
                }
            }
            return ValidationResult.valid();
        };
    }

    public static BusinessRule<ChangeVehicleStatusCommand> timestampMustBeRecent() {
        return command -> {
            java.time.Duration age = java.time.Duration.between(command.timestamp(), java.time.Instant.now());

            // Status changes older than 1hour are suspicious
            if (age.toHours() > 1) {
                return ValidationResult.invalid(
                        String.format("Status change timestamp is too old: %d hours", age.toHours()),
                        "TIMESTAMP_TOO_OLD"
                );
            }

            return ValidationResult.valid();
        };
    }


    public static <T> BusinessRule<T> vehicleIdMustBeValid(java.util.function.Function<T, tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId> idExtractor) {
        return object -> {
            tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId vehicleId = idExtractor.apply(object);

            if (vehicleId == null) {
                return ValidationResult.invalid("Vehicle ID cannot be null", "VEHICLE_ID_NULL");
            }

            String idValue = vehicleId.getValue();
            if (idValue == null || idValue.trim().isEmpty()) {
                return ValidationResult.invalid("Vehicle ID cannot be empty", "VEHICLE_ID_EMPTY");
            }

            if (idValue.length() < 3 || idValue.length() > 50) {
                return ValidationResult.invalid(
                        String.format("Vehicle ID length invalid: %s (must be 3-50 characters)", idValue),
                        "VEHICLE_ID_INVALID_LENGTH"
                );
            }

            return ValidationResult.valid();
        };
    }

    public static <T> BusinessRule<T> timestampMustBeReasonable(java.util.function.Function<T, java.time.Instant> timestampExtractor) {
        return object -> {
            java.time.Instant timestamp = timestampExtractor.apply(object);

            if (timestamp == null) {
                return ValidationResult.invalid("Timestamp cannot be null", "TIMESTAMP_NULL");
            }

            java.time.Instant now = java.time.Instant.now();

            if (timestamp.isAfter(now.plusSeconds(300))) {
                return ValidationResult.invalid(
                        "Timestamp cannot be more than 5 minutes in the future",
                        "TIMESTAMP_FUTURE"
                );
            }

            if (timestamp.isBefore(now.minusSeconds(86400))) {
                return ValidationResult.invalid(
                        "Timestamp cannot be older than 24 hours",
                        "TIMESTAMP_TOO_OLD"
                );
            }

            return ValidationResult.valid();
        };
    }


    public static BusinessRule<UpdateVehicleLocationCommand>[] getStandardGpsValidationRules() {
        return new BusinessRule[]{
                gpsDataMustBeRecent(java.time.Duration.ofMinutes(10)),
                gpsAccuracyMustBeAcceptable(100.0),
                speedMustBeReasonable(),
                locationMustBeReasonable(),
                timestampMustBeReasonable(UpdateVehicleLocationCommand::timestamp),
                vehicleIdMustBeValid(UpdateVehicleLocationCommand::vehicleId)
        };
    }

    public static BusinessRule<ChangeVehicleStatusCommand>[] getStandardStatusChangeValidationRules() {
        return new BusinessRule[]{
                reasonMustBeDescriptive(),
                criticalChangesMustBeAuthorized(),
                timestampMustBeRecent(),
                timestampMustBeReasonable(ChangeVehicleStatusCommand::timestamp),
                vehicleIdMustBeValid(ChangeVehicleStatusCommand::vehicleId)
        };
    }

    private VehicleBusinessRules() {
        // Utility class - no instances
    }
}