package tm.ugur.ugur_v3.domain.shared.specifications;

public final class NotSpecification<T> implements Specification<T> {

    private final Specification<T> specification;

    public NotSpecification(Specification<T> specification) {
        this.specification = specification;
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !specification.isSatisfiedBy(candidate);
    }

    @Override
    public String getDescription() {
        return String.format("NOT(%s)", specification.getDescription());
    }
}
