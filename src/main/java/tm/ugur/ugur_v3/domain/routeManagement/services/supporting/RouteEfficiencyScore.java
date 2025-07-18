package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;


/**
 * Оценка эффективности маршрута
 * TODO: Реализовать в Infrastructure слое при интеграции с аналитикой
 */
public record RouteEfficiencyScore(
        RouteId routeId,
        double overallScore,
        double timeEfficiency,
        double costEfficiency,
        double passengerSatisfaction,
        double environmentalImpact,
        EfficiencyGrade grade
) {

    public enum EfficiencyGrade {
        A_EXCELLENT(90),
        B_GOOD(80),
        C_AVERAGE(70),
        D_BELOW_AVERAGE(60),
        F_POOR(0);

        private final int minScore;

        EfficiencyGrade(int minScore) {
            this.minScore = minScore;
        }

        public static EfficiencyGrade fromScore(double score) {
            if (score >= 90) return A_EXCELLENT;
            if (score >= 80) return B_GOOD;
            if (score >= 70) return C_AVERAGE;
            if (score >= 60) return D_BELOW_AVERAGE;
            return F_POOR;
        }
    }

    public static RouteEfficiencyScore calculate(RouteId routeId, double time, double cost, double satisfaction) {
        double overall = (time + cost + satisfaction) / 3.0;
        return new RouteEfficiencyScore(
                routeId,
                overall,
                time,
                cost,
                satisfaction,
                75.0, // default environmental score
                EfficiencyGrade.fromScore(overall)
        );
    }

    public boolean needsImprovement() {
        return overallScore < 70.0;
    }
}