package tm.ugur.ugur_v3.domain.shared.specifications;

import java.time.LocalDateTime;

public class VehicleActiveSpecification implements Specification<LocalDateTime> {
    private final long maxInactiveMinutes;

    public VehicleActiveSpecification(long maxInactiveMinutes) {
        this.maxInactiveMinutes = maxInactiveMinutes;
    }

    @Override
    public boolean isSatisfiedBy(java.time.LocalDateTime lastUpdate) {
        if (lastUpdate == null) return false;

        java.time.LocalDateTime threshold = java.time.LocalDateTime.now()
                .minusMinutes(maxInactiveMinutes);

        return lastUpdate.isAfter(threshold);
    }
}