package tm.ugur.ugur_v3.infrastructure.persistence.config;

import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ValidationDepth;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Validated
public class DatabaseConfig {

    @Bean
    @Profile("!test")
    public PostgresqlConnectionConfiguration postgresqlConfig(DatabaseProperties props) {
        log.info("Configuring PostgreSQL with PostGIS for GPS tracking system");

        return PostgresqlConnectionConfiguration.builder()
                .host(props.getHost())
                .port(props.getPort())
                .database(props.getDatabase())
                .username(props.getUsername())
                .password(props.getPassword())
                .applicationName("ugur-gps-tracker")
                .connectTimeout(props.getConnectionTimeout())
                .statementTimeout(props.getStatementTimeout())

                .options(Map.of(
                        "search_path", "public",
                        "timezone", "UTC",

                        "shared_preload_libraries", "postgis",

                        "work_mem", "256MB",
                        "effective_cache_size", "2GB",
                        "random_page_cost", "1.1",
                        "seq_page_cost", "1.0",

                        "enable_seqscan", "off",
                        "enable_sort", "on",
                        "constraint_exclusion", "partition"
                ))
                .build();
    }

    @Bean
    public ConnectionPoolConfiguration poolConfig(PostgresqlConnectionConfiguration pgConfig,
                                                  PoolProperties poolProps) {
        log.info("Configuring connection pool: initial={}, max={}",
                poolProps.getInitialSize(), poolProps.getMaxSize());

        return ConnectionPoolConfiguration.builder(new PostgresqlConnectionFactory(pgConfig))
                .name("ugur-gps-pool")
                .initialSize(poolProps.getInitialSize())
                .maxSize(poolProps.getMaxSize())
                .minIdle(poolProps.getMinIdle())
                .maxIdleTime(poolProps.getMaxIdleTime())
                .maxAcquireTime(poolProps.getMaxAcquireTime())
                .maxCreateConnectionTime(poolProps.getMaxCreateConnectionTime())
                .maxLifeTime(poolProps.getMaxLifeTime())
                .validationQuery(poolProps.getValidationQuery())
                .validationDepth(ValidationDepth.REMOTE)
                .build();
    }

    @Setter
    @Getter
    @Validated
    @Configuration
    @ConfigurationProperties(prefix = "ugur.database")
    public static class DatabaseProperties {

        @NotBlank
        private String host = "localhost";

        @Min(1) @Max(65535)
        private int port = 5432;

        @NotBlank
        private String database = "ugur";

        @NotBlank
        private String username = "postgres";

        @NotBlank
        private String password = "postgres";

        @NotNull
        private Duration connectionTimeout = Duration.ofSeconds(30);

        @NotNull
        private Duration statementTimeout = Duration.ofMinutes(2);


    }

    @Setter
    @Getter
    @Validated
    @Configuration
    @ConfigurationProperties(prefix = "ugur.database.pool")
    public static class PoolProperties {

        @Min(10) @Max(100)
        private int initialSize = 25;

        @Min(50) @Max(2000)
        private int maxSize = 500;

        @Min(5) @Max(100)
        private int minIdle = 15;

        @NotNull
        private Duration maxIdleTime = Duration.ofMinutes(20);

        @NotNull
        private Duration maxAcquireTime = Duration.ofSeconds(5);

        @NotNull
        private Duration maxCreateConnectionTime = Duration.ofSeconds(3);

        @NotNull
        private Duration maxLifeTime = Duration.ofHours(1);

        @NotBlank
        private String validationQuery = "SELECT 1, PostGIS_Version()";


    }
}