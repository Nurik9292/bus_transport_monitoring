package tm.ugur.ugur_v3.domain.shared.specifications;

import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;

public class ValidGpsDataSpecification implements Specification<GeoCoordinate> {
    private final double maxAccuracyMeters;

    public ValidGpsDataSpecification(double maxAccuracyMeters) {
        this.maxAccuracyMeters = maxAccuracyMeters;
    }

    @Override
    public boolean isSatisfiedBy(GeoCoordinate coordinate) {
        return coordinate.isValidForTracking() &&
                (coordinate.getAccuracy() == null || coordinate.getAccuracy() <= maxAccuracyMeters);
    }
}
