package tm.ugur.ugur_v3.application.vehicleManagement.commands.results;

import lombok.Getter;
import tm.ugur.ugur_v3.application.shared.commands.CommandResult;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.handlers.ProcessGpsDataHandler.ProviderProcessingResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ProcessGpsDataResult implements CommandResult {

    private final boolean successful;
    private final int successfulUpdates;
    private final int failedUpdates;
    private final List<String> processedProviders;
    private final List<String> errors;
    private final Duration processingTime;
    private final Instant processedAt;
    private final Map<String, ProviderStatistics> providerStatistics;

    private ProcessGpsDataResult(boolean successful,
                                 int successfulUpdates,
                                 int failedUpdates,
                                 List<String> processedProviders,
                                 List<String> errors,
                                 Duration processingTime,
                                 Map<String, ProviderStatistics> providerStatistics) {
        this.successful = successful;
        this.successfulUpdates = successfulUpdates;
        this.failedUpdates = failedUpdates;
        this.processedProviders = List.copyOf(processedProviders);
        this.errors = List.copyOf(errors);
        this.processingTime = processingTime;
        this.processedAt = Instant.now();
        this.providerStatistics = Map.copyOf(providerStatistics);
    }

    public static ProcessGpsDataResult empty() {
        return new ProcessGpsDataResult(
                true,
                0,
                0,
                List.of(),
                List.of(),
                Duration.ZERO,
                Map.of()
        );
    }

    public static ProcessGpsDataResult noProviders(List<String> requestedProviders) {
        return new ProcessGpsDataResult(
                false,
                0,
                0,
                List.of(),
                List.of("No active providers found for: " + String.join(", ", requestedProviders)),
                Duration.ZERO,
                Map.of()
        );
    }

    public static ProcessGpsDataResult fromProviderResults(List<ProviderProcessingResult> providerResults,
                                                           Duration processingTime) {
        int totalSuccessful = providerResults.stream()
                .mapToInt(ProviderProcessingResult::successfulUpdates)
                .sum();

        int totalFailed = providerResults.stream()
                .mapToInt(ProviderProcessingResult::failedUpdates)
                .sum();

        List<String> allErrors = providerResults.stream()
                .flatMap(result -> result.errors().stream())
                .toList();

        List<String> processedProviders = providerResults.stream()
                .map(ProviderProcessingResult::providerName)
                .toList();

        Map<String, ProviderStatistics> statistics = providerResults.stream()
                .collect(Collectors.toMap(
                        ProviderProcessingResult::providerName,
                        result -> new ProviderStatistics(
                                result.successfulUpdates(),
                                result.failedUpdates(),
                                result.errors().size(),
                                calculateSuccessRate(result.successfulUpdates(), result.failedUpdates())
                        )
                ));

        boolean successful = totalFailed == 0 && !providerResults.isEmpty();

        return new ProcessGpsDataResult(
                successful,
                totalSuccessful,
                totalFailed,
                processedProviders,
                allErrors,
                processingTime,
                statistics
        );
    }

    public ProcessGpsDataResult merge(ProcessGpsDataResult other) {
        List<String> allProviders = new ArrayList<>(this.processedProviders);
        allProviders.addAll(other.processedProviders);

        List<String> allErrors = new ArrayList<>(this.errors);
        allErrors.addAll(other.errors);

        Map<String, ProviderStatistics> combinedStatistics = new java.util.HashMap<>(this.providerStatistics);
        combinedStatistics.putAll(other.providerStatistics);

        Duration maxProcessingTime = this.processingTime.compareTo(other.processingTime) > 0
                ? this.processingTime
                : other.processingTime;

        boolean combinedSuccess = this.successful && other.successful;

        return new ProcessGpsDataResult(
                combinedSuccess,
                this.successfulUpdates + other.successfulUpdates,
                this.failedUpdates + other.failedUpdates,
                allProviders,
                allErrors,
                maxProcessingTime,
                combinedStatistics
        );
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return null;
        }
        return errors.size() == 1 ? errors.getFirst() :
                String.format("%d errors occurred: %s", errors.size(), String.join("; ", errors));
    }

    public String getResultSummary() {
        return String.format(
                "GPS processing completed: %d successful, %d failed updates from %d providers in %dms",
                successfulUpdates,
                failedUpdates,
                processedProviders.size(),
                processingTime.toMillis()
        );
    }

    public int getTotalUpdates() {
        return successfulUpdates + failedUpdates;
    }

    public int getErrorCount() {
        return errors.size();
    }

    public double getOverallSuccessRate() {
        return calculateSuccessRate(successfulUpdates, failedUpdates);
    }

    public boolean hasErrors() {
        return !errors.isEmpty() || failedUpdates > 0;
    }

    public ProviderStatistics getProviderStatistics(String providerName) {
        return providerStatistics.get(providerName);
    }

    private static double calculateSuccessRate(int successful, int failed) {
        int total = successful + failed;
        return total > 0 ? (double) successful / total * 100.0 : 0.0;
    }

    @Override
    public String toString() {
        return String.format(
                "ProcessGpsDataResult{successful=%s, updates=%d/%d, providers=%s, processingTime=%dms}",
                successful,
                successfulUpdates,
                getTotalUpdates(),
                processedProviders,
                processingTime.toMillis()
        );
    }

    public record ProviderStatistics(
            int successfulUpdates,
            int failedUpdates,
            int errorCount,
            double successRate
    ) {
        public int getTotalUpdates() {
            return successfulUpdates + failedUpdates;
        }

        public boolean isHealthy() {
            return successRate >= 90.0 && errorCount == 0;
        }
    }
}