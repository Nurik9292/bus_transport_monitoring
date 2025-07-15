package tm.ugur.ugur_v3.application.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import lombok.extern.slf4j.Slf4j;
import tm.ugur.ugur_v3.application.shared.validation.rules.BusinessRule;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;


@Slf4j
public class DefaultValidatorService implements ValidatorService {

    private final Validator validator;

    public DefaultValidatorService() {
        try(ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Override
    public <T> void validate(T object) {
        if (object == null) {
            throw new ValidationException("Validation object cannot be null");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);

        if (!violations.isEmpty()) {
            log.warn("Validation failed for {}: {}", object.getClass().getSimpleName(), violations);
            throw new ValidationException(String.valueOf(violations));
        }

        log.debug("Validation passed for {}", object.getClass().getSimpleName());
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateAndReturn(T object) {
        if (object == null) {
            return Set.of();
        }

        return validator.validate(object);
    }

    @Override
    public <T> void validate(T object, Class<?>... groups) {
        if (object == null) {
            throw new ValidationException("Validation object cannot be null");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object, groups);

        if (!violations.isEmpty()) {
            log.warn("Group validation failed for {}: {}", object.getClass().getSimpleName(), violations);
            throw new ValidationException(String.valueOf(violations));
        }

        log.debug("Group validation passed for {}", object.getClass().getSimpleName());
    }

    @Override
    public <T> void validateAll(List<T> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }

        List<ValidationException> exceptions = new ArrayList<>();

        for (T object : objects) {
            try {
                validate(object);
            } catch (ValidationException e) {
                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            String combinedMessage = exceptions.stream()
                    .map(ValidationException::getMessage)
                    .collect(java.util.stream.Collectors.joining("; "));
            throw new ValidationException("Batch validation failed: " + combinedMessage);
        }
    }

    @SafeVarargs
    @Override
    public final <T> void validateWithBusinessRules(T object, BusinessRule<T>... businessRules) {
        validate(object);

        List<String> businessRuleViolations = new ArrayList<>();

        for (BusinessRule<T> rule : businessRules) {
            ValidationResult result = rule.validate(object);
            if (!result.isValid()) {
                businessRuleViolations.add(result.errorMessage());
                log.warn("Business rule violation for {}: {}",
                        object.getClass().getSimpleName(), result.errorMessage());
            }
        }

        if (!businessRuleViolations.isEmpty()) {
            throw new ValidationException(businessRuleViolations);
        }

        log.debug("Business rule validation passed for {}", object.getClass().getSimpleName());
    }

    @Override
    public <T> boolean isValid(T object) {
        if (object == null) {
            return false;
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);
        return violations.isEmpty();
    }

    @Override
    public <T> List<String> getValidationMessages(T object) {
        if (object == null) {
            return List.of("Object cannot be null");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);

        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(java.util.stream.Collectors.toList());
    }
}
