package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import java.util.List;

public record ScheduleEfficiencyAnalysis(
        double adherenceScore,
        double passengerSatisfactionScore,
        double resourceUtilizationScore,
        List<EfficiencyRecommendation> recommendations
) {}
