package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.FrequencyPattern;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.time.LocalTime;
import java.util.Map;

@Getter
public final class FrequencyPatternAddedEvent extends BaseRouteEvent {

    private final String patternName;
    private final FrequencyPattern frequencyPattern;
    private final int headwayMinutes;
    private final LocalTime peakStartTime;
    private final LocalTime peakEndTime;
    private final int peakHeadwayMinutes;
    private final double loadFactor;
    private final boolean hasPeakHours;
    private final String addedBy;
    private final PatternType patternType;

    private FrequencyPatternAddedEvent(RouteId routeId, String patternName, FrequencyPattern frequencyPattern,
                                       String addedBy, PatternType patternType, String correlationId,
                                       Map<String, Object> metadata) {
        super("FrequencyPatternAdded", routeId, correlationId, metadata);
        this.patternName = patternName;
        this.frequencyPattern = frequencyPattern;
        this.headwayMinutes = frequencyPattern.getHeadwayMinutes();
        this.peakStartTime = frequencyPattern.getPeakStartTime();
        this.peakEndTime = frequencyPattern.getPeakEndTime();
        this.peakHeadwayMinutes = frequencyPattern.getPeakHeadwayMinutes();
        this.loadFactor = frequencyPattern.getLoadFactor();
        this.hasPeakHours = frequencyPattern.hasPeakHours();
        this.addedBy = addedBy;
        this.patternType = patternType;
    }

    public static FrequencyPatternAddedEvent standard(RouteId routeId, String patternName, FrequencyPattern pattern, String addedBy) {
        return new FrequencyPatternAddedEvent(routeId, patternName, pattern, addedBy, PatternType.STANDARD, null, null);
    }

    public static FrequencyPatternAddedEvent peakHours(RouteId routeId, String patternName, FrequencyPattern pattern, String addedBy) {
        return new FrequencyPatternAddedEvent(routeId, patternName, pattern, addedBy, PatternType.PEAK_HOURS, null, null);
    }

    public static FrequencyPatternAddedEvent dynamic(RouteId routeId, String patternName, FrequencyPattern pattern, String addedBy) {
        return new FrequencyPatternAddedEvent(routeId, patternName, pattern, addedBy, PatternType.DYNAMIC, null, null);
    }

    public static FrequencyPatternAddedEvent seasonal(RouteId routeId, String patternName, FrequencyPattern pattern, String addedBy) {
        return new FrequencyPatternAddedEvent(routeId, patternName, pattern, addedBy, PatternType.SEASONAL, null, null);
    }

    public boolean isHighFrequency() { return headwayMinutes <= 10; }
    public boolean isLowFrequency() { return headwayMinutes >= 30; }
    public boolean hasSignificantPeakReduction() {
        return hasPeakHours && (headwayMinutes - peakHeadwayMinutes) >= 10;
    }
    public boolean isLoadFactorOptimized() { return loadFactor >= 0.8 && loadFactor <= 1.2; }

    public enum PatternType {
        STANDARD("Стандартный"),
        PEAK_HOURS("Час пик"),
        DYNAMIC("Динамический"),
        SEASONAL("Сезонный"),
        EVENT_BASED("Событийный"),
        WEATHER_ADAPTIVE("Адаптивный к погоде");

        private final String displayName;

        PatternType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Override
    public String toString() {
        String peakInfo = hasPeakHours ? String.format(", peak=%s-%s (%dmin)", peakStartTime, peakEndTime, peakHeadwayMinutes) : "";
        return String.format("FrequencyPatternAddedEvent{routeId=%s, pattern='%s', type=%s, headway=%dmin%s, load=%.1f, by='%s'}",
                routeId, patternName, patternType, headwayMinutes, peakInfo, loadFactor, addedBy);
    }
}