package tm.ugur.ugur_v3.infrastructure.external.gps.resilience;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider;

import java.util.List;

@Component
public class FallbackStrategy {

    public Flux<GpsDataProvider.GpsLocationData> fallbackToCache(String providerName, Throwable error) {
        return cacheService.getLastKnownLocations()
                .doOnSubscribe(s -> log.warn("Using fallback cache for {} due to: {}",
                        providerName, error.getMessage()));
    }

    public Flux<GpsDataProvider.GpsLocationData> fallbackToAlternativeProvider(String failedProvider,
                                                                               List<GpsDataProvider> alternatives) {
        return Flux.fromIterable(alternatives)
                .filter(provider -> !provider.getProviderName().equals(failedProvider))
                .flatMap(GpsDataProvider::getVehicleLocations);
    }
}