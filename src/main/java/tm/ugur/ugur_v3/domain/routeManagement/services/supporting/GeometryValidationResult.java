package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import java.util.List;

public record GeometryValidationResult(
        boolean isGeometryValid,
        List<GeometryIssue> geometryIssues,
        RouteConnectivity connectivity,
        DistanceValidation distanceValidation
) {}