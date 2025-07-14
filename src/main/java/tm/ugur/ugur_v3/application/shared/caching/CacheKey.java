package tm.ugur.ugur_v3.application.shared.caching;

import java.util.Objects;
import java.util.regex.Pattern;

public final class CacheKey {

    private static final String SEPARATOR = ":";
    private static final Pattern VALID_COMPONENT = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int MAX_KEY_LENGTH = 250;

    private final String namespace;
    private final String entity;
    private final String identifier;
    private final String suffix;
    private final String fullKey;
    private final int hashCode;


    private CacheKey(String namespace, String entity, String identifier, String suffix) {
        this.namespace = validateComponent(namespace, "namespace");
        this.entity = validateComponent(entity, "entity");
        this.identifier = validateComponent(identifier, "identifier");
        this.suffix = suffix != null ? validateComponent(suffix, "suffix") : "";

        this.fullKey = buildFullKey();
        this.hashCode = fullKey.hashCode();

        if (fullKey.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Cache key too long: " + fullKey.length() + " chars. Max: " + MAX_KEY_LENGTH
            );
        }
    }

    public static CacheKey of(String namespace, String entity, String identifier) {
        return new CacheKey(namespace, entity, identifier, null);
    }

    public static CacheKey of(String namespace, String entity, String identifier, String suffix) {
        return new CacheKey(namespace, entity, identifier, suffix);
    }

    public static class Vehicle {

        public static CacheKey location(String vehicleId) {
            return of("vehicle", "location", vehicleId, "current");
        }

        public static CacheKey locationHistory(String vehicleId) {
            return of("vehicle", "location", vehicleId, "history");
        }

        public static CacheKey status(String vehicleId) {
            return of("vehicle", "status", vehicleId);
        }

        public static CacheKey metadata(String vehicleId) {
            return of("vehicle", "metadata", vehicleId);
        }

        public static CacheKey routeAssignment(String vehicleId) {
            return of("vehicle", "route", vehicleId, "assignment");
        }
    }

    public static class Route {

        public static CacheKey eta(String routeId, String stopId) {
            return of("route", "eta", routeId, stopId);
        }

        public static CacheKey metadata(String routeId) {
            return of("route", "metadata", routeId);
        }

        public static CacheKey stops(String routeId) {
            return of("route", "stops", routeId);
        }

        public static CacheKey schedule(String routeId) {
            return of("route", "schedule", routeId);
        }

        public static CacheKey trafficDelays(String routeId) {
            return of("route", "traffic", routeId, "delays");
        }
    }

    public static class User {

        public static CacheKey preferences(String userId) {
            return of("user", "preferences", userId);
        }

        public static CacheKey favoriteRoutes(String userId) {
            return of("user", "favorites", userId, "routes");
        }

        public static CacheKey notificationSettings(String userId) {
            return of("user", "notifications", userId, "settings");
        }

        public static CacheKey session(String sessionId) {
            return of("user", "session", sessionId);
        }
    }

    public static class Analytics {

        public static CacheKey dailyMetrics(String date) {
            return of("analytics", "metrics", date, "daily");
        }

        public static CacheKey routePerformance(String routeId, String period) {
            return of("analytics", "performance", routeId, period);
        }

        public static CacheKey systemHealth() {
            return of("analytics", "system", "health", "current");
        }
    }

    public CacheKey withSuffix(String newSuffix) {
        return new CacheKey(namespace, entity, identifier, newSuffix);
    }

    public CacheKey withoutSuffix() {
        return new CacheKey(namespace, entity, identifier, null);
    }

    public String toPattern() {
        if (suffix != null && !suffix.isEmpty()) {
            return namespace + SEPARATOR + entity + SEPARATOR + identifier + SEPARATOR + "*";
        } else {
            return namespace + SEPARATOR + entity + SEPARATOR + "*";
        }
    }

    public String toNamespacePattern() {
        return namespace + SEPARATOR + "*";
    }

    public String toEntityPattern() {
        return namespace + SEPARATOR + entity + SEPARATOR + "*";
    }

    public boolean belongsToNamespace(String targetNamespace) {
        return namespace.equals(targetNamespace);
    }

    public boolean belongsToEntity(String targetEntity) {
        return entity.equals(targetEntity);
    }

    public static CacheKey parse(String fullKey) {
        Objects.requireNonNull(fullKey, "Full key cannot be null");

        String[] parts = fullKey.split(Pattern.quote(SEPARATOR));

        if (parts.length < 3 || parts.length > 4) {
            throw new IllegalArgumentException("Invalid cache key format: " + fullKey);
        }

        String namespace = parts[0];
        String entity = parts[1];
        String identifier = parts[2];
        String suffix = parts.length == 4 ? parts[3] : null;

        return new CacheKey(namespace, entity, identifier, suffix);
    }


    private static String validateComponent(String component, String componentName) {
        Objects.requireNonNull(component, componentName + " cannot be null");

        String trimmed = component.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(componentName + " cannot be empty");
        }

        if (!VALID_COMPONENT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    componentName + " contains invalid characters: " + trimmed +
                            ". Only alphanumeric, underscore, and hyphen allowed."
            );
        }

        if (trimmed.length() > 50) {
            throw new IllegalArgumentException(
                    componentName + " too long: " + trimmed.length() + " chars. Max: 50"
            );
        }

        return trimmed;
    }

    private String buildFullKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(namespace).append(SEPARATOR)
                .append(entity).append(SEPARATOR)
                .append(identifier);

        if (suffix != null && !suffix.isEmpty()) {
            sb.append(SEPARATOR).append(suffix);
        }

        return sb.toString();
    }

    public String getNamespace() { return namespace; }
    public String getEntity() { return entity; }
    public String getIdentifier() { return identifier; }
    public String getSuffix() { return suffix; }
    public String getFullKey() { return fullKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheKey cacheKey)) return false;
        return Objects.equals(fullKey, cacheKey.fullKey);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return fullKey;
    }
}