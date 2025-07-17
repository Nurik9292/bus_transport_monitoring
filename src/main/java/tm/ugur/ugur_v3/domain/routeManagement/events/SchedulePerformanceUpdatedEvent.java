
package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.Map;

@Getter
public final class SchedulePerformanceUpdatedEvent extends BaseRouteEvent {

    private final double scheduleAdherence;
    private final int missedTrips;
    private final double passengerLoadFactor;
    private final double averageDelay;
    private final int onTimeTrips;
    private final int totalTripsScheduled;

    private final double previousScheduleAdherence;
    private final int previousMissedTrips;
    private final double previousPassengerLoadFactor;
    private final double previousAverageDelay;

    private final PerformanceChangeType changeType;
    private final double adherenceChange;
    private final double loadFactorChange;
    private final double delayChange;
    private final PerformanceGrade currentGrade;
    private final PerformanceGrade previousGrade;

    private final Timestamp performancePeriodStart;
    private final Timestamp performancePeriodEnd;
    private final String reportingSource;
    private final boolean isRealTimeUpdate;

    private final String performanceNotes;
    private final boolean triggersAlert;

    private SchedulePerformanceUpdatedEvent(RouteId routeId, double scheduleAdherence, int missedTrips,
                                            double passengerLoadFactor, double averageDelay, int onTimeTrips,
                                            int totalTripsScheduled, double previousScheduleAdherence,
                                            int previousMissedTrips, double previousPassengerLoadFactor,
                                            double previousAverageDelay, Timestamp performancePeriodStart,
                                            Timestamp performancePeriodEnd, String reportingSource,
                                            boolean isRealTimeUpdate, String performanceNotes,
                                            String correlationId, Map<String, Object> metadata) {
        super("SchedulePerformanceUpdated", routeId, correlationId, metadata);

        this.scheduleAdherence = scheduleAdherence;
        this.missedTrips = missedTrips;
        this.passengerLoadFactor = passengerLoadFactor;
        this.averageDelay = averageDelay;
        this.onTimeTrips = onTimeTrips;
        this.totalTripsScheduled = totalTripsScheduled;

        this.previousScheduleAdherence = previousScheduleAdherence;
        this.previousMissedTrips = previousMissedTrips;
        this.previousPassengerLoadFactor = previousPassengerLoadFactor;
        this.previousAverageDelay = previousAverageDelay;

        this.adherenceChange = scheduleAdherence - previousScheduleAdherence;
        this.loadFactorChange = passengerLoadFactor - previousPassengerLoadFactor;
        this.delayChange = averageDelay - previousAverageDelay;
        this.changeType = determineChangeType();
        this.currentGrade = determinePerformanceGrade(scheduleAdherence, missedTrips, passengerLoadFactor);
        this.previousGrade = determinePerformanceGrade(previousScheduleAdherence, previousMissedTrips, previousPassengerLoadFactor);

        this.performancePeriodStart = performancePeriodStart;
        this.performancePeriodEnd = performancePeriodEnd;
        this.reportingSource = reportingSource;
        this.isRealTimeUpdate = isRealTimeUpdate;
        this.performanceNotes = performanceNotes;
        this.triggersAlert = shouldTriggerAlert();
    }


    public static SchedulePerformanceUpdatedEvent of(RouteId routeId, double scheduleAdherence,
                                                     int missedTrips, double passengerLoadFactor) {
        return new SchedulePerformanceUpdatedEvent(routeId, scheduleAdherence, missedTrips, passengerLoadFactor,
                0.0, 0, 0, 0.0, 0, 0.0, 0.0, null, null, "SYSTEM", true, null, null, null);
    }

    public static SchedulePerformanceUpdatedEvent withComparison(RouteId routeId, double scheduleAdherence,
                                                                 int missedTrips, double passengerLoadFactor,
                                                                 double previousAdherence, int previousMissed,
                                                                 double previousLoadFactor) {
        return new SchedulePerformanceUpdatedEvent(routeId, scheduleAdherence, missedTrips, passengerLoadFactor,
                0.0, 0, 0, previousAdherence, previousMissed, previousLoadFactor, 0.0,
                null, null, "SYSTEM", true, null, null, null);
    }

    public static SchedulePerformanceUpdatedEvent detailed(RouteId routeId, double scheduleAdherence, int missedTrips,
                                                           double passengerLoadFactor, double averageDelay,
                                                           int onTimeTrips, int totalTripsScheduled,
                                                           double previousAdherence, int previousMissed,
                                                           double previousLoadFactor, double previousDelay,
                                                           Timestamp periodStart, Timestamp periodEnd,
                                                           String reportingSource, String notes) {
        return new SchedulePerformanceUpdatedEvent(routeId, scheduleAdherence, missedTrips, passengerLoadFactor,
                averageDelay, onTimeTrips, totalTripsScheduled, previousAdherence,
                previousMissed, previousLoadFactor, previousDelay, periodStart,
                periodEnd, reportingSource, false, notes, null, null);
    }

    public static SchedulePerformanceUpdatedEvent realTime(RouteId routeId, double scheduleAdherence,
                                                           int missedTrips, double passengerLoadFactor,
                                                           String reportingSource) {
        return new SchedulePerformanceUpdatedEvent(routeId, scheduleAdherence, missedTrips, passengerLoadFactor,
                0.0, 0, 0, 0.0, 0, 0.0, 0.0, null, null, reportingSource, true, null, null, null);
    }


    public boolean isPerformanceImprovement() {
        return changeType == PerformanceChangeType.IMPROVEMENT ||
                changeType == PerformanceChangeType.SIGNIFICANT_IMPROVEMENT;
    }

    public boolean isPerformanceDecline() {
        return changeType == PerformanceChangeType.DECLINE ||
                changeType == PerformanceChangeType.SIGNIFICANT_DECLINE;
    }

    public boolean requiresAttention() {
        return currentGrade.ordinal() >= PerformanceGrade.POOR.ordinal();
    }

    public boolean isOverloaded() {
        return passengerLoadFactor > 1.2;
    }

    public boolean isUnderutilized() {
        return passengerLoadFactor < 0.6;
    }

    public boolean hasSignificantChange() {
        return Math.abs(adherenceChange) > 5.0 ||
                Math.abs(loadFactorChange) > 0.2 ||
                Math.abs(delayChange) > 2.0;
    }

    public boolean isExcellentPerformance() {
        return currentGrade == PerformanceGrade.EXCELLENT &&
                scheduleAdherence >= 95.0 &&
                missedTrips == 0 &&
                passengerLoadFactor >= 0.8 && passengerLoadFactor <= 1.1;
    }

    public boolean isCriticalPerformance() {
        return currentGrade == PerformanceGrade.CRITICAL ||
                scheduleAdherence < 50.0 ||
                missedTrips > totalTripsScheduled * 0.2; // >20% пропущенных рейсов
    }

    public double getCompletionRate() {
        if (totalTripsScheduled == 0) return 100.0;
        return ((double) (totalTripsScheduled - missedTrips) / totalTripsScheduled) * 100.0;
    }

    public double getOnTimeRate() {
        if (totalTripsScheduled == 0) return 100.0;
        return ((double) onTimeTrips / totalTripsScheduled) * 100.0;
    }

    public double getOverallPerformanceScore() {
        double adherenceWeight = 0.4;
        double completionWeight = 0.3;
        double loadFactorWeight = 0.2;
        double delayWeight = 0.1;

        double adherenceScore = Math.min(scheduleAdherence, 100.0);
        double completionScore = getCompletionRate();
        double loadFactorScore = calculateLoadFactorScore();
        double delayScore = calculateDelayScore();

        return (adherenceScore * adherenceWeight) +
                (completionScore * completionWeight) +
                (loadFactorScore * loadFactorWeight) +
                (delayScore * delayWeight);
    }


    private PerformanceChangeType determineChangeType() {
        if (Math.abs(adherenceChange) < 1.0 && Math.abs(loadFactorChange) < 0.1) {
            return PerformanceChangeType.STABLE;
        }

        double overallChange = adherenceChange + (loadFactorChange * 50) - (delayChange * 10);

        if (overallChange > 10.0) return PerformanceChangeType.SIGNIFICANT_IMPROVEMENT;
        if (overallChange > 2.0) return PerformanceChangeType.IMPROVEMENT;
        if (overallChange < -10.0) return PerformanceChangeType.SIGNIFICANT_DECLINE;
        if (overallChange < -2.0) return PerformanceChangeType.DECLINE;
        return PerformanceChangeType.STABLE;
    }

    private PerformanceGrade determinePerformanceGrade(double adherence, int missed, double loadFactor) {
        if (adherence >= 95.0 && missed == 0 && loadFactor >= 0.8 && loadFactor <= 1.1) {
            return PerformanceGrade.EXCELLENT;
        }
        if (adherence >= 85.0 && missed <= 2 && loadFactor >= 0.7 && loadFactor <= 1.2) {
            return PerformanceGrade.GOOD;
        }
        if (adherence >= 75.0 && missed <= 5 && loadFactor >= 0.6 && loadFactor <= 1.3) {
            return PerformanceGrade.SATISFACTORY;
        }
        if (adherence >= 60.0 && loadFactor <= 1.5) {
            return PerformanceGrade.POOR;
        }
        return PerformanceGrade.CRITICAL;
    }

    private boolean shouldTriggerAlert() {
        return requiresAttention() ||
                hasSignificantChange() ||
                isCriticalPerformance() ||
                (currentGrade.ordinal() > previousGrade.ordinal() + 1);
    }

    private double calculateLoadFactorScore() {
        if (passengerLoadFactor >= 0.8 && passengerLoadFactor <= 1.0) return 100.0;
        if (passengerLoadFactor >= 0.7 && passengerLoadFactor <= 1.1) return 90.0;
        if (passengerLoadFactor >= 0.6 && passengerLoadFactor <= 1.2) return 80.0;
        if (passengerLoadFactor >= 0.5 && passengerLoadFactor <= 1.3) return 70.0;
        if (passengerLoadFactor <= 1.5) return 50.0;
        return 30.0;
    }

    private double calculateDelayScore() {
        if (averageDelay <= 1.0) return 100.0;
        if (averageDelay <= 3.0) return 90.0;
        if (averageDelay <= 5.0) return 80.0;
        if (averageDelay <= 10.0) return 60.0;
        return 40.0;
    }


    @Getter
    public enum PerformanceChangeType {
        SIGNIFICANT_IMPROVEMENT("Значительное улучшение", "📈"),
        IMPROVEMENT("Улучшение", "⬆️"),
        STABLE("Стабильно", "➡️"),
        DECLINE("Ухудшение", "⬇️"),
        SIGNIFICANT_DECLINE("Значительное ухудшение", "📉");

        private final String displayName;
        private final String indicator;

        PerformanceChangeType(String displayName, String indicator) {
            this.displayName = displayName;
            this.indicator = indicator;
        }

        @Override
        public String toString() { return indicator + " " + displayName; }
    }

    @Getter
    public enum PerformanceGrade {
        EXCELLENT("Отлично", "🟢", 5),
        GOOD("Хорошо", "🟡", 4),
        SATISFACTORY("Удовлетворительно", "🟠", 3),
        POOR("Плохо", "🔴", 2),
        CRITICAL("Критично", "🚨", 1);

        private final String displayName;
        private final String indicator;
        private final int numericValue;

        PerformanceGrade(String displayName, String indicator, int numericValue) {
            this.displayName = displayName;
            this.indicator = indicator;
            this.numericValue = numericValue;
        }

        public boolean isBetterThan(PerformanceGrade other) {
            return this.numericValue > other.numericValue;
        }

        public boolean isWorseThan(PerformanceGrade other) {
            return this.numericValue < other.numericValue;
        }

        @Override
        public String toString() { return indicator + " " + displayName; }
    }

    // ================== OBJECT METHODS ==================

    @Override
    public String toString() {
        return String.format(
                "SchedulePerformanceUpdatedEvent{routeId=%s, adherence=%.1f%% (%+.1f%%), missed=%d, load=%.2f (%+.2f), grade=%s→%s, change=%s}",
                routeId, scheduleAdherence, adherenceChange, missedTrips, passengerLoadFactor, loadFactorChange,
                previousGrade, currentGrade, changeType
        );
    }

    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append(String.format("Performance Update for Route %s\n", routeId));
        info.append(String.format("Period: %s - %s\n", performancePeriodStart, performancePeriodEnd));
        info.append(String.format("Schedule Adherence: %.1f%% (was %.1f%%, change: %+.1f%%)\n",
                scheduleAdherence, previousScheduleAdherence, adherenceChange));
        info.append(String.format("Missed Trips: %d (was %d)\n", missedTrips, previousMissedTrips));
        info.append(String.format("Load Factor: %.2f (was %.2f, change: %+.2f)\n",
                passengerLoadFactor, previousPassengerLoadFactor, loadFactorChange));
        info.append(String.format("Average Delay: %.1f min (was %.1f min, change: %+.1f min)\n",
                averageDelay, previousAverageDelay, delayChange));
        info.append(String.format("Performance Grade: %s (was %s)\n", currentGrade, previousGrade));
        info.append(String.format("Overall Change: %s\n", changeType));
        info.append(String.format("Overall Score: %.1f/100\n", getOverallPerformanceScore()));

        if (performanceNotes != null && !performanceNotes.trim().isEmpty()) {
            info.append(String.format("Notes: %s\n", performanceNotes));
        }

        if (triggersAlert) {
            info.append("⚠️ This update triggers a performance alert\n");
        }

        return info.toString();
    }
}