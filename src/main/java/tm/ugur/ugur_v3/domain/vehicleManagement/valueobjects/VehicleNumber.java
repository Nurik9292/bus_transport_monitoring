package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

public record VehicleNumber(String value) {

    public VehicleNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle number cannot be empty");
        }

        String normalized = value.trim().toUpperCase();
        if (!isValidFormat(normalized)) {
            throw new IllegalArgumentException("Invalid vehicle number format: " + value);
        }

        this.value = normalized;
    }

    private boolean isValidFormat(String number) {
        return number.matches("^[A-Z0-9-]{3,15}$");
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String value() {
        return value;
    }
}
