package tm.ugur.ugur_v3.application.shared.validation;

import java.util.Set;
import java.util.List;
import jakarta.validation.ConstraintViolation;
import org.springframework.stereotype.Service;
import tm.ugur.ugur_v3.application.shared.validation.rules.BusinessRule;

@Service
public interface ValidatorService {


    <T> void validate(T object);


    <T> Set<ConstraintViolation<T>> validateAndReturn(T object);


    <T> void validate(T object, Class<?>... groups);


    <T> void validateAll(List<T> objects);


    <T> void validateWithBusinessRules(T object, BusinessRule<T>... businessRules);

    <T> boolean isValid(T object);


    <T> List<String> getValidationMessages(T object);

}