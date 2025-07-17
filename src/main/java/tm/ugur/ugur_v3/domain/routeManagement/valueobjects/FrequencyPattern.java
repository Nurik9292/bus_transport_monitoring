package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.time.LocalTime;


@Getter
public final class FrequencyPattern extends ValueObject {

    private final String patternName;
    private final int headwayMinutes;
    private final LocalTime peakStartTime;
    private final LocalTime peakEndTime;
    private final int peakHeadwayMinutes;
    private final double loadFactor;
    private final boolean isActive;

    private FrequencyPattern(String patternName, int headwayMinutes, LocalTime peakStartTime,
                             LocalTime peakEndTime, int peakHeadwayMinutes, double loadFactor, boolean isActive) {
        this.patternName = patternName;
        this.headwayMinutes = headwayMinutes;
        this.peakStartTime = peakStartTime;
        this.peakEndTime = peakEndTime;
        this.peakHeadwayMinutes = peakHeadwayMinutes;
        this.loadFactor = loadFactor;
        this.isActive = isActive;
    }

    public static FrequencyPattern create(String patternName, int headwayMinutes) {
        validatePattern(patternName, headwayMinutes);
        return new FrequencyPattern(patternName, headwayMinutes, null, null, headwayMinutes, 1.0, true);
    }

    public static FrequencyPattern createWithPeakHours(String patternName, int regularHeadway,
                                                       LocalTime peakStart, LocalTime peakEnd, int peakHeadway) {
        validatePattern(patternName, regularHeadway);
        validatePeakPattern(peakStart, peakEnd, peakHeadway);
        return new FrequencyPattern(patternName, regularHeadway, peakStart, peakEnd, peakHeadway, 1.0, true);
    }

    public int getHeadwayForTime(LocalTime time) {
        if (hasPeakHours() && isTimeInPeak(time)) {
            return peakHeadwayMinutes;
        }
        return headwayMinutes;
    }

    public boolean hasPeakHours() {
        return peakStartTime != null && peakEndTime != null;
    }

    public boolean isTimeInPeak(LocalTime time) {
        if (!hasPeakHours()) return false;

        if (peakEndTime.isAfter(peakStartTime)) {
            return !time.isBefore(peakStartTime) && !time.isAfter(peakEndTime);
        } else {
            return !time.isBefore(peakStartTime) || !time.isAfter(peakEndTime);
        }
    }

    public FrequencyPattern adjustLoadFactor(double newLoadFactor) {
        if (newLoadFactor < 0.1 || newLoadFactor > 3.0) {
            throw new BusinessRuleViolationException("INVALID_LOAD_FACTOR",
                    "Load factor must be between 0.1 and 3.0");
        }
        return new FrequencyPattern(patternName, headwayMinutes, peakStartTime, peakEndTime,
                peakHeadwayMinutes, newLoadFactor, isActive);
    }

    public FrequencyPattern activate() {
        return new FrequencyPattern(patternName, headwayMinutes, peakStartTime, peakEndTime,
                peakHeadwayMinutes, loadFactor, true);
    }

    public FrequencyPattern deactivate() {
        return new FrequencyPattern(patternName, headwayMinutes, peakStartTime, peakEndTime,
                peakHeadwayMinutes, loadFactor, false);
    }

    private static void validatePattern(String patternName, int headwayMinutes) {
        if (patternName == null || patternName.trim().isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_PATTERN_NAME", "Pattern name cannot be empty");
        }

        if (headwayMinutes < 1 || headwayMinutes > 240) {
            throw new BusinessRuleViolationException("INVALID_HEADWAY",
                    "Headway must be between 1 and 240 minutes");
        }
    }

    private static void validatePeakPattern(LocalTime peakStart, LocalTime peakEnd, int peakHeadway) {
        if (peakStart == null || peakEnd == null) {
            throw new BusinessRuleViolationException("NULL_PEAK_TIME", "Peak times cannot be null");
        }

        if (peakHeadway < 1 || peakHeadway > 120) {
            throw new BusinessRuleViolationException("INVALID_PEAK_HEADWAY",
                    "Peak headway must be between 1 and 120 minutes");
        }
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{patternName, headwayMinutes, peakStartTime, peakEndTime,
                peakHeadwayMinutes, loadFactor, isActive};
    }

    @Override
    public String toString() {
        String base = String.format("%s: %d min", patternName, headwayMinutes);
        if (hasPeakHours()) {
            base += String.format(" (Peak %s-%s: %d min)", peakStartTime, peakEndTime, peakHeadwayMinutes);
        }
        return base;
    }
}