package tm.ugur.ugur_v3.infrastructure.external.gps.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@EnableConfigurationProperties(TugdkGpsProperties.class)
@ConditionalOnProperty(name = "gps.tugdk.enabled", havingValue = "true", matchIfMissing = true)
@Import({TugdkWebClientConfig.class})
public class TugdkGpsConfig {

    private final TugdkGpsProperties properties;

    @PostConstruct
    public void validateConfiguration() {
        log.info("Initializing TUGDK GPS integration...");

        log.info("TUGDK GPS Configuration: {}", properties.getConfigSummary());

        validateRequiredProperties();

        if (!properties.isProductionReady()) {
            log.warn("TUGDK GPS configuration is not production-ready. Please review configuration.");
        }

        log.info("TUGDK GPS integration initialized successfully");
    }

    private void validateRequiredProperties() {
        if (properties.getUrl() == null || properties.getUrl().trim().isEmpty()) {
            throw new IllegalStateException("TUGDK GPS URL is required");
        }

        if (properties.getBearerToken() == null || properties.getBearerToken().trim().isEmpty()) {
            throw new IllegalStateException("TUGDK GPS Bearer token is required");
        }

        if (properties.getTimeout() == null || properties.getTimeout().isNegative()) {
            throw new IllegalStateException("TUGDK GPS timeout must be positive");
        }

        if (properties.getMaxRetries() < 1) {
            throw new IllegalStateException("TUGDK GPS max retries must be at least 1");
        }

        if (properties.getPollingInterval() == null || properties.getPollingInterval().isNegative()) {
            throw new IllegalStateException("TUGDK GPS polling interval must be positive");
        }
    }
}