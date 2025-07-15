package tm.ugur.ugur_v3.application.shared.commands;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface QueryResult {

    default boolean isFromCache() {
        return false;
    }

    default Duration getQueryTime() {
        return Duration.ZERO;
    }

    default Instant getTimestamp() {
        return Instant.now();
    }

    default Map<String, Object> getMetadata() {
        return java.util.Map.of(
                "resultType", this.getClass().getSimpleName(),
                "fromCache", isFromCache(),
                "timestamp", getTimestamp(),
                "queryTime", getQueryTime().toMillis()
        );
    }

    default boolean shouldCache() {
        return true;
    }
}