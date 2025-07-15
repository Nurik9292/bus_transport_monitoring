package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;
import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidEntityIdException;

import java.util.UUID;
import java.util.regex.Pattern;

public final class TrackingSessionId extends EntityId {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern CUSTOM_PATTERN = Pattern.compile(
            "^TRK-[A-Z0-9]{8,16}$"
    );

    private TrackingSessionId(String value) {
        super(value);
    }

    public static TrackingSessionId of(String value) {
        return new TrackingSessionId(value);
    }

    public static TrackingSessionId generate() {
        return new TrackingSessionId(UUID.randomUUID().toString());
    }

    public static TrackingSessionId withPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return generate();
        }

        String normalized = prefix.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (normalized.length() > 16) {
            normalized = normalized.substring(0, 16);
        } else if (normalized.length() < 8) {
            String suffix = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
            normalized = normalized + suffix.substring(0, Math.min(8 - normalized.length(), suffix.length()));
        }

        return new TrackingSessionId("TRK-" + normalized);
    }

    public static TrackingSessionId forVehicleAt(VehicleId vehicleId, long timestamp) {
        if (vehicleId == null) {
            return generate();
        }

        String vehicleHash = Integer.toHexString(vehicleId.getValue().hashCode()).toUpperCase();
        String timeHash = Long.toHexString(timestamp).toUpperCase();

        String combined = vehicleHash + timeHash;
        if (combined.length() > 16) {
            combined = combined.substring(0, 16);
        } else if (combined.length() < 8) {
            combined = combined + "00000000".substring(combined.length());
        }

        return new TrackingSessionId("TRK-" + combined);
    }

    @Override
    protected void validate() {


        String value = getValue();

        if (!isValidFormat(value)) {
            throw new InvalidEntityIdException(
                    String.format("Invalid tracking session ID format: '%s'. " +
                            "Expected UUID or TRK-XXXXXXXX format", value)
            );
        }
    }

    private boolean isValidFormat(String value) {
        return UUID_PATTERN.matcher(value).matches() ||
                CUSTOM_PATTERN.matcher(value).matches();
    }

    public boolean isUUIDFormat() {
        return UUID_PATTERN.matcher(getValue()).matches();
    }

    public boolean isCustomFormat() {
        return CUSTOM_PATTERN.matcher(getValue()).matches();
    }

    public String getCorrelationPrefix() {
        if (!isCustomFormat()) {
            return null;
        }

        String value = getValue();
        return value.substring(4);
    }

    public boolean canCorrelateWith(TrackingSessionId other) {
        if (other == null) {
            return false;
        }

        if (isUUIDFormat() || other.isUUIDFormat()) {
            return false;
        }

        String thisPrefix = getCorrelationPrefix();
        String otherPrefix = other.getCorrelationPrefix();

        if (thisPrefix == null || otherPrefix == null) {
            return false;
        }

        return thisPrefix.length() >= 4 &&
                otherPrefix.length() >= 4 &&
                thisPrefix.substring(0, 4).equals(otherPrefix.substring(0, 4));
    }

    public String getShortFormat() {
        String value = getValue();
        if (value.length() <= 12) {
            return value;
        }

        if (isCustomFormat()) {

        }

        return value.substring(0, 8) + "...";
    }

    public String getCorrelationKey() {
        if (isCustomFormat()) {
            String prefix = getCorrelationPrefix();
            return prefix != null && prefix.length() >= 4 ?
                    "TRK-" + prefix.substring(0, 4) : "TRK-UNKNOWN";
        } else {
            String value = getValue();
            int dashIndex = value.indexOf('-');
            return dashIndex > 0 ? value.substring(0, dashIndex) : value.substring(0, 8);
        }
    }

    public static boolean isValidSessionFormat(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }

        String trimmed = sessionId.trim();
        return UUID_PATTERN.matcher(trimmed).matches() ||
                CUSTOM_PATTERN.matcher(trimmed).matches();
    }

    public static TrackingSessionId tryParse(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidEntityIdException("Cannot parse tracking session ID from empty input");
        }

        String cleaned = input.trim().toUpperCase();

        if (isValidSessionFormat(cleaned)) {
            return of(cleaned);
        }

        java.util.regex.Matcher uuidMatcher = UUID_PATTERN.matcher(cleaned);
        if (uuidMatcher.find()) {
            return of(uuidMatcher.group());
        }

        java.util.regex.Matcher customMatcher = CUSTOM_PATTERN.matcher(cleaned);
        if (customMatcher.find()) {
            return of(customMatcher.group());
        }

        throw new InvalidEntityIdException(
                String.format("Could not parse valid tracking session ID from: '%s'", input)
        );
    }
}