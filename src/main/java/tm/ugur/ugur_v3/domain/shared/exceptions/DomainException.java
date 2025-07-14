package tm.ugur.ugur_v3.domain.shared.exceptions;

public class DomainException extends RuntimeException {

    private final String errorCode;
    private final java.util.Map<String, Object> context;

    protected DomainException(String message, String errorCode) {
        this(message, errorCode, null, java.util.Collections.emptyMap());
    }

    protected DomainException(String message, String errorCode, Throwable cause) {
        this(message, errorCode, cause, java.util.Collections.emptyMap());
    }

    protected DomainException(String message, String errorCode, java.util.Map<String, Object> context) {
        this(message, errorCode, null, context);
    }

    protected DomainException(String message, String errorCode, Throwable cause, java.util.Map<String, Object> context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = java.util.Map.copyOf(context);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public java.util.Map<String, Object> getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]: %s", getClass().getSimpleName(), errorCode, getMessage());
    }
}
