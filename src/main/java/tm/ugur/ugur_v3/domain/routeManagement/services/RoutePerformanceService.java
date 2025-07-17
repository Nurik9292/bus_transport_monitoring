package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.List;

public interface RoutePerformanceService extends DomainService {

    Mono<ComprehensivePerformanceReport> calculatePerformanceMetrics(Route route,
                                                                     PerformanceDataCollection dataCollection,
                                                                     AnalysisPeriod period);

    Mono<BenchmarkComparison> compareWithBenchmarks(Route route,
                                                    PerformanceMetrics currentMetrics,
                                                    IndustryBenchmarks benchmarks);

    Mono<PerformanceForecast> forecastPerformance(Route route,
                                                  HistoricalPerformanceData historicalData,
                                                  ForecastParameters parameters);

    Flux<PerformanceAnomaly> detectPerformanceAnomalies(Route route,
                                                        RealTimePerformanceData realTimeData,
                                                        PerformanceBaseline baseline);

    Mono<List<PerformanceRecommendation>> generateImprovementRecommendations(Route route,
                                                                             PerformanceAnalysisResult analysis,
                                                                             ConstraintsAndGoals constraints);

    Mono<PerformanceReport> generatePerformanceReport(Route route,
                                                      ReportConfiguration configuration,
                                                      Timestamp reportDate);


    record ComprehensivePerformanceReport(
            RouteId routeId,
            OverallPerformanceScore overallScore,
            Map<RoutePerformanceMetric, Double> metricValues,
            PerformanceTrends trends,
            List<KeyInsight> insights,
            PerformanceGrading grading
    ) {}

    record BenchmarkComparison(
            Map<RoutePerformanceMetric, BenchmarkResult> comparisons,
            double overallRanking,
            List<String> strengthAreas,
            List<String> improvementAreas,
            CompetitivePosition position
    ) {}

    record PerformanceForecast(
            Map<RoutePerformanceMetric, ForecastValue> predictions,
            ConfidenceInterval confidenceInterval,
            List<ForecastAssumption> assumptions,
            RiskFactors riskFactors
    ) {}

    record PerformanceAnomaly(
            RoutePerformanceMetric affectedMetric,
            double currentValue,
            double expectedValue,
            double deviationPercentage,
            AnomalySeverity severity,
            List<String> possibleCauses,
            Timestamp detectedAt
    ) {}

    record PerformanceRecommendation(
            String recommendationType,
            String description,
            double expectedImprovement,
            ImplementationComplexity complexity,
            double estimatedCost,
            int priorityScore,
            List<String> actionSteps
    ) {}

    enum AnomalySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    enum ImplementationComplexity {
        SIMPLE, MODERATE, COMPLEX, VERY_COMPLEX
    }

    enum CompetitivePosition {
        LEADING, ABOVE_AVERAGE, AVERAGE, BELOW_AVERAGE, LAGGING
    }
}