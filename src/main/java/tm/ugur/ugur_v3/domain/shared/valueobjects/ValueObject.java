package tm.ugur.ugur_v3.domain.shared.valueobjects;

import java.util.Arrays;

public abstract class ValueObject {

    private transient volatile int hashCode = 0;

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ValueObject other = (ValueObject) obj;
        Object[] thisComponents = getEqualityComponents();
        Object[] otherComponents = other.getEqualityComponents();

        return Arrays.deepEquals(thisComponents, otherComponents);
    }


    @Override
    public final int hashCode() {
        if (hashCode == 0) {
            hashCode = Arrays.deepHashCode(getEqualityComponents());
            // Если случайно получили 0, устанавливаем 1 для избежания повторных вычислений
            if (hashCode == 0) {
                hashCode = 1;
            }
        }
        return hashCode;
    }


    protected abstract Object[] getEqualityComponents();


    protected void validate() {
        // По умолчанию нет валидации - переопределяется в подклассах
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + Arrays.toString(getEqualityComponents());
    }
}
