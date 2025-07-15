package tm.ugur.ugur_v3.domain.shared.exceptions;

import lombok.Getter;

@Getter
public final class BusinessRuleViolationException extends DomainException {

    private final String ruleName;

    public BusinessRuleViolationException(String ruleName, String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
        this.ruleName = ruleName;
    }

    public BusinessRuleViolationException(String ruleName, String message, java.util.Map<String, Object> context) {
        super(message, "BUSINESS_RULE_VIOLATION", context);
        this.ruleName = ruleName;
    }

}