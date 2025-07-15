package tm.ugur.ugur_v3.application.shared.commands;

import java.time.Instant;
import java.util.Map;

public interface Query {

    default Instant getTimestamp() {
        return Instant.now();
    }

    default Map<String, Object> getMetadata() {
        return Map.of(
                "queryType", this.getClass().getSimpleName(),
                "timestamp", getTimestamp()
        );
    }

    default String getCorrelationId() {
        return null;
    }

    default boolean isCacheable() {
        return true;
    }

    default java.time.Duration getCacheTtl() {
        return java.time.Duration.ofMinutes(5);
    }

    default java.time.Duration getTimeout() {
        return java.time.Duration.ofSeconds(30);
    }
}