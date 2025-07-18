package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.services.supporting.*;
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

}