package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

public record IMEI(String value) {

    public IMEI(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("IMEI cannot be empty");
        }

        String normalized = value.trim();
        if (!isValidIMEI(normalized)) {
            throw new IllegalArgumentException("Invalid IMEI format: " + value);
        }

        this.value = normalized;
    }

    private boolean isValidIMEI(String imei) {
        return imei.matches("^\\d{15}$") && isValidLuhnChecksum(imei);
    }

    private boolean isValidLuhnChecksum(String imei) {
        int sum = 0;
        boolean alternate = false;

        for (int i = imei.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(imei.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    @Override
    public String toString() {
        return value;
    }
}
