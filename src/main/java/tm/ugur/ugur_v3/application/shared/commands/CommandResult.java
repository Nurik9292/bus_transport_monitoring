package tm.ugur.ugur_v3.application.shared.commands;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface CommandResult {

    boolean isSuccessful();

    default Duration getProcessingTime() {
        return Duration.ZERO;
    }

    default Instant getTimestamp() {
        return Instant.now();
    }

    default Map<String, Object> getMetadata() {
        return Map.of(
                "resultType", this.getClass().getSimpleName(),
                "successful", isSuccessful(),
                "processingTime", getProcessingTime().toMillis(),
                "timestamp", getTimestamp().toString()
        );
    }

    default String getErrorMessage() {
        return null;
    }

    default boolean hasError() {
        return !isSuccessful();
    }

    default boolean isCritical() {
        return false;
    }

    default boolean requiresNotification() {
        return isCritical() || hasError();
    }

    default PerformanceCategory getPerformanceCategory() {
        long millis = getProcessingTime().toMillis();
        if (millis <= 50) return PerformanceCategory.FAST;
        if (millis <= 200) return PerformanceCategory.NORMAL;
        if (millis <= 1000) return PerformanceCategory.SLOW;
        return PerformanceCategory.VERY_SLOW;
    }

    default LoggingPriority getLoggingPriority() {
        if (hasError()) return LoggingPriority.ERROR;
        if (isCritical()) return LoggingPriority.WARN;
        if (getPerformanceCategory() == PerformanceCategory.VERY_SLOW) return LoggingPriority.WARN;
        return LoggingPriority.INFO;
    }

    default String getSummary() {
        if (isSuccessful()) {
            return String.format("Command successful (%dms)", getProcessingTime().toMillis());
        } else {
            return String.format("Command failed: %s",
                    getErrorMessage() != null ? getErrorMessage() : "unknown error");
        }
    }

    enum PerformanceCategory {
        FAST,
        NORMAL,
        SLOW,
        VERY_SLOW
    }

    enum LoggingPriority {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}