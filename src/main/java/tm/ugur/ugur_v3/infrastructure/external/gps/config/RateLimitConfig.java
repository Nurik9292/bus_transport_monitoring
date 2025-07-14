package tm.ugur.ugur_v3.infrastructure.external.gps.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Configuration
public class RateLimitConfig {

    public static class ProviderLimits {
        private final int maxRequestsPerMinute;
        private final Duration windowSize;
        private final Duration penaltyDuration;

        public ProviderLimits(int maxRequestsPerMinute, Duration windowSize, Duration penaltyDuration) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
            this.windowSize = windowSize;
            this.penaltyDuration = penaltyDuration;
        }

    }

    public ProviderLimits getTugdkLimits() {
        return new ProviderLimits(60, Duration.ofMinutes(1), Duration.ofMinutes(5));
    }

    public ProviderLimits getAyaukLimits() {
        return new ProviderLimits(30, Duration.ofMinutes(1), Duration.ofMinutes(10));
    }
}