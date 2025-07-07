package tm.ugur.ugur_v3.domain.shared.valueobjects;

public record Speed(double kmh) {

    public Speed {
        if (kmh < 0.0) {
            throw new IllegalArgumentException("Speed cannot be negative");
        }
        if (kmh > 300.0) {
            throw new IllegalArgumentException("Speed too high for public transport: " + kmh);
        }

    }

    public double getMps() {
        return kmh / 3.6;
    }

    public boolean isStationary() {
        return kmh < 1.0;
    }

    public boolean isMoving() {
        return !isStationary();
    }

    public boolean isOverSpeedLimit(double speedLimitKmh) {
        return kmh > speedLimitKmh;
    }

    @Override
    public String toString() {
        return String.format("%.1f km/h", kmh);
    }
}
