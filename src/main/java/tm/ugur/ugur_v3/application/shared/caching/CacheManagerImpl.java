package tm.ugur.ugur_v3.application.shared.caching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheManagerImpl implements CacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    private final Map<String, CachePolicy> namespacePolicies = new ConcurrentHashMap<>();
    private final Map<String, CacheMetricsImpl> namespaceMetrics = new ConcurrentHashMap<>();


    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(CacheKey key, Supplier<T> fallback, Duration ttl) {
        try {
            Object cached = redisTemplate.opsForValue().get(key.getFullKey());

            if (cached != null) {
                hitCount.incrementAndGet();
                updateNamespaceMetrics(key.getNamespace(), true);
                log.trace("Cache HIT: {}", key.getFullKey());
                return (T) cached;
            }


            missCount.incrementAndGet();
            updateNamespaceMetrics(key.getNamespace(), false);
            log.trace("Cache MISS: {}", key.getFullKey());

            T value = fallback.get();
            if (value != null) {
                put(key, value, ttl);
            }

            return value;

        } catch (Exception e) {
            log.warn("Cache error for key {}: {}", key.getFullKey(), e.getMessage());

            return fallback.get();
        }
    }

    @Override
    public void put(CacheKey key, Object value, Duration ttl) {
        try {
            if (value == null) {
                log.debug("Skipping cache put for null value: {}", key.getFullKey());
                return;
            }

            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key.getFullKey(), value, ttl);
            } else {
                redisTemplate.opsForValue().set(key.getFullKey(), value);
            }

            log.trace("Cached value for key: {} (TTL: {})", key.getFullKey(), ttl);

        } catch (Exception e) {
            log.warn("Failed to cache value for key {}: {}", key.getFullKey(), e.getMessage());

        }
    }

    @Override
    public void evict(CacheKey key) {
        try {
            Boolean deleted = redisTemplate.delete(key.getFullKey());
            if (Boolean.TRUE.equals(deleted)) {
                evictionCount.incrementAndGet();
                log.trace("Evicted cache key: {}", key.getFullKey());
            }
        } catch (Exception e) {
            log.warn("Failed to evict cache key {}: {}", key.getFullKey(), e.getMessage());
        }
    }

    @Override
    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (!keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                evictionCount.addAndGet(deleted);
                log.debug("Evicted {} keys matching pattern: {}", deleted, pattern);
            }
        } catch (Exception e) {
            log.warn("Failed to evict by pattern {}: {}", pattern, e.getMessage());
        }
    }

    @Override
    public boolean exists(CacheKey key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key.getFullKey()));
        } catch (Exception e) {
            log.warn("Failed to check existence of key {}: {}", key.getFullKey(), e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Duration> getTtl(CacheKey key) {
        try {
            long ttlSeconds = redisTemplate.getExpire(key.getFullKey());
            if (ttlSeconds > 0) {
                return Optional.of(Duration.ofSeconds(ttlSeconds));
            }
        } catch (Exception e) {
            log.warn("Failed to get TTL for key {}: {}", key.getFullKey(), e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean extendTtl(CacheKey key, Duration additionalTtl) {
        try {
            Optional<Duration> currentTtl = getTtl(key);
            if (currentTtl.isPresent()) {
                Duration newTtl = currentTtl.get().plus(additionalTtl);
                return Boolean.TRUE.equals(redisTemplate.expire(key.getFullKey(), newTtl));
            }
        } catch (Exception e) {
            log.warn("Failed to extend TTL for key {}: {}", key.getFullKey(), e.getMessage());
        }
        return false;
    }



    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> getAsync(CacheKey key, Supplier<CompletableFuture<T>> fallback, Duration ttl) {
        return reactiveRedisTemplate.opsForValue()
                .get(key.getFullKey())
                .cast(Object.class)
                .map(cached -> {
                    hitCount.incrementAndGet();
                    updateNamespaceMetrics(key.getNamespace(), true);
                    return (T) cached;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    missCount.incrementAndGet();
                    updateNamespaceMetrics(key.getNamespace(), false);

                    return Mono.fromFuture(fallback.get())
                            .doOnNext(value -> {
                                if (value != null) {
                                    putAsync(key, value, ttl);
                                }
                            });
                }))
                .onErrorResume(error -> {
                    log.warn("Async cache error for key {}: {}", key.getFullKey(), error.getMessage());
                    return Mono.fromFuture(fallback.get());
                })
                .toFuture();
    }

    @Override
    public CompletableFuture<Void> putAsync(CacheKey key, Object value, Duration ttl) {
        if (value == null) {
            return CompletableFuture.completedFuture(null);
        }

        Mono<Boolean> putOperation;
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            putOperation = reactiveRedisTemplate.opsForValue().set(key.getFullKey(), value, ttl);
        } else {
            putOperation = reactiveRedisTemplate.opsForValue().set(key.getFullKey(), value);
        }

        return putOperation
                .doOnSuccess(success -> log.trace("Async cached value for key: {}", key.getFullKey()))
                .doOnError(error -> log.warn("Failed to async cache key {}: {}", key.getFullKey(), error.getMessage()))
                .onErrorReturn(false)
                .then()
                .toFuture();
    }



    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<CacheKey, T> getBatch(List<CacheKey> keys, Function<List<CacheKey>, Map<CacheKey, T>> fallback, Duration ttl) {
        try {
            List<String> stringKeys = keys.stream()
                    .map(CacheKey::getFullKey)
                    .collect(Collectors.toList());

            List<Object> cached = redisTemplate.opsForValue().multiGet(stringKeys);
            Map<CacheKey, T> result = new HashMap<>();
            List<CacheKey> missedKeys = new ArrayList<>();

            for (int i = 0; i < keys.size(); i++) {
                CacheKey key = keys.get(i);
                Object value = cached != null && i < cached.size() ? cached.get(i) : null;

                if (value != null) {
                    result.put(key, (T) value);
                    hitCount.incrementAndGet();
                } else {
                    missedKeys.add(key);
                    missCount.incrementAndGet();
                }
            }


            if (!missedKeys.isEmpty()) {
                Map<CacheKey, T> fallbackResults = fallback.apply(missedKeys);
                result.putAll(fallbackResults);


                putBatch(fallbackResults.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> (Object) entry.getValue()
                        )), ttl);
            }

            return result;

        } catch (Exception e) {
            log.warn("Batch cache operation failed: {}", e.getMessage());
            return fallback.apply(keys);
        }
    }

    @Override
    public void putBatch(Map<CacheKey, Object> entries, Duration ttl) {
        try {
            Map<String, Object> stringKeyEntries = entries.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().getFullKey(),
                            Map.Entry::getValue
                    ));

            if (stringKeyEntries.isEmpty()) {
                return;
            }

            redisTemplate.opsForValue().multiSet(stringKeyEntries);


            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                stringKeyEntries.keySet().forEach(key ->
                        redisTemplate.expire(key, ttl));
            }

            log.trace("Batch cached {} entries", stringKeyEntries.size());

        } catch (Exception e) {
            log.warn("Failed to batch cache entries: {}", e.getMessage());
        }
    }

    @Override
    public void evictBatch(List<CacheKey> keys) {
        try {
            List<String> stringKeys = keys.stream()
                    .map(CacheKey::getFullKey)
                    .collect(Collectors.toList());

            Long deleted = redisTemplate.delete(stringKeys);
            evictionCount.addAndGet(deleted);
            log.trace("Batch evicted {} keys", deleted);

        } catch (Exception e) {
            log.warn("Failed to batch evict keys: {}", e.getMessage());
        }
    }



    @Override
    public CompletableFuture<CacheWarmingResult> warmCache(CacheWarmingStrategy warmingStrategy) {
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();
            List<String> errors = new ArrayList<>();
            AtomicLong loadedCount = new AtomicLong(0);
            AtomicLong failedCount = new AtomicLong(0);

            try {
                List<CacheKey> keysToWarm = warmingStrategy.getKeysToWarm();
                Map<CacheKey, Object> data = warmingStrategy.loadData(keysToWarm);

                putBatch(data, warmingStrategy.getTtl());
                loadedCount.set(data.size());

            } catch (Exception e) {
                log.error("Cache warming failed: {}", e.getMessage(), e);
                errors.add(e.getMessage());
                failedCount.set(warmingStrategy.getKeysToWarm().size());
            }

            Duration executionTime = Duration.between(start, Instant.now());

            return new CacheWarmingResultImpl(
                    loadedCount.get(),
                    failedCount.get(),
                    executionTime,
                    errors,
                    errors.isEmpty()
            );
        });
    }

    @Override
    public CacheMetrics getMetrics() {
        return new CacheMetricsImpl(
                hitCount.get(),
                missCount.get(),
                hitCount.get() + missCount.get() > 0 ?
                        (double) hitCount.get() / (hitCount.get() + missCount.get()) : 0.0,
                Duration.ofMillis(1),
                evictionCount.get(),
                0L,
                0L,
                Map.of("get", hitCount.get() + missCount.get(), "evict", evictionCount.get())
        );
    }

    @Override
    public CacheMetrics getMetrics(String namespace) {
        return namespaceMetrics.computeIfAbsent(namespace,
                k -> new CacheMetricsImpl(0, 0, 0.0, Duration.ZERO, 0, 0, 0, Map.of()));
    }

    @Override
    public void resetMetrics() {
        hitCount.set(0);
        missCount.set(0);
        evictionCount.set(0);
        namespaceMetrics.clear();
    }

    @Override
    public void configureCachePolicy(String namespace, CachePolicy policy) {
        namespacePolicies.put(namespace, policy);
        log.info("Configured cache policy for namespace: {}", namespace);
    }

    @Override
    public CachePolicy getCachePolicy(String namespace) {
        return namespacePolicies.get(namespace);
    }

    @Override
    public CacheHealthCheck performHealthCheck() {
        Instant checkTime = Instant.now();
        List<String> issues = new ArrayList<>();
        boolean isHealthy = true;
        Duration responseTime = Duration.ZERO;

        try {
            Instant start = Instant.now();


            String testKey = "health:check:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "test", Duration.ofSeconds(5));
            String result = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);

            responseTime = Duration.between(start, Instant.now());

            if (!"test".equals(result)) {
                issues.add("Basic read/write test failed");
                isHealthy = false;
            }

            if (responseTime.toMillis() > 100) {
                issues.add("High response time: " + responseTime.toMillis() + "ms");
            }

        } catch (Exception e) {
            issues.add("Health check failed: " + e.getMessage());
            isHealthy = false;
        }

        return new CacheHealthCheckImpl(isHealthy, responseTime,
                isHealthy ? 1.0 : 0.0, issues, checkTime);
    }

    @Override
    public void createNamespace(String namespace, NamespaceConfig config) {
        configureCachePolicy(namespace, config.getCachePolicy());
        log.info("Created namespace: {} with config: {}", namespace, config);
    }

    @Override
    public void dropNamespace(String namespace) {
        evictByPattern(namespace + ":*");
        namespacePolicies.remove(namespace);
        namespaceMetrics.remove(namespace);
        log.info("Dropped namespace: {}", namespace);
    }

    @Override
    public Set<String> getActiveNamespaces() {
        return new HashSet<>(namespacePolicies.keySet());
    }



    private void updateNamespaceMetrics(String namespace, boolean hit) {
        namespaceMetrics.computeIfAbsent(namespace,
                        k -> new CacheMetricsImpl(0, 0, 0.0, Duration.ZERO, 0, 0, 0, Map.of()))
                .updateMetrics(hit);
    }


    private record CacheWarmingResultImpl(long loadedKeys, long failedKeys, Duration executionTime, List<String> errors,
                                          boolean successful) implements CacheWarmingResult {
            private CacheWarmingResultImpl(long loadedKeys, long failedKeys,
                                           Duration executionTime, List<String> errors, boolean successful) {
                this.loadedKeys = loadedKeys;
                this.failedKeys = failedKeys;
                this.executionTime = executionTime;
                this.errors = List.copyOf(errors);
                this.successful = successful;
            }
        }

    private static class CacheMetricsImpl implements CacheMetrics {
        private final AtomicLong hitCount = new AtomicLong();
        private final AtomicLong missCount = new AtomicLong();
        private final AtomicLong evictionCount = new AtomicLong();
        private final Duration averageAccessTime;
        private final long currentSize;
        private final long usedMemory;
        private final Map<String, Long> operationCounts;

        public CacheMetricsImpl(long hits, long misses, double hitRate,
                                Duration avgTime, long evictions, long size, long memory,
                                Map<String, Long> operations) {
            this.hitCount.set(hits);
            this.missCount.set(misses);
            this.evictionCount.set(evictions);
            this.averageAccessTime = avgTime;
            this.currentSize = size;
            this.usedMemory = memory;
            this.operationCounts = Map.copyOf(operations);
        }

        public void updateMetrics(boolean hit) {
            if (hit) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }
        }

        @Override public long getHitCount() { return hitCount.get(); }
        @Override public long getMissCount() { return missCount.get(); }
        @Override public double getHitRate() {
            long total = hitCount.get() + missCount.get();
            return total > 0 ? (double) hitCount.get() / total : 0.0;
        }
        @Override public Duration getAverageAccessTime() { return averageAccessTime; }
        @Override public long getEvictionCount() { return evictionCount.get(); }
        @Override public long getCurrentSize() { return currentSize; }
        @Override public long getUsedMemory() { return usedMemory; }
        @Override public Map<String, Long> getOperationCounts() { return operationCounts; }
    }

    private record CacheHealthCheckImpl(boolean healthy, Duration responseTime, double availability,
                                        List<String> issues, Instant checkTime) implements CacheHealthCheck {
            private CacheHealthCheckImpl(boolean healthy, Duration responseTime,
                                         double availability, List<String> issues, Instant checkTime) {
                this.healthy = healthy;
                this.responseTime = responseTime;
                this.availability = availability;
                this.issues = List.copyOf(issues);
                this.checkTime = checkTime;
            }
        }
}