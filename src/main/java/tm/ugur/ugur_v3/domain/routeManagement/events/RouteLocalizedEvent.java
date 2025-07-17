package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.Map;

@Getter
public final class RouteLocalizedEvent extends BaseRouteEvent {

    private final String languageCode;
    private final String localizedName;
    private final String localizedDescription;
    private final String updatedBy;

    private RouteLocalizedEvent(RouteId routeId, String languageCode, String localizedName,
                                String localizedDescription, String updatedBy,
                                String correlationId, Map<String, Object> metadata) {
        super("RouteLocalized", routeId, correlationId, metadata);
        this.languageCode = languageCode;
        this.localizedName = localizedName;
        this.localizedDescription = localizedDescription;
        this.updatedBy = updatedBy;
    }

    public static RouteLocalizedEvent of(RouteId routeId, String languageCode, String localizedName,
                                         String localizedDescription, String updatedBy) {
        return new RouteLocalizedEvent(routeId, languageCode, localizedName, localizedDescription,
                updatedBy, null, null);
    }

    @Override
    public String toString() {
        return String.format("RouteLocalizedEvent{routeId=%s, lang='%s', name='%s'}",
                routeId, languageCode, localizedName);
    }
}
