package tm.ugur.ugur_v3.application.shared.validation.rules;

import tm.ugur.ugur_v3.application.shared.validation.ValidationResult;

import java.util.function.Function;

public final class CommonBusinessRules {


    public static <T> BusinessRule<T> notEmpty(String fieldName, Function<T, String> fieldExtractor) {
        return object -> {
            String value = fieldExtractor.apply(object);
            if (value == null || value.trim().isEmpty()) {
                return ValidationResult.invalid(fieldName + " cannot be null or empty", "REQUIRED_FIELD");
            }
            return ValidationResult.valid();
        };
    }

    public static <T> BusinessRule<T> numberInRange(String fieldName,
                                                    java.util.function.Function<T, Number> fieldExtractor,
                                                    double min, double max) {
        return object -> {
            Number value = fieldExtractor.apply(object);
            if (value == null) {
                return ValidationResult.invalid(fieldName + " cannot be null", "REQUIRED_FIELD");
            }

            double doubleValue = value.doubleValue();
            if (doubleValue < min || doubleValue > max) {
                return ValidationResult.invalid(
                        String.format("%s must be between %.2f and %.2f", fieldName, min, max),
                        "VALUE_OUT_OF_RANGE"
                );
            }
            return ValidationResult.valid();
        };
    }

    public static <T> BusinessRule<T> timestampNotInFuture(String fieldName,
                                                           java.util.function.Function<T, java.time.Instant> fieldExtractor) {
        return object -> {
            java.time.Instant value = fieldExtractor.apply(object);
            if (value == null) {
                return ValidationResult.invalid(fieldName + " cannot be null", "REQUIRED_FIELD");
            }

            if (value.isAfter(java.time.Instant.now())) {
                return ValidationResult.invalid(fieldName + " cannot be in the future", "INVALID_TIMESTAMP");
            }
            return ValidationResult.valid();
        };
    }

    public static <T> BusinessRule<T> timestampNotTooOld(String fieldName,
                                                         java.util.function.Function<T, java.time.Instant> fieldExtractor,
                                                         java.time.Duration maxAge) {
        return object -> {
            java.time.Instant value = fieldExtractor.apply(object);
            if (value == null) {
                return ValidationResult.invalid(fieldName + " cannot be null", "REQUIRED_FIELD");
            }

            java.time.Instant oldestAllowed = java.time.Instant.now().minus(maxAge);
            if (value.isBefore(oldestAllowed)) {
                return ValidationResult.invalid(
                        String.format("%s is too old (older than %s)", fieldName, maxAge),
                        "TIMESTAMP_TOO_OLD"
                );
            }
            return ValidationResult.valid();
        };
    }

    private CommonBusinessRules() {
        // Utility class
    }
}