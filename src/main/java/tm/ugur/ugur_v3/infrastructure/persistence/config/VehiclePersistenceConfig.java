package tm.ugur.ugur_v3.infrastructure.persistence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;
import java.time.Duration;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "ugur.persistence.vehicle")
public class VehiclePersistenceConfig {

    @NotNull
    private DatabaseValidation database = new DatabaseValidation();

    @NotNull
    private QueryPerformance queryPerformance = new QueryPerformance();

    @NotNull
    private GeospatialConfig geospatial = new GeospatialConfig();

    @NotNull
    private HistoryConfig history = new HistoryConfig();

    @Data
    public static class DatabaseValidation {

        @NotNull
        private LocationValidation location = new LocationValidation();

        @NotNull
        private VehicleEntityValidation vehicle = new VehicleEntityValidation();

        private boolean enableConstraintValidation = true;
        private boolean enableForeignKeyChecks = true;

        @Min(1) @Max(30)
        private int validationTimeoutSeconds = 5;

        @Data
        public static class LocationValidation {

            @DecimalMin("0.1") @DecimalMax("1000.0")
            private double maxAccuracyMeters = 100.0;

            @DecimalMin("0.0") @DecimalMax("500.0")
            private double maxSpeedKmh = 300.0;

            @NotNull
            private Duration maxLocationAge = Duration.ofMinutes(10);

            private boolean rejectNullIslandCoordinates = true;
            private boolean enableLocationBoundsCheck = true;

            // Turkmenistan bounds для validation
            @DecimalMin("52.0") @DecimalMax("67.0")
            private double minLongitude = 52.5;

            @DecimalMax("67.0")
            private double maxLongitude = 66.7;

            @DecimalMin("35.0") @DecimalMax("43.0")
            private double minLatitude = 35.1;

            @DecimalMax("43.0")
            private double maxLatitude = 42.8;
        }

        @Data
        public static class VehicleEntityValidation {

            @Min(3) @Max(50)
            private int minLicensePlateLength = 5;

            @Max(50)
            private int maxLicensePlateLength = 20;

            @Min(1) @Max(1000)
            private int minCapacity = 1;

            @Max(1000)
            private int maxCapacity = 300;

            private boolean requireUniqueVehicleIds = true;
            private boolean enableLicensePlateValidation = true;
        }
    }

    @Data
    public static class QueryPerformance {

        @NotNull
        private GeospatialQueries geospatial = new GeospatialQueries();

        @NotNull
        private BatchOperations batch = new BatchOperations();

        @NotNull
        private ConnectionSettings connection = new ConnectionSettings();

        @Data
        public static class GeospatialQueries {

            @NotNull
            private Duration spatialQueryTimeout = Duration.ofSeconds(10);

            @Min(1) @Max(10000)
            private int maxNearbyResults = 100;

            @DecimalMin("1.0") @DecimalMax("100000.0")
            private double defaultSearchRadiusMeters = 1000.0;

            @DecimalMin("1.0") @DecimalMax("1000000.0")
            private double maxSearchRadiusMeters = 50000.0;

            private boolean enableSpatialIndexHints = true;
            private boolean enableGeometrySimplification = true;
        }

        @Data
        public static class BatchOperations {

            @Min(10) @Max(10000)
            private int vehicleBatchSize = 1000;

            @Min(100) @Max(100000)
            private int locationBatchSize = 5000;

            @NotNull
            private Duration batchTimeout = Duration.ofSeconds(30);

            @Min(1) @Max(100)
            private int maxConcurrentBatches = 10;

            private boolean enableBatchOptimization = true;
        }

        @Data
        public static class ConnectionSettings {

            @NotNull
            private Duration queryTimeout = Duration.ofSeconds(30);

            @NotNull
            private Duration longRunningQueryTimeout = Duration.ofMinutes(2);

            @Min(1) @Max(10)
            private int maxRetries = 3;

            @NotNull
            private Duration retryDelay = Duration.ofSeconds(1);

            private boolean enableQueryPlanCaching = true;
        }
    }

    @Data
    public static class GeospatialConfig {

        @NotNull
        private IndexConfiguration indexes = new IndexConfiguration();

        @NotNull
        private CoordinateSystemConfig coordinateSystem = new CoordinateSystemConfig();

        @Data
        public static class IndexConfiguration {

            private boolean enableGistIndexes = true;
            private boolean enableSpGistIndexes = false; // SP-GiST для specific use cases
            private boolean enableBrinIndexes = false;   // Block Range indexes

            @DecimalMin("1.0") @DecimalMax("100.0")
            private double gistIndexFillfactor = 90.0;

            private boolean enableClusteringOnSpatialIndex = true;
        }

        @Data
        public static class CoordinateSystemConfig {

            @Min(1) @Max(999999)
            private int srid = 4326; // WGS84

            private boolean enableTransformations = true;
            private boolean enableGeographyType = false; // Use GEOMETRY instead of GEOGRAPHY

            // Distance calculations
            private String distanceFunction = "ST_Distance"; // vs ST_Distance_Sphere
            private boolean useSphericalCalculations = true;
        }
    }

    @Data
    public static class HistoryConfig {

        @NotNull
        private PartitioningConfig partitioning = new PartitioningConfig();

        @NotNull
        private RetentionConfig retention = new RetentionConfig();

        @Data
        public static class PartitioningConfig {

            private boolean enablePartitioning = true;
            private String partitioningStrategy = "MONTHLY"; // DAILY, WEEKLY, MONTHLY

            @Min(1) @Max(12)
            private int partitionsToPreCreate = 3;

            private boolean enablePartitionPruning = true;
            private boolean enablePartitionWiseJoins = true;
        }

        @Data
        public static class RetentionConfig {

            @NotNull
            private Duration rawLocationRetention = Duration.ofDays(90);

            @NotNull
            private Duration aggregatedDataRetention = Duration.ofDays(365);

            @NotNull
            private Duration backupRetention = Duration.ofDays(2555); // 7 years

            private boolean enableAutomaticCleanup = true;

            @NotNull
            private Duration cleanupInterval = Duration.ofHours(24);
        }
    }
}