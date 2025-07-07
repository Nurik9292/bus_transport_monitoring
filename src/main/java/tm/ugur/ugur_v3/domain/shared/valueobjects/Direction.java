package tm.ugur.ugur_v3.domain.shared.valueobjects;

public record Direction(int degrees) {

    public Direction {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalArgumentException("Direction must be between 0 and 360 degrees");
        }
    }

    public String getCompass() {
        if (degrees >= 337.5 || degrees < 22.5) return "С";     // North
        if (degrees >= 22.5 && degrees < 67.5) return "СВ";     // Northeast
        if (degrees >= 67.5 && degrees < 112.5) return "В";     // East
        if (degrees >= 112.5 && degrees < 157.5) return "ЮВ";   // Southeast
        if (degrees >= 157.5 && degrees < 202.5) return "Ю";    // South
        if (degrees >= 202.5 && degrees < 247.5) return "ЮЗ";   // Southwest
        if (degrees >= 247.5 && degrees < 292.5) return "З";    // West
        return "СЗ";                                             // Northwest
    }

    public boolean isOppositeDirection(Direction other) {
        int diff = Math.abs(this.degrees - other.degrees);
        return diff >= 160 && diff <= 200; // 180° ± 20° tolerance
    }

    public double angleDifference(Direction other) {
        double diff = Math.abs(this.degrees - other.degrees);
        return Math.min(diff, 360 - diff);
    }

    @Override
    public String toString() {
        return degrees + "° " + getCompass();
    }
}
