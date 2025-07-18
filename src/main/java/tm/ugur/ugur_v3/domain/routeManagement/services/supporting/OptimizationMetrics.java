package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

public record OptimizationMetrics(
        double fuelSavingsPercentage,
        int timeSavingsMinutes,
        double costReductionPercentage,
        int carbonEmissionReduction
) {}