package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;

import java.util.List;
import java.util.Map;

public interface RouteLocalizationService extends DomainService {

    Mono<LocalizationResult> addRouteLocalization(RouteId routeId,
                                                  String languageCode,
                                                  RouteLocalizationData localizationData);

    Mono<LocalizedRouteInfo> getLocalizedRouteInfo(RouteId routeId,
                                                   String languageCode,
                                                   String fallbackLanguage);

    Mono<SynchronizationResult> synchronizeLocalizations(RouteId routeId,
                                                         List<String> targetLanguages,
                                                         LocalizationSource source);

    Mono<LocalizationValidationResult> validateLocalization(RouteId routeId,
                                                            String languageCode,
                                                            LocalizationQualityStandards standards);


    record RouteLocalizationData(
            String localizedName,
            String localizedDescription,
            Map<String, String> localizedStopNames,
            Map<String, String> localizedAnnouncements,
            LocalizationMetadata metadata
    ) {}

    record LocalizedRouteInfo(
            String routeName,
            String routeDescription,
            Map<String, String> stopNames,
            List<String> announcements,
            String languageCode,
            boolean isComplete
    ) {}

    record LocalizationResult(
            boolean success,
            String languageCode,
            int itemsLocalized,
            List<String> errors,
            LocalizationQuality quality
    ) {}

    enum LocalizationQuality {
        EXCELLENT, GOOD, ACCEPTABLE, POOR, INCOMPLETE
    }

    enum LocalizationSource {
        MANUAL, AUTOMATED_TRANSLATION, PROFESSIONAL_TRANSLATION, CROWDSOURCED
    }
}