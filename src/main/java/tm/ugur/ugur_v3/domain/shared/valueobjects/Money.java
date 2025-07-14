package tm.ugur.ugur_v3.domain.shared.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidMoneyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

@Getter
public final class Money extends ValueObject {

    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("TMT"); // Туркменский манат
    private static final int DECIMAL_PLACES = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(DECIMAL_PLACES, ROUNDING_MODE);
        this.currency = currency;
        validate();
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    @Override
    protected void validate() {
        if (amount == null) {
            throw new InvalidMoneyException("Amount cannot be null");
        }
        if (currency == null) {
            throw new InvalidMoneyException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMoneyException("Amount cannot be negative: " + amount);
        }
    }

    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        ensureSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMoneyException("Subtraction result cannot be negative");
        }
        return new Money(result, this.currency);
    }

    public Money multiply(double factor) {
        if (factor < 0) {
            throw new InvalidMoneyException("Multiplication factor cannot be negative");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public Money divide(double divisor) {
        if (divisor <= 0) {
            throw new InvalidMoneyException("Division by zero or negative number");
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), ROUNDING_MODE), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    private void ensureSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new InvalidMoneyException(
                    String.format("Currency mismatch: %s vs %s", this.currency, other.currency));
        }
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{amount, currency};
    }

    @Override
    public String toString() {
        return String.format("%.2f %s", amount.doubleValue(), currency.getCurrencyCode());
    }
}