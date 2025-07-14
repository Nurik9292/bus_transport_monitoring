package tm.ugur.ugur_v3.application.shared.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ValidatorService {

    <T> ValidationResult validate(T object, ValidationGroup validationGroup);

    <T> ValidationResult validate(T object, ValidationContext context);

    <T> CompletableFuture<ValidationResult> validateAsync(T object, ValidationGroup validationGroup);

    <T> Map<T, ValidationResult> validateBatch(List<T> objects, ValidationGroup validationGroup);

    <T> ValidationResult validateFields(T object, Set<String> fieldNames, ValidationGroup validationGroup);

    <T> ValidationBuilder<T> forObject(T object);

    <T> void registerRule(ValidationRule<T> rule);

    void unregisterRule(String ruleId);

    List<ValidationRule<?>> getActiveRules(ValidationGroup validationGroup);

    boolean isValid(Object value, String fieldName, ConstraintType constraintType);

    String sanitize(String value, SanitizationType sanitizationType);

    ValidationStatistics getStatistics();

    void resetStatistics();

    interface ValidationBuilder<T> {

        ValidationBuilder<T> withGroup(ValidationGroup group);

        ValidationBuilder<T> withRule(ValidationRule<T> rule);

        ValidationBuilder<T> withContext(ValidationContext context);

        ValidationBuilder<T> enableAsync();

        ValidationBuilder<T> fieldsOnly(Set<String> fieldNames);

        ValidationBuilder<T> failFast();

        ValidationBuilder<T> withDebug();

        ValidationResult validate();

        CompletableFuture<ValidationResult> validateAsync();
    }

    interface ValidationResult {
        boolean isValid();

        List<ValidationError> getErrors();

        List<ValidationWarning> getWarnings();

        Map<String, List<ValidationError>> getFieldErrors();

        List<ValidationError> getGlobalErrors();

        int getRulesChecked();

        java.time.Duration getExecutionTime();

        Map<String, Object> getDebugInfo();

        ValidationResult combine(ValidationResult other);

        ValidationResult filterBySeverity(ValidationSeverity minSeverity);
    }

    interface ValidationError {
        String getErrorCode();

        String getMessage();

        String getFieldName();

        Object getRejectedValue();

        ValidationSeverity getSeverity();

        Map<String, Object> getParameters();

        String getRuleId();
    }

    interface ValidationWarning {
        String getWarningCode();

        String getMessage();

        String getFieldName();

        Object getValue();

        String getRecommendation();
    }

    interface ValidationRule<T> {
        String getRuleId();

        String getRuleName();

        String getDescription();

        Set<ValidationGroup> getApplicableGroups();

        int getPriority();

        boolean supportsAsync();

        ValidationResult validate(T object, ValidationContext context);

        CompletableFuture<ValidationResult> validateAsync(T object, ValidationContext context);

        boolean isApplicable(T object, ValidationContext context);
    }

    interface ValidationContext {
        ValidationGroup getValidationGroup();

        Map<String, Object> getProperties();

        <T> T getProperty(String key, Class<T> type);

        void setProperty(String key, Object value);

        String getCurrentUser();

        java.time.Instant getValidationTime();

        boolean isUpdate();

        String getExistingObjectId();
    }

    enum ValidationGroup {
        BASIC,

        CREATE,

        UPDATE,

        DELETE,

        API,

        BATCH,

        ADMIN,

        FULL
    }

    enum ValidationSeverity {
        ERROR,

        WARNING,

        INFO
    }

    enum ConstraintType {
        NOT_NULL,
        NOT_EMPTY,
        NOT_BLANK,
        MIN_LENGTH,
        MAX_LENGTH,
        PATTERN,
        EMAIL,
        URL,
        NUMERIC,
        POSITIVE,
        NEGATIVE,
        RANGE,
        GPS_COORDINATE,
        VEHICLE_ID,
        ROUTE_ID,
        USER_ID
    }

    enum SanitizationType {
        HTML,

        SQL,

        XSS,

        ALPHANUMERIC_ONLY,

        SEARCH_QUERY,

        FILENAME,

        URL
    }

    interface ValidationStatistics {
        long getTotalValidations();

        long getSuccessfulValidations();

        long getFailedValidations();

        double getSuccessRate();

        java.time.Duration getAverageValidationTime();

        Map<ValidationGroup, Long> getValidationsByGroup();

        Map<String, Long> getMostCommonErrors();

        Map<String, Long> getValidationsByRule();

        java.time.Duration getCollectionPeriod();

        java.time.Instant getLastResetTime();
    }
}