package tm.ugur.ugur_v3.application.shared.caching;

import lombok.Getter;

import java.time.Duration;
import java.util.function.Function;


public final class CacheStrategy {

    public static final CacheStrategyConfig SHORT_TTL = CacheStrategyConfig.builder()
            .withTtl(Duration.ofMinutes(1))
            .withRefreshAhead(Duration.ofSeconds(45))
            .withMaxSize(10000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LRU)
            .withCompressionEnabled(false)
            .withPriority(CachePriority.HIGH)
            .withKeyGenerator(key -> CacheKey.of("vehicle", "data", key.toString()))
            .build();

    public static final CacheStrategyConfig MEDIUM_TTL = CacheStrategyConfig.builder()
            .withTtl(Duration.ofMinutes(5))
            .withRefreshAhead(Duration.ofMinutes(3))
            .withMaxSize(15000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LRU)
            .withCompressionEnabled(false)
            .withPriority(CachePriority.MEDIUM)
            .withKeyGenerator(key -> CacheKey.of("vehicle", "data", key.toString()))
            .build();

    public static final CacheStrategyConfig LONG_TTL = CacheStrategyConfig.builder()
            .withTtl(Duration.ofMinutes(15))
            .withRefreshAhead(Duration.ofMinutes(10))
            .withMaxSize(20000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LRU)
            .withCompressionEnabled(true)
            .withPriority(CachePriority.LOW)
            .withKeyGenerator(key -> CacheKey.of("vehicle", "data", key.toString()))
            .build();

    public static final CacheStrategyConfig VEHICLE_LOCATION = CacheStrategyConfig.builder()
            .withTtl(Duration.ofSeconds(30))
            .withRefreshAhead(Duration.ofSeconds(20))
            .withMaxSize(50000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LRU)
            .withCompressionEnabled(false)
            .withPriority(CachePriority.HIGH)
            .withKeyGenerator(key -> CacheKey.Vehicle.location(key.toString()))
            .build();


    public static final CacheStrategyConfig ETA_CALCULATIONS = CacheStrategyConfig.builder()
            .withTtl(Duration.ofMinutes(5))
            .withRefreshAhead(Duration.ofMinutes(3))
            .withMaxSize(100000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LFU)
            .withCompressionEnabled(true)
            .withPriority(CachePriority.MEDIUM)
            .withKeyGenerator(key -> {
                String[] parts = key.toString().split(":");
                return CacheKey.Route.eta(parts[0], parts[1]);
            })
            .build();

    public static final CacheStrategyConfig ROUTE_METADATA = CacheStrategyConfig.builder()
            .withTtl(Duration.ofHours(24))
            .withRefreshAhead(Duration.ofHours(20))
            .withMaxSize(10000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.TTL_BASED)
            .withCompressionEnabled(true)
            .withPriority(CachePriority.LOW)
            .withKeyGenerator(key -> CacheKey.Route.metadata(key.toString()))
            .build();

    public static final CacheStrategyConfig USER_PREFERENCES = CacheStrategyConfig.builder()
            .withTtl(Duration.ofHours(12))
            .withRefreshAhead(Duration.ofHours(10))
            .withMaxSize(200000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LRU)
            .withCompressionEnabled(true)
            .withPriority(CachePriority.MEDIUM)
            .withWriteThrough(true)
            .withKeyGenerator(key -> CacheKey.User.preferences(key.toString()))
            .build();


    public static final CacheStrategyConfig USER_SESSION = CacheStrategyConfig.builder()
            .withTtl(Duration.ofMinutes(30))
            .withMaxSize(100000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.TTL_BASED)
            .withCompressionEnabled(false)
            .withPriority(CachePriority.HIGH)
            .withKeyGenerator(key -> CacheKey.User.session(key.toString()))
            .build();


    public static final CacheStrategyConfig ANALYTICS_DATA = CacheStrategyConfig.builder()
            .withTtl(Duration.ofDays(7))
            .withMaxSize(50000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LFU)
            .withCompressionEnabled(true)
            .withPriority(CachePriority.LOW)
            .withKeyGenerator(key -> {
                String[] parts = key.toString().split(":");
                return CacheKey.Analytics.dailyMetrics(parts[0]);
            })
            .build();

    public static final CacheStrategyConfig EXTERNAL_API_DATA = CacheStrategyConfig.builder()
            .withTtl(Duration.ofMinutes(15))
            .withRefreshAhead(Duration.ofMinutes(10))
            .withMaxSize(20000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LRU)
            .withCompressionEnabled(true)
            .withPriority(CachePriority.MEDIUM)
            .withStaleWhileRevalidate(Duration.ofHours(1))
            .build();

    public static final CacheStrategyConfig COMPUTED_DATA = CacheStrategyConfig.builder()
            .withTtl(Duration.ofMinutes(30))
            .withRefreshAhead(Duration.ofMinutes(20))
            .withMaxSize(30000)
            .withEvictionStrategy(CacheManager.EvictionStrategy.LFU)
            .withCompressionEnabled(true)
            .withPriority(CachePriority.MEDIUM)
            .build();


    public static CacheStrategyConfig adaptive(DataCharacteristics characteristics) {
        CacheStrategyConfig.Builder builder = CacheStrategyConfig.builder();

        Duration ttl = switch (characteristics.volatility()) {
            case VERY_HIGH -> Duration.ofSeconds(10);
            case HIGH -> Duration.ofSeconds(30);
            case MEDIUM -> Duration.ofMinutes(5);
            case LOW -> Duration.ofHours(1);
            case VERY_LOW -> Duration.ofDays(1);
        };

        CacheManager.EvictionStrategy eviction = switch (characteristics.accessPattern()) {
            case HOTSPOT -> CacheManager.EvictionStrategy.LFU;
            case SEQUENTIAL -> CacheManager.EvictionStrategy.FIFO;
            case RANDOM -> CacheManager.EvictionStrategy.LRU;
            case TEMPORAL -> CacheManager.EvictionStrategy.TTL_BASED;
        };

        CachePriority priority = switch (characteristics.businessImportance()) {
            case CRITICAL -> CachePriority.CRITICAL;
            case HIGH -> CachePriority.HIGH;
            case MEDIUM -> CachePriority.MEDIUM;
            case LOW -> CachePriority.LOW;
        };

        return builder
                .withTtl(ttl)
                .withRefreshAhead(ttl.multipliedBy(3).dividedBy(4))
                .withEvictionStrategy(eviction)
                .withPriority(priority)
                .withCompressionEnabled(characteristics.dataSize() == DataSize.LARGE)
                .build();
    }


    public record DataCharacteristics(
            DataVolatility volatility,
            AccessPattern accessPattern,
            BusinessImportance businessImportance,
            DataSize dataSize) {

        public static DataCharacteristics of(
                DataVolatility volatility,
                AccessPattern accessPattern,
                BusinessImportance importance,
                DataSize size) {
                return new DataCharacteristics(volatility, accessPattern, importance, size);
            }
        }


    public enum DataVolatility {
        VERY_HIGH,
        HIGH,
        MEDIUM,
        LOW,
        VERY_LOW
    }

    public enum AccessPattern {
        HOTSPOT,
        SEQUENTIAL,
        RANDOM,
        TEMPORAL
    }

    public enum BusinessImportance {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public enum DataSize {
        SMALL,
        MEDIUM,
        LARGE
    }

    @Getter
    public enum CachePriority {
        CRITICAL(4),
        HIGH(3),
        MEDIUM(2),
        LOW(1);

        private final int level;

        CachePriority(int level) {
            this.level = level;
        }

    }




    @Getter
    public static class CacheStrategyConfig {
        private final Duration ttl;
        private final Duration refreshAhead;
        private final long maxSize;
        private final CacheManager.EvictionStrategy evictionStrategy;
        private final boolean compressionEnabled;
        private final CachePriority priority;
        private final boolean writeThrough;
        private final Duration staleWhileRevalidate;
        private final Function<Object, CacheKey> keyGenerator;

        private CacheStrategyConfig(Builder builder) {
            this.ttl = builder.ttl;
            this.refreshAhead = builder.refreshAhead;
            this.maxSize = builder.maxSize;
            this.evictionStrategy = builder.evictionStrategy;
            this.compressionEnabled = builder.compressionEnabled;
            this.priority = builder.priority;
            this.writeThrough = builder.writeThrough;
            this.staleWhileRevalidate = builder.staleWhileRevalidate;
            this.keyGenerator = builder.keyGenerator;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Duration ttl = Duration.ofMinutes(5);
            private Duration refreshAhead = Duration.ZERO;
            private long maxSize = 10000;
            private CacheManager.EvictionStrategy evictionStrategy = CacheManager.EvictionStrategy.LRU;
            private boolean compressionEnabled = false;
            private CachePriority priority = CachePriority.MEDIUM;
            private boolean writeThrough = false;
            private Duration staleWhileRevalidate = Duration.ZERO;
            private Function<Object, CacheKey> keyGenerator = key -> CacheKey.of("default", "data", key.toString());

            public Builder withTtl(Duration ttl) {
                this.ttl = ttl;
                return this;
            }

            public Builder withRefreshAhead(Duration refreshAhead) {
                this.refreshAhead = refreshAhead;
                return this;
            }

            public Builder withMaxSize(long maxSize) {
                this.maxSize = maxSize;
                return this;
            }

            public Builder withEvictionStrategy(CacheManager.EvictionStrategy evictionStrategy) {
                this.evictionStrategy = evictionStrategy;
                return this;
            }

            public Builder withCompressionEnabled(boolean compressionEnabled) {
                this.compressionEnabled = compressionEnabled;
                return this;
            }

            public Builder withPriority(CachePriority priority) {
                this.priority = priority;
                return this;
            }

            public Builder withWriteThrough(boolean writeThrough) {
                this.writeThrough = writeThrough;
                return this;
            }

            public Builder withStaleWhileRevalidate(Duration staleWhileRevalidate) {
                this.staleWhileRevalidate = staleWhileRevalidate;
                return this;
            }

            public Builder withKeyGenerator(Function<Object, CacheKey> keyGenerator) {
                this.keyGenerator = keyGenerator;
                return this;
            }

            public CacheStrategyConfig build() {
                return new CacheStrategyConfig(this);
            }
        }
    }
}