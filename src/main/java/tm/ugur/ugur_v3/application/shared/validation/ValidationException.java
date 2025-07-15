package tm.ugur.ugur_v3.application.shared.validation;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {

    private final String errorCode;
    private final List<String> violations;
    private final Map<String, Object> context;

    public ValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.violations = List.of(message);
        this.context = Map.of();
    }

    public ValidationException(List<String> violations) {
        super(buildMessage(violations));
        this.errorCode = "VALIDATION_ERROR";
        this.violations = List.copyOf(violations);
        this.context = Map.of();
    }

    public ValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.violations = List.of(message);
        this.context = Map.of();
    }

    public ValidationException(String message, String errorCode, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.violations = List.of(message);
        this.context = Map.copyOf(context);
    }

    public ValidationException(List<String> violations, String errorCode) {
        super(buildMessage(violations));
        this.errorCode = errorCode;
        this.violations = List.copyOf(violations);
        this.context = Map.of();
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "VALIDATION_ERROR";
        this.violations = List.of(message);
        this.context = Map.of();
    }

    private static String buildMessage(List<String> violations) {
        if (violations == null || violations.isEmpty()) {
            return "Validation failed";
        }
        if (violations.size() == 1) {
            return violations.getFirst();
        }
        return "Multiple validation errors: " + String.join("; ", violations);
    }

    public boolean hasMultipleViolations() {
        return violations.size() > 1;
    }

    public int getViolationCount() {
        return violations.size();
    }

    @Override
    public String toString() {
        return String.format("%s[%s]: %s (%d violations)",
                getClass().getSimpleName(),
                errorCode,
                getMessage(),
                violations.size());
    }
}