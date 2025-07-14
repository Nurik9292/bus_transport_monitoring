package tm.ugur.ugur_v3.application.shared.caching;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public interface CacheManager {

    <T> T get(CacheKey key, Supplier<T> fallback, Duration ttl);

    <T> CompletableFuture<T> getAsync(CacheKey key, Supplier<CompletableFuture<T>> fallback, Duration ttl);

    <T> Map<CacheKey, T> getBatch(List<CacheKey> keys, Function<List<CacheKey>, Map<CacheKey, T>> fallback, Duration ttl);

    void put(CacheKey key, Object value, Duration ttl);

    CompletableFuture<Void> putAsync(CacheKey key, Object value, Duration ttl);

    void putBatch(Map<CacheKey, Object> entries, Duration ttl);

    void evict(CacheKey key);

    void evictBatch(List<CacheKey> keys);

    void evictByPattern(String pattern);

    boolean exists(CacheKey key);

    Optional<Duration> getTtl(CacheKey key);

    boolean extendTtl(CacheKey key, Duration additionalTtl);

    CompletableFuture<CacheWarmingResult> warmCache(CacheWarmingStrategy warmingStrategy);

    CacheMetrics getMetrics();

    CacheMetrics getMetrics(String namespace);

    void resetMetrics();

    void configureCachePolicy(String namespace, CachePolicy policy);

    CachePolicy getCachePolicy(String namespace);

    CacheHealthCheck performHealthCheck();

    void createNamespace(String namespace, NamespaceConfig config);

    void dropNamespace(String namespace);

    Set<String> getActiveNamespaces();

    interface CacheWarmingResult {

        long getLoadedKeys();

        long getFailedKeys();

        Duration getExecutionTime();

        List<String> getErrors();

        boolean isSuccessful();
    }

    interface CacheWarmingStrategy {

        List<CacheKey> getKeysToWarm();

        Map<CacheKey, Object> loadData(List<CacheKey> keys);

        Duration getTtl();

        int getPriority();

        int getBatchSize();
    }

    interface CacheMetrics {

        long getHitCount();

        long getMissCount();

        double getHitRate();

        Duration getAverageAccessTime();

        long getEvictionCount();

        long getCurrentSize();

        long getUsedMemory();

        Map<String, Long> getOperationCounts();
    }


    interface CachePolicy {

        Duration getDefaultTtl();

        Duration getMaxTtl();

        EvictionStrategy getEvictionStrategy();

        long getMaxKeys();

        long getMaxValueSize();

        boolean isCompressionEnabled();

        boolean isSerializationOptimized();
    }

    enum EvictionStrategy {
        LRU,
        LFU,
        FIFO,
        TTL_BASED,
        CUSTOM
    }


    interface NamespaceConfig {

        CachePolicy getCachePolicy();

        boolean isReplicationEnabled();

        ConsistencyLevel getConsistencyLevel();

        Optional<Duration> getBackupFrequency();
    }

    enum ConsistencyLevel {
        EVENTUAL,
        STRONG,
        SESSION
    }

    interface CacheHealthCheck {

        boolean isHealthy();

        Duration getResponseTime();

        double getAvailability();

        List<String> getIssues();

        java.time.Instant getCheckTime();
    }
}