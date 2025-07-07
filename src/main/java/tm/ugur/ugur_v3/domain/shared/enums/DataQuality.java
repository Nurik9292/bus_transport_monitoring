package tm.ugur.ugur_v3.domain.shared.enums;

public enum DataQuality {

    EXCELLENT(0, 5),
    GOOD(5, 15),
    FAIR(15, 50),
    POOR(50, 100),
    UNUSABLE(100, Double.MAX_VALUE);

    private final double minAccuracy;
    private final double maxAccuracy;

    DataQuality(double minAccuracy, double maxAccuracy) {
        this.minAccuracy = minAccuracy;
        this.maxAccuracy = maxAccuracy;
    }

    public static DataQuality fromAccuracy(Double accuracy) {
        if (accuracy == null) return FAIR;

        for (DataQuality quality : values()) {
            if (accuracy >= quality.minAccuracy && accuracy < quality.maxAccuracy) {
                return quality;
            }
        }
        return UNUSABLE;
    }

    public boolean isAcceptable() {
        return this != UNUSABLE;
    }
}
