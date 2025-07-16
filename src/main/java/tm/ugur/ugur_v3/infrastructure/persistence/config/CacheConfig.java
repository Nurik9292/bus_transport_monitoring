package tm.ugur.ugur_v3.infrastructure.persistence.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.constraints.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Validated
public class CacheConfig {


    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        log.info("Configuring Reactive Redis Template for GPS tracking cache");

        RedisSerializationContext<String, Object> serializationContext =
                RedisSerializationContext.<String, Object>newSerializationContext()
                        .key(new StringRedisSerializer())
                        .value(new GenericJackson2JsonRedisSerializer())
                        .hashKey(new StringRedisSerializer())
                        .hashValue(new GenericJackson2JsonRedisSerializer())
                        .build();

        ReactiveRedisTemplate<String, Object> template = new ReactiveRedisTemplate<>(
                connectionFactory, serializationContext);

        log.info("Reactive Redis Template configured successfully");
        return template;
    }

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        RedisSerializationContext<String, String> serializationContext =
                RedisSerializationContext.<String, String>newSerializationContext()
                        .key(new StringRedisSerializer())
                        .value(new StringRedisSerializer())
                        .hashKey(new StringRedisSerializer())
                        .hashValue(new StringRedisSerializer())
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    @Bean
    public ReactiveGpsDataCacheService reactiveGpsDataCacheService(
            ReactiveRedisTemplate<String, Object> redisTemplate,
            CacheProperties cacheProps) {
        return new ReactiveGpsDataCacheService(redisTemplate, cacheProps);
    }

    @Bean
    public ReactiveVehicleCacheService reactiveVehicleCacheService(
            ReactiveRedisTemplate<String, Object> redisTemplate,
            CacheProperties cacheProps) {
        return new ReactiveVehicleCacheService(redisTemplate, cacheProps);
    }

    @Setter
    @Getter
    @Validated
    @Configuration
    @ConfigurationProperties(prefix = "ugur.cache")
    public static class CacheProperties {

        @NotNull
        private Duration defaultTtl = Duration.ofMinutes(5);

        @NotNull
        private VehicleCacheConfig vehicles = new VehicleCacheConfig();

        @NotNull
        private LocationCacheConfig locations = new LocationCacheConfig();

        @NotNull
        private RouteCacheConfig routes = new RouteCacheConfig();

        private boolean enableDistributedCache = true;
        private boolean enableCacheMetrics = true;
        private boolean enableCompression = true;
        private boolean enableReactiveOptimizations = true;

        @Setter
        @Getter
        @Validated
        public static class VehicleCacheConfig {
            @NotNull
            private Duration ttl = Duration.ofMinutes(5);

            @Min(1000) @Max(100000)
            private int maxSize = 10000;

            private boolean enableNearCache = true;
            private boolean enableReactiveStreaming = true;

        }

        @Setter
        @Getter
        @Validated
        public static class LocationCacheConfig {
            @NotNull
            private Duration ttl = Duration.ofSeconds(30);

            @Min(10000) @Max(1000000)
            private int maxSize = 50000;

            private boolean enableCompression = true;
            private boolean enableBatchOperations = true;

        }

        @Setter
        @Getter
        @Validated
        public static class RouteCacheConfig {
            @NotNull
            private Duration ttl = Duration.ofHours(1);

            @Min(100) @Max(10000)
            private int maxSize = 1000;

        }
    }

    public static class ReactiveGpsDataCacheService {

        private final ReactiveRedisTemplate<String, Object> redisTemplate;
        private final CacheProperties cacheProps;

        private static final String GPS_LOCATION_PREFIX = "ugur:gps:location:";
        private static final String GPS_BATCH_PREFIX = "ugur:gps:batch:";
        private static final String VEHICLE_STATUS_PREFIX = "ugur:vehicle:status:";
        private static final String VEHICLES_IN_RADIUS_PREFIX = "ugur:nearby:";
        private static final String ROUTE_ASSIGNMENT_PREFIX = "ugur:route:assignment:";

        public ReactiveGpsDataCacheService(ReactiveRedisTemplate<String, Object> redisTemplate,
                                           CacheProperties cacheProps) {
            this.redisTemplate = redisTemplate;
            this.cacheProps = cacheProps;
            log.info("Reactive GPS Data Cache Service initialized with TTL: {}",
                    cacheProps.getLocations().getTtl());
        }

        public Mono<Boolean> cacheVehicleLocation(
                String vehicleId,
                tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate location,
                double speedMs) {

            String key = GPS_LOCATION_PREFIX + vehicleId;

            VehicleLocationCache locationCache = new VehicleLocationCache(
                    vehicleId,
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    speedMs,
                    Instant.now()
            );

            return redisTemplate.opsForValue()
                    .set(key, locationCache, cacheProps.getLocations().getTtl())
                    .doOnNext(success -> log.debug("Cached location for vehicle: {} - success: {}", vehicleId, success))
                    .doOnError(error -> log.error("Failed to cache location for vehicle: {}", vehicleId, error));
        }

        public Mono<VehicleLocationCache> getCachedVehicleLocation(String vehicleId) {
            String key = GPS_LOCATION_PREFIX + vehicleId;

            return redisTemplate.opsForValue()
                    .get(key)
                    .cast(VehicleLocationCache.class)
                    .doOnNext(cache -> log.debug("Retrieved cached location for vehicle: {}", vehicleId))
                    .doOnError(error -> log.error("Failed to retrieve cached location for vehicle: {}", vehicleId, error));
        }

        public Mono<Boolean> batchCacheLocations(Map<String, VehicleLocationCache> locations) {

            Map<String, Object> cacheMap = locations.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> GPS_LOCATION_PREFIX + entry.getKey(),
                            Map.Entry::getValue
                    ));

            return redisTemplate.opsForValue()
                    .multiSet(cacheMap)
                    .doOnNext(success -> log.debug("Batch cached {} vehicle locations - success: {}",
                            locations.size(), success))
                    .doOnError(error -> log.error("Failed to batch cache vehicle locations", error));
        }

        public Mono<Boolean> cacheVehiclesInRadius(
                tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate center,
                double radiusMeters,
                List<String> vehicleIds) {

            String key = VEHICLES_IN_RADIUS_PREFIX +
                    center.getLatitude() + ":" + center.getLongitude() + ":" + radiusMeters;

            VehiclesInRadiusCache radiusCache = new VehiclesInRadiusCache(
                    center.getLatitude(),
                    center.getLongitude(),
                    radiusMeters,
                    vehicleIds,
                    Instant.now()
            );

            return redisTemplate.opsForValue()
                    .set(key, radiusCache, Duration.ofSeconds(30))
                    .doOnNext(success -> log.debug("Cached {} vehicles in radius - success: {}",
                            vehicleIds.size(), success));
        }

        public Mono<VehiclesInRadiusCache> getCachedVehiclesInRadius(
                tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate center,
                double radiusMeters) {

            String key = VEHICLES_IN_RADIUS_PREFIX +
                    center.getLatitude() + ":" + center.getLongitude() + ":" + radiusMeters;

            return redisTemplate.opsForValue()
                    .get(key)
                    .cast(VehiclesInRadiusCache.class);
        }

        public Flux<VehicleLocationCache> streamAllCachedLocations() {
            String pattern = GPS_LOCATION_PREFIX + "*";

            return redisTemplate.keys(pattern)
                    .flatMap(key -> redisTemplate.opsForValue().get(key))
                    .cast(VehicleLocationCache.class)
                    .doOnNext(cache -> log.trace("Streamed cached location: {}", cache.vehicleId()));
        }

        public Mono<Long> clearStaleEntries() {
            String pattern = "ugur:*";

            return redisTemplate.keys(pattern)
                    .filterWhen(key -> redisTemplate.getExpire(key)
                            .map(expire -> expire.getSeconds() < 0))
                    .collectList()
                    .flatMap(expiredKeys -> {
                        if (expiredKeys.isEmpty()) {
                            return Mono.just(0L);
                        }
                        log.info("Clearing {} stale cache entries", expiredKeys.size());
                        return redisTemplate.delete(expiredKeys.toArray(new String[0]));
                    });
        }

        public Mono<CacheStats> getCacheStats() {
            return Flux.just(
                            GPS_LOCATION_PREFIX + "*",
                            VEHICLE_STATUS_PREFIX + "*",
                            VEHICLES_IN_RADIUS_PREFIX + "*",
                            ROUTE_ASSIGNMENT_PREFIX + "*"
                    )
                    .flatMap(pattern -> redisTemplate.keys(pattern).count())
                    .collectList()
                    .map(counts -> new CacheStats(
                            counts.getFirst(),
                            counts.get(1),
                            counts.get(2),
                            counts.get(3)
                    ))
                    .doOnNext(stats -> log.info("Reactive cache stats: locations={}, statuses={}, radius={}, routes={}",
                            stats.locationCacheCount(), stats.statusCacheCount(),
                            stats.radiusCacheCount(), stats.routeAssignmentCacheCount()));
        }

        public record VehicleLocationCache(
                String vehicleId,
                double latitude,
                double longitude,
                double accuracy,
                double speedMs,
                Instant timestamp
        ) {
            public boolean isStale(Duration maxAge) {
                return Duration.between(timestamp, Instant.now()).compareTo(maxAge) > 0;
            }
        }

        public record VehiclesInRadiusCache(
                double centerLatitude,
                double centerLongitude,
                double radiusMeters,
                List<String> vehicleIds,
                Instant timestamp
        ) {}

        public record CacheStats(
                long locationCacheCount,
                long statusCacheCount,
                long radiusCacheCount,
                long routeAssignmentCacheCount
        ) {
            public long totalCacheCount() {
                return locationCacheCount + statusCacheCount + radiusCacheCount + routeAssignmentCacheCount;
            }
        }
    }

    public static class ReactiveVehicleCacheService {

        private final ReactiveRedisTemplate<String, Object> redisTemplate;
        private final CacheProperties cacheProps;

        private static final String VEHICLE_PREFIX = "ugur:vehicle:";
        private static final String VEHICLE_STATUS_PREFIX = "ugur:vehicle:status:";

        public ReactiveVehicleCacheService(ReactiveRedisTemplate<String, Object> redisTemplate,
                                           CacheProperties cacheProps) {
            this.redisTemplate = redisTemplate;
            this.cacheProps = cacheProps;
        }

        public Mono<Boolean> cacheVehicle(String vehicleId, Object vehicle) {
            String key = VEHICLE_PREFIX + vehicleId;

            return redisTemplate.opsForValue()
                    .set(key, vehicle, cacheProps.getVehicles().getTtl())
                    .doOnNext(success -> log.debug("Cached vehicle: {} - success: {}", vehicleId, success));
        }

        public Mono<Object> getCachedVehicle(String vehicleId) {
            String key = VEHICLE_PREFIX + vehicleId;

            return redisTemplate.opsForValue()
                    .get(key)
                    .doOnNext(vehicle -> log.debug("Retrieved cached vehicle: {}", vehicleId));
        }

        public Mono<Boolean> cacheVehicleStatus(String vehicleId, String status, String movementStatus) {
            String key = VEHICLE_STATUS_PREFIX + vehicleId;

            VehicleStatusCache statusCache = new VehicleStatusCache(
                    vehicleId, status, movementStatus, Instant.now()
            );

            return redisTemplate.opsForValue()
                    .set(key, statusCache, Duration.ofMinutes(2))
                    .doOnNext(success -> log.debug("Cached vehicle status: {} - {} - success: {}",
                            vehicleId, status, success));
        }

        public Flux<VehicleStatusCache> streamVehiclesByStatus(String status) {
            String pattern = VEHICLE_STATUS_PREFIX + "*";

            return redisTemplate.keys(pattern)
                    .flatMap(key -> redisTemplate.opsForValue().get(key))
                    .cast(VehicleStatusCache.class)
                    .filter(cache -> status.equals(cache.status()));
        }

        public record VehicleStatusCache(
                String vehicleId,
                String status,
                String movementStatus,
                Instant timestamp
        ) {}
    }
}