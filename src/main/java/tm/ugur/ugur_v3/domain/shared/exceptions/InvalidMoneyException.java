package tm.ugur.ugur_v3.domain.shared.exceptions;

public final class InvalidMoneyException extends DomainException {

    public InvalidMoneyException(String message) {
        super(message, "INVALID_MONEY");
    }

    public InvalidMoneyException(String message, java.util.Map<String, Object> context) {
        super(message, "INVALID_MONEY", context);
    }
}
