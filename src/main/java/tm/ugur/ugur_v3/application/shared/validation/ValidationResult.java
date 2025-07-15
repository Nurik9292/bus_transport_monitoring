package tm.ugur.ugur_v3.application.shared.validation;

public  record ValidationResult(
        boolean isValid,
        String errorMessage,
        String errorCode
) {
    public static ValidationResult valid() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage, "BUSINESS_RULE_VIOLATION");
    }

    public static ValidationResult invalid(String errorMessage, String errorCode) {
        return new ValidationResult(false, errorMessage, errorCode);
    }
}