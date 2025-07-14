package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;

import java.util.regex.Pattern;

@Getter
public final class LicensePlate extends ValueObject {


    private static final Pattern PLATE_PATTERN = Pattern.compile("^\\d{4}\\s[A-Z]{3}$");


    private static final Pattern DASH_PATTERN = Pattern.compile("^\\d{2}-\\d{2}\\s[A-Z]{3}$");
    private static final Pattern COMPACT_PATTERN = Pattern.compile("^\\d{4}[A-Z]{3}$");


    private static final Pattern VALID_LETTERS = Pattern.compile("^[AGHMNPRSTUWXYZ]+$");

    private final String value;

    private LicensePlate(String value) {
        this.value = value;
        validate();
    }

    public static LicensePlate of(String plate) {
        if (plate == null || plate.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "License plate cannot be null or empty",
                    "INVALID_LICENSE_PLATE"
            );
        }

        String normalized = normalizeFormat(plate.trim());
        return new LicensePlate(normalized);
    }

    public static LicensePlate fromGpsApiName(String gpsName) {
        if (gpsName == null || gpsName.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "GPS name cannot be null or empty",
                    "INVALID_GPS_NAME"
            );
        }


        String cleaned = gpsName.trim().toUpperCase();


        if (DASH_PATTERN.matcher(cleaned).matches()) {
            String numbers = cleaned.substring(0, 2) + cleaned.substring(3, 5);
            String letters = cleaned.substring(6);
            cleaned = numbers + " " + letters;
        }

        return of(cleaned);
    }

    public static LicensePlate fromRouteApiCarNumber(String carNumber) {
        return of(carNumber);
    }

    private static String normalizeFormat(String plate) {

        String cleaned = plate.replaceAll("\\s+", " ").toUpperCase();


        if (COMPACT_PATTERN.matcher(cleaned).matches()) {
            cleaned = cleaned.substring(0, 4) + " " + cleaned.substring(4);
        }


        if (DASH_PATTERN.matcher(cleaned).matches()) {
            String numbers = cleaned.substring(0, 2) + cleaned.substring(3, 5);
            String letters = cleaned.substring(6);
            cleaned = numbers + " " + letters;
        }

        return cleaned;
    }

    @Override
    protected void validate() {
        if (!PLATE_PATTERN.matcher(value).matches()) {
            throw new BusinessRuleViolationException(
                    String.format("Invalid license plate format: '%s'. Expected format: 'NNNN XXX' (e.g., '1234 AGH')", value),
                    "INVALID_LICENSE_PLATE_FORMAT"
            );
        }

        validateLetters();
    }

    private void validateLetters() {
        String letters = value.substring(5);

        if (!VALID_LETTERS.matcher(letters).matches()) {
            throw new BusinessRuleViolationException(
                    String.format("Invalid letters in license plate: '%s'. Only Turkmen letters allowed", letters),
                    "INVALID_LICENSE_PLATE_LETTERS"
            );
        }
    }

    public boolean isGovernmentVehicle() {
        String letters = value.substring(5);
        return letters.equals("AGH") || letters.equals("AHN") || letters.equals("AGM");
    }

    public boolean isPublicTransport() {
        String letters = value.substring(5);
        return letters.equals("AGH") && isInPublicTransportNumberRange();
    }

    private boolean isInPublicTransportNumberRange() {
        try {
            int number = Integer.parseInt(value.substring(0, 4));

            return number >= 1000 && number <= 9999;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getNumbers() {
        return value.substring(0, 4);
    }

    public String getLetters() {
        return value.substring(5);
    }

    public String getCompactFormat() {
        return value.replace(" ", "");
    }

    public String getGpsApiFormat() {
        String numbers = value.substring(0, 4);
        String letters = value.substring(5);
        return numbers.substring(0, 2) + "-" + numbers.substring(2) + " " + letters;
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{value};
    }

    @Override
    public String toString() {
        return value;
    }

    public String getDisplayFormat() {
        return String.format("%s %s", getNumbers(), getLetters());
    }

    public static boolean isValidFormat(String plate) {
        try {
            of(plate);
            return true;
        } catch (BusinessRuleViolationException e) {
            return false;
        }
    }

    public static LicensePlate tryParseFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Cannot parse license plate from empty text",
                    "INVALID_PARSE_INPUT"
            );
        }


        String cleaned = text.trim().toUpperCase();


        if (isValidFormat(cleaned)) {
            return of(cleaned);
        }


        if (cleaned.matches(".*\\d{2}-\\d{2}\\s[A-Z]{3}.*")) {
            return fromGpsApiName(cleaned);
        }


        Pattern extractPattern = Pattern.compile("(\\d{4}\\s?[A-Z]{3})");
        java.util.regex.Matcher matcher = extractPattern.matcher(cleaned);
        if (matcher.find()) {
            return of(matcher.group(1));
        }

        throw new BusinessRuleViolationException(
                String.format("Could not extract valid license plate from text: '%s'", text),
                "INVALID_LICENSE_PLATE_EXTRACTION"
        );
    }
}