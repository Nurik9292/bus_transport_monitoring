package tm.ugur.ugur_v3.application.shared.validation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ValidationException extends RuntimeException {

    private final ValidatorService.ValidationResult validationResult;
    private final String objectType;

    public ValidationException(ValidatorService.ValidationResult validationResult) {
        this(validationResult, "Unknown");
    }

    public ValidationException(ValidatorService.ValidationResult validationResult, String objectType) {
        super(buildErrorMessage(validationResult, objectType));
        this.validationResult = validationResult;
        this.objectType = objectType;
    }

    public ValidationException(String fieldName, String errorMessage, Object rejectedValue) {
        super(String.format("Validation failed for field '%s': %s (rejected value: %s)",
                fieldName, errorMessage, rejectedValue));
        this.validationResult = null;
        this.objectType = "Unknown";
    }

    public ValidatorService.ValidationResult getValidationResult() {
        return validationResult;
    }

    public String getObjectType() {
        return objectType;
    }

    public List<ValidatorService.ValidationError> getValidationErrors() {
        return validationResult != null ? validationResult.getErrors() : List.of();
    }

    public Map<String, List<ValidatorService.ValidationError>> getFieldErrors() {
        return validationResult != null ? validationResult.getFieldErrors() : Map.of();
    }

    public List<ValidatorService.ValidationError> getGlobalErrors() {
        return validationResult != null ? validationResult.getGlobalErrors() : List.of();
    }

    public boolean hasFieldErrors(String fieldName) {
        return getFieldErrors().containsKey(fieldName) &&
                !getFieldErrors().get(fieldName).isEmpty();
    }

    public int getErrorCount() {
        return validationResult != null ? validationResult.getErrors().size() : 1;
    }

    public ValidatorService.ValidationError getFirstError() {
        List<ValidatorService.ValidationError> errors = getValidationErrors();
        return errors.isEmpty() ? null : errors.getFirst();
    }

    public String toJson() {
        if (validationResult == null) {
            return "{\"error\": \"" + getMessage().replace("\"", "\\\"") + "\"}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"objectType\": \"").append(objectType).append("\",");
        json.append("\"errorCount\": ").append(getErrorCount()).append(",");
        json.append("\"errors\": [");

        List<ValidatorService.ValidationError> errors = getValidationErrors();
        for (int i = 0; i < errors.size(); i++) {
            ValidatorService.ValidationError error = errors.get(i);
            json.append("{");
            json.append("\"field\": \"").append(escapeJson(error.getFieldName())).append("\",");
            json.append("\"code\": \"").append(escapeJson(error.getErrorCode())).append("\",");
            json.append("\"message\": \"").append(escapeJson(error.getMessage())).append("\",");
            json.append("\"rejectedValue\": \"").append(escapeJson(String.valueOf(error.getRejectedValue()))).append("\"");
            json.append("}");
            if (i < errors.size() - 1) {
                json.append(",");
            }
        }

        json.append("]}");
        return json.toString();
    }

    public Map<String, Object> toMap() {
        if (validationResult == null) {
            return Map.of("error", getMessage());
        }

        return Map.of(
                "objectType", objectType,
                "errorCount", getErrorCount(),
                "fieldErrors", getFieldErrors().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .map(error -> Map.of(
                                                "code", error.getErrorCode(),
                                                "message", error.getMessage(),
                                                "rejectedValue", error.getRejectedValue()
                                        ))
                                        .collect(Collectors.toList())
                        )),
                "globalErrors", getGlobalErrors().stream()
                        .map(error -> Map.of(
                                "code", error.getErrorCode(),
                                "message", error.getMessage()
                        ))
                        .collect(Collectors.toList())
        );
    }

    public String getErrorSummary() {
        if (validationResult == null) {
            return getMessage();
        }

        int errorCount = getErrorCount();
        if (errorCount == 1) {
            ValidatorService.ValidationError error = getFirstError();
            return error.getFieldName() != null ?
                    String.format("Field '%s': %s", error.getFieldName(), error.getMessage()) :
                    error.getMessage();
        } else {
            return String.format("%d validation errors found for %s", errorCount, objectType);
        }
    }

    public boolean hasCriticalErrors() {
        return getValidationErrors().stream()
                .anyMatch(error -> error.getSeverity() == ValidatorService.ValidationSeverity.ERROR);
    }

    public List<ValidatorService.ValidationError> getErrorsBySeverity(ValidatorService.ValidationSeverity severity) {
        return getValidationErrors().stream()
                .filter(error -> error.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    public ValidationException combine(ValidationException other) {
        if (this.validationResult == null || other.validationResult == null) {
            throw new IllegalArgumentException("Cannot combine exceptions without validation results");
        }

        ValidatorService.ValidationResult combinedResult = this.validationResult.combine(other.validationResult);
        return new ValidationException(combinedResult, this.objectType + " & " + other.objectType);
    }

    public static ValidationException missingRequiredField(String fieldName) {
        return new ValidationException(fieldName, "Field is required", null);
    }

    public static ValidationException invalidFormat(String fieldName, Object value, String expectedFormat) {
        String message = String.format("Invalid format. Expected: %s", expectedFormat);
        return new ValidationException(fieldName, message, value);
    }

    public static ValidationException outOfRange(String fieldName, Object value, Object minValue, Object maxValue) {
        String message = String.format("Value must be between %s and %s", minValue, maxValue);
        return new ValidationException(fieldName, message, value);
    }

    public static ValidationException duplicateValue(String fieldName, Object value) {
        return new ValidationException(fieldName, "Value already exists", value);
    }

    public static ValidationException businessRuleViolation(String businessRule, String description) {
        return new ValidationException(null,
                String.format("Business rule '%s' violated: %s", businessRule, description), null);
    }

    private static String buildErrorMessage(ValidatorService.ValidationResult validationResult, String objectType) {
        if (validationResult == null || validationResult.isValid()) {
            return "Validation passed";
        }

        List<ValidatorService.ValidationError> errors = validationResult.getErrors();
        if (errors.isEmpty()) {
            return String.format("Validation failed for %s (no specific errors)", objectType);
        }

        if (errors.size() == 1) {
            ValidatorService.ValidationError error = errors.getFirst();
            String fieldPart = error.getFieldName() != null ?
                    String.format(" in field '%s'", error.getFieldName()) : "";
            return String.format("Validation failed for %s%s: %s", objectType, fieldPart, error.getMessage());
        }

        return String.format("Validation failed for %s with %d errors: %s",
                objectType,
                errors.size(),
                errors.stream()
                        .limit(3)
                        .map(ValidatorService.ValidationError::getMessage)
                        .collect(Collectors.joining("; ")));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // Для validation errors нам обычно не нужен полный stack trace
        // Это значительно улучшает performance при создании exceptions
        return this;
    }

    public static class Common {

        public static ValidationException invalidGpsCoordinates(double latitude, double longitude) {
            if (latitude < -90 || latitude > 90) {
                return invalidFormat("latitude", latitude, "value between -90 and 90");
            }
            if (longitude < -180 || longitude > 180) {
                return invalidFormat("longitude", longitude, "value between -180 and 180");
            }
            return new ValidationException("coordinates", "Invalid GPS coordinates",
                    String.format("lat: %f, lng: %f", latitude, longitude));
        }

        public static ValidationException invalidVehicleId(String vehicleId) {
            return invalidFormat("vehicleId", vehicleId, "alphanumeric string of 8-16 characters");
        }

        public static ValidationException invalidRouteId(String routeId) {
            return invalidFormat("routeId", routeId, "alphanumeric string starting with 'R'");
        }

        public static ValidationException invalidUserId(String userId) {
            return invalidFormat("userId", userId, "valid user identifier");
        }

        public static ValidationException invalidTimestamp(Object timestamp) {
            return invalidFormat("timestamp", timestamp, "ISO-8601 timestamp");
        }

        public static ValidationException invalidSpeed(double speed) {
            if (speed < 0) {
                return new ValidationException("speed", "Speed cannot be negative", speed);
            }
            if (speed > 300) {
                return outOfRange("speed", speed, 0, 300);
            }
            return new ValidationException("speed", "Invalid speed value", speed);
        }

        public static ValidationException invalidEta(Object eta) {
            return invalidFormat("eta", eta, "positive duration or future timestamp");
        }

        public static ValidationException rateLimitExceeded(String operation, int maxRequests) {
            return businessRuleViolation("rate_limit",
                    String.format("Too many %s requests. Maximum %d requests allowed.", operation, maxRequests));
        }

        public static ValidationException insufficientPermissions(String operation) {
            return businessRuleViolation("permissions",
                    String.format("Insufficient permissions to perform %s", operation));
        }
    }
}