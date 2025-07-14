package tm.ugur.ugur_v3.domain.vehicleManagement.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;

public interface GpsDataProvider extends DomainService {

    Flux<GpsLocationData> getVehicleLocations();

    String getProviderName();

    Mono<Boolean> isAvailable();

    Mono<ProviderHealthStatus> getHealthStatus();

    @Override
    default String getServiceDescription() {
        return "GPS Data Provider: " + getProviderName();
    }


    record GpsLocationData(
            String vehicleIdentifier,
            double latitude,
            double longitude,
            double accuracy,
            Double speed,
            Double bearing,
            java.time.Instant timestamp,
            java.util.Map<String, Object> metadata
    ) {}

    record ProviderHealthStatus(
            String providerName,
            boolean isHealthy,
            String status,
            java.time.Duration responseTime,
            int successfulRequests,
            int failedRequests,
            java.time.Instant lastCheck
    ) {}
}