package tm.ugur.ugur_v3.infrastructure.external.gps.adapters;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider;

import java.util.List;

@Component
public class UniversalGpsAdapter implements GpsDataProvider {
    @Override
    public Flux<GpsLocationData> getVehicleLocations() {
        return null;
    }

    @Override
    public String getProviderName() {
        return "";
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return null;
    }

    @Override
    public Mono<ProviderHealthStatus> getHealthStatus() {
        return null;
    }

//    private final List<GpsDataProvider> providers;
//    private final GpsProviderHealthMonitor healthMonitor;
//    private final GpsLoadBalancer loadBalancer;
//
//    public UniversalGpsAdapter(List<GpsDataProvider> providers,
//                               GpsProviderHealthMonitor healthMonitor,
//                               GpsLoadBalancer loadBalancer) {
//        this.providers = providers.stream()
//                .filter(p -> !(p instanceof UniversalGpsAdapter))
//                .toList();
//        this.healthMonitor = healthMonitor;
//        this.loadBalancer = loadBalancer;
//    }
//
//    @Override
//    public Flux<GpsLocationData> getVehicleLocations() {
//        return loadBalancer.selectHealthyProviders(providers)
//                .flatMap(GpsDataProvider::getVehicleLocations)
//                .distinct(data -> data.vehicleIdentifier()) // Deduplicate
//                .doOnNext(data -> healthMonitor.recordSuccessfulRequest(getProviderName()))
//                .doOnError(error -> healthMonitor.recordFailedRequest(getProviderName()));
//    }
//
//    @Override
//    public String getProviderName() {
//        return "Universal-GPS-Adapter";
//    }
//
//    @Override
//    public Mono<Boolean> isAvailable() {
//        return Flux.fromIterable(providers)
//                .flatMap(GpsDataProvider::isAvailable)
//                .any(available -> available);
//    }
//
//    @Override
//    public Mono<ProviderHealthStatus> getHealthStatus() {
//        return healthMonitor.getAggregatedHealth(providers);
//    }
}