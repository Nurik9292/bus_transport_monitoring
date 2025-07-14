package tm.ugur.ugur_v3.infrastructure.external.gps.dto;

public record ProviderStatusDto(
        String providerName,
        boolean isHealthy,
        String status,
        java.time.Duration responseTime,
        double successRate,
        int totalRequests,
        int failedRequests,
        java.time.Instant lastCheck,
        java.util.Map<String, Object> metrics
) {}