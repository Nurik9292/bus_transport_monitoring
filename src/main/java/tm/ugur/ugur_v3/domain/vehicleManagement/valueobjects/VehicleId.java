package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;
import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidEntityIdException;

import java.util.regex.Pattern;

public final class VehicleId extends EntityId {

    private static final Pattern VEHICLE_ID_PATTERN = Pattern.compile("^V\\d{8,12}$");

    private static final int MIN_LENGTH = 9;

    private static final int MAX_LENGTH = 13;

    private VehicleId(String value) {
        super(value);
    }

    public static VehicleId of(String value) {
        return new VehicleId(value);
    }

    public static VehicleId of(long numericId) {
        if (numericId <= 0) {
            throw new InvalidEntityIdException("Numeric vehicle ID must be positive: " + numericId);
        }

        String formattedId = String.format("V%08d", numericId);
        return new VehicleId(formattedId);
    }

    public static VehicleId generateRandom() {
        long randomId = (long) (Math.random() * 99999999L) + 1;
        return of(randomId);
    }

    public static VehicleId fromExternalApi(String externalId) {
        if (externalId == null || externalId.trim().isEmpty()) {
            throw new InvalidEntityIdException("External vehicle ID cannot be null or empty");
        }

        String trimmed = externalId.trim();

        if (VEHICLE_ID_PATTERN.matcher(trimmed).matches()) {
            return new VehicleId(trimmed);
        }

        if (trimmed.matches("\\d{8,12}")) {
            return new VehicleId("V" + trimmed);
        }


        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.length() >= 8 && digitsOnly.length() <= 12) {
            return new VehicleId("V" + digitsOnly);
        }

        throw new InvalidEntityIdException(
                "Cannot parse vehicle ID from external format: " + externalId);
    }

    @Override
    protected void validate() {
        super.validate();

        String value = getValue();

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidEntityIdException(
                    String.format("Vehicle ID length must be between %d and %d characters: '%s' (%d)",
                            MIN_LENGTH, MAX_LENGTH, value, value.length()));
        }

        if (!VEHICLE_ID_PATTERN.matcher(value).matches()) {
            throw new InvalidEntityIdException(
                    "Vehicle ID must match format 'V' followed by 8-12 digits: " + value);
        }
    }

    public long getNumericPart() {
        String numericPart = getValue().substring(1); // Убираем "V"
        return Long.parseLong(numericPart);
    }

    public boolean isTestVehicle() {
        return getValue().startsWith("V99");
    }

    public boolean isCompatibleWithProvider(String providerName) {
        return switch (providerName.toLowerCase()) {
            case "tugdk" ->
                // TUGDK API поддерживает любые форматы
                    true;
            case "ayauk" ->
                // AYAUK предпочитает короткие ID
                    getValue().length() <= 10;
            default -> true;
        };
    }

    public String toDisplayFormat() {
        String numeric = getValue().substring(1);
        // Форматируем как XXX-XXX-XX для читаемости
        if (numeric.length() >= 8) {
            return String.format("%s-%s-%s",
                    numeric.substring(0, 3),
                    numeric.substring(3, 6),
                    numeric.substring(6));
        }
        return numeric;
    }

    @Override
    public String toString() {
        return String.format("VehicleId{%s}", getValue());
    }
}