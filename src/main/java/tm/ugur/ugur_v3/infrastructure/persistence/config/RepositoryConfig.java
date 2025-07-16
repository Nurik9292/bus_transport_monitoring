package tm.ugur.ugur_v3.infrastructure.persistence.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider;
import tm.ugur.ugur_v3.infrastructure.persistence.repositories.jpa.R2dbcVehicleRepository;

import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Validated
public class RepositoryConfig {

    private final R2dbcEntityTemplate entityTemplate;

    @Bean
    public R2dbcVehicleRepository vehicleRepository() {
        log.info("Configuring Vehicle Repository with PostGIS support");
        return new R2dbcVehicleRepository(entityTemplate);
    }

    @Bean
    public VehicleLocationHistoryRepository locationHistoryRepository(RepositoryProperties props) {
        log.info("Configuring Location History Repository with batch size: {}", props.getBatchSize());
        return new VehicleLocationHistoryRepository(entityTemplate, props);
    }

    @Setter
    @Getter
    @Validated
    @Configuration
    @ConfigurationProperties(prefix = "ugur.repository")
    public static class RepositoryProperties {


        @Min(10) @Max(10000)
        private int batchSize = 1000;

        @NotNull
        private Duration queryTimeout = Duration.ofSeconds(30);

        @Min(100) @Max(100000)
        private int maxCacheSize = 10000;

        @NotNull
        private Duration cacheTtl = Duration.ofMinutes(5);

        @Min(1) @Max(100)
        private int maxRetries = 3;

        @NotNull
        private Duration retryDelay = Duration.ofSeconds(1);


        @DecimalMin("0.1") @DecimalMax("100000.0")
        private double defaultRadiusMeters = 1000.0;

        @Min(1) @Max(1000)
        private int maxNearbyResults = 100;

        @NotNull
        private Duration spatialQueryTimeout = Duration.ofSeconds(10);

    }

    public static class VehicleLocationHistoryRepository {

        private final R2dbcEntityTemplate template;
        private final RepositoryProperties props;

        public VehicleLocationHistoryRepository(R2dbcEntityTemplate template, RepositoryProperties props) {
            this.template = template;
            this.props = props;
        }

        public Mono<Integer> saveBatch(
               List<GpsDataProvider.GpsLocationData> locations) {


            return Mono.just(locations.size());
        }
    }
}