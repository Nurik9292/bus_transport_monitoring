package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record TransferOptimizationResult(
        Map<RouteId, List<LocalTime>> synchronizedDepartures,
        double averageTransferWaitTime,
        int improvedConnections,
        PassengerBenefitEstimate benefitEstimate
) {}
