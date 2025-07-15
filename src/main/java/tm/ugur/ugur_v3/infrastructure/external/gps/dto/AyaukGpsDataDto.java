package tm.ugur.ugur_v3.infrastructure.external.gps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public record AyaukGpsDataDto(
        @JsonProperty("car_number") String carNumber,
        @JsonProperty("date") String date,
        @JsonProperty("number") String number,
        @JsonProperty("change") int change
) {

    public boolean isValid() {
        return carNumber != null &&
                !carNumber.trim().isEmpty() &&
                number != null &&
                !number.trim().isEmpty() &&
                date != null &&
                !date.trim().isEmpty();
    }

    public String getNormalizedCarNumber() {
        return carNumber != null ? carNumber.trim().toUpperCase() : null;
    }

    public String getRouteNumber() {
        return number != null ? number.trim() : null;
    }

    public LocalDate getParsedDate() {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isForToday() {
        LocalDate assignmentDate = getParsedDate();
        return assignmentDate != null && assignmentDate.equals(LocalDate.now());
    }

    public boolean isRecentAssignment() {
        LocalDate assignmentDate = getParsedDate();
        if (assignmentDate == null) {
            return false;
        }

        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        return assignmentDate.isAfter(threeDaysAgo) || assignmentDate.equals(threeDaysAgo);
    }

    public VehicleShift getShift() {
        return switch (change) {
            case 0 -> VehicleShift.FIRST_SHIFT;
            case 1 -> VehicleShift.SECOND_SHIFT;
            case 2 -> VehicleShift.THIRD_SHIFT;
            default -> VehicleShift.UNKNOWN;
        };
    }

    public boolean isOnActiveShift() {
        return getShift() != VehicleShift.UNKNOWN;
    }

    public Map<String, Object> getRouteMetadata() {
        return Map.of(
                "source", "AYAUK",
                "routeNumber", Objects.requireNonNull(getRouteNumber()),
                "assignmentDate", date,
                "shift", getShift().name(),
                "shiftNumber", change,
                "isToday", isForToday(),
                "isRecent", isRecentAssignment(),
                "vehicleId", Objects.requireNonNull(getNormalizedCarNumber())
        );
    }

    public String getAssignmentKey() {
        return String.format("%s_%s_%s_%d",
                getNormalizedCarNumber(),
                getRouteNumber(),
                date,
                change);
    }

    public boolean conflictsWith(AyaukGpsDataDto other) {
        if (other == null || !isValid() || !other.isValid()) {
            return false;
        }

        return Objects.equals(getNormalizedCarNumber(), other.getNormalizedCarNumber()) &&
                date.equals(other.date) &&
                change == other.change &&
                !Objects.equals(getRouteNumber(), other.getRouteNumber());
    }

    public int comparePriority(AyaukGpsDataDto other) {
        if (other == null) return 1;

        LocalDate thisDate = getParsedDate();
        LocalDate otherDate = other.getParsedDate();

        if (thisDate == null && otherDate == null) return 0;
        if (thisDate == null) return -1;
        if (otherDate == null) return 1;

        return thisDate.compareTo(otherDate);
    }

    @Getter
    public enum VehicleShift {
        FIRST_SHIFT("First Shift", "06:00-14:00"),
        SECOND_SHIFT("Second Shift", "14:00-22:00"),
        THIRD_SHIFT("Third Shift", "22:00-06:00"),
        UNKNOWN("Unknown Shift", "Unknown");

        private final String displayName;
        private final String timeRange;

        VehicleShift(String displayName, String timeRange) {
            this.displayName = displayName;
            this.timeRange = timeRange;
        }

        public boolean isActiveAt(java.time.LocalTime time) {
            return switch (this) {
                case FIRST_SHIFT -> time.isAfter(java.time.LocalTime.of(6, 0)) &&
                        time.isBefore(java.time.LocalTime.of(14, 0));
                case SECOND_SHIFT -> time.isAfter(java.time.LocalTime.of(14, 0)) &&
                        time.isBefore(java.time.LocalTime.of(22, 0));
                case THIRD_SHIFT -> time.isAfter(java.time.LocalTime.of(22, 0)) ||
                        time.isBefore(java.time.LocalTime.of(6, 0));
                default -> false;
            };
        }
    }
}