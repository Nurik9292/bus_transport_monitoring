package tm.ugur.ugur_v3.application.vehicleManagement.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import tm.ugur.ugur_v3.application.shared.commands.Command;

import java.time.Instant;
import java.util.List;

@Getter
public class ProcessGpsDataCommand implements Command {

    @NotNull
    private final List<String> providerNames;

    @NotNull
    private final Instant requestedAt;

    @NotBlank
    private final String requestedBy;

    private final boolean enableBatchProcessing;
    private final int maxConcurrentProviders;

    private ProcessGpsDataCommand(List<String> providerNames,
                                  Instant requestedAt,
                                  String requestedBy,
                                  boolean enableBatchProcessing,
                                  int maxConcurrentProviders) {
        this.providerNames = providerNames != null ? List.copyOf(providerNames) : List.of();
        this.requestedAt = requestedAt;
        this.requestedBy = requestedBy;
        this.enableBatchProcessing = enableBatchProcessing;
        this.maxConcurrentProviders = maxConcurrentProviders;
    }

    public static ProcessGpsDataCommand fromAllProviders(String requestedBy) {
        return new ProcessGpsDataCommand(
                List.of("TUGDK", "AYAUK"),
                Instant.now(),
                requestedBy,
                true,
                10
        );
    }

    public static ProcessGpsDataCommand fromSpecificProviders(List<String> providerNames, String requestedBy) {
        return new ProcessGpsDataCommand(
                providerNames,
                Instant.now(),
                requestedBy,
                true,
                5
        );
    }

    public static ProcessGpsDataCommand singleProvider(String providerName, String requestedBy) {
        return new ProcessGpsDataCommand(
                List.of(providerName),
                Instant.now(),
                requestedBy,
                false,
                1
        );
    }

    public void validate() {
        if (providerNames.isEmpty()) {
            throw new IllegalArgumentException("At least one GPS provider must be specified");
        }

        if (requestedBy == null || requestedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("RequestedBy cannot be null or empty");
        }

        if (maxConcurrentProviders < 1 || maxConcurrentProviders > 20) {
            throw new IllegalArgumentException("MaxConcurrentProviders must be between 1 and 20");
        }
    }

    public String getCommandName() {
        return "ProcessGpsData";
    }

    public String getAggregateId() {
        return "GPS_SYSTEM";
    }
}