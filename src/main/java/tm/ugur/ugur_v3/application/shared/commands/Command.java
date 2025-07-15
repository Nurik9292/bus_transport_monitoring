package tm.ugur.ugur_v3.application.shared.commands;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;

public interface Command {

    default Instant getTimestamp() {
        return Instant.now();
    }

    default Map<String, Object> getMetadata() {
        return Map.of(
                "commandType", this.getClass().getSimpleName(),
                "timestamp", getTimestamp()
        );
    }

    default String getCorrelationId() {
        return null;
    }

    default CommandPriority getPriority() {
        return CommandPriority.NORMAL;
    }

    @Getter
    enum CommandPriority {
        CRITICAL(1),
        HIGH(2),
        NORMAL(3),
        LOW(4);

        private final int level;

        CommandPriority(int level) {
            this.level = level;
        }

    }
}