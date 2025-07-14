package tm.ugur.ugur_v3.infrastructure.external.gps.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "gps")
public class GpsApiConfig {

    private TugdkConfig tugdk;
    private AyaukConfig ayauk;
    private GlobalConfig global;

    @Getter
    public static class TugdkConfig {
        private String url;
        private String bearerToken;
        private final Duration timeout = Duration.ofSeconds(10);
        private final int maxRetries = 3;
        private final Duration retryDelay = Duration.ofSeconds(2);
    }


    @Getter
    public static class AyaukConfig {
        private String url;
        private String username;
        private String password;
        private final Duration timeout = Duration.ofSeconds(15);
    }

    @Getter
    public static class GlobalConfig {
        private final int batchSize = 100;
        private final Duration healthCheckInterval = Duration.ofMinutes(1);
        private final Duration circuitBreakerTimeout = Duration.ofSeconds(30);
        private final double circuitBreakerFailureThreshold = 0.5;
    }
}
