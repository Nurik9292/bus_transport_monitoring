package tm.ugur.ugur_v3.application.shared.validation.rules;

import tm.ugur.ugur_v3.application.shared.validation.ValidationResult;

@FunctionalInterface
public interface BusinessRule<T> {
    ValidationResult validate(T object);
}