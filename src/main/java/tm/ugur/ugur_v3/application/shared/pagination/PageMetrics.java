package tm.ugur.ugur_v3.application.shared.pagination;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;


@Getter
public final class PageMetrics {

    private final Duration queryExecutionTime;
    private final Duration totalProcessingTime;
    private final long rowsScanned;
    private final long rowsReturned;
    private final boolean indexUsed;
    private final boolean cacheHit;
    private final Optional<String> executionPlan;
    private final Optional<String> optimizationApplied;
    private final Instant timestamp;

    private PageMetrics(Duration queryExecutionTime, Duration totalProcessingTime,
                        long rowsScanned, long rowsReturned, boolean indexUsed,
                        boolean cacheHit, Optional<String> executionPlan,
                        Optional<String> optimizationApplied, Instant timestamp) {
        this.queryExecutionTime = queryExecutionTime;
        this.totalProcessingTime = totalProcessingTime;
        this.rowsScanned = rowsScanned;
        this.rowsReturned = rowsReturned;
        this.indexUsed = indexUsed;
        this.cacheHit = cacheHit;
        this.executionPlan = executionPlan;
        this.optimizationApplied = optimizationApplied;
        this.timestamp = timestamp;
    }


    public static PageMetrics empty() {
        return new PageMetrics(
                Duration.ZERO, Duration.ZERO, 0, 0, false, false,
                Optional.empty(), Optional.empty(), Instant.now()
        );
    }


    public static PageMetrics basic(Duration queryTime, long rowsReturned) {
        return builder()
                .withQueryExecutionTime(queryTime)
                .withTotalProcessingTime(queryTime)
                .withRowsReturned(rowsReturned)
                .withTimestamp(Instant.now())
                .build();
    }


    public static Builder builder() {
        return new Builder();
    }


    public double getEfficiencyRatio() {
        if (rowsScanned == 0) {
            return 1.0;
        }

        double scanRatio = (double) rowsReturned / rowsScanned;

        double indexBonus = indexUsed ? 1.2 : 1.0;
        double cacheBonus = cacheHit ? 1.5 : 1.0;

        return Math.min(1.0, scanRatio * indexBonus * cacheBonus);
    }

    public boolean isSlowQuery(Duration threshold) {
        return queryExecutionTime.compareTo(threshold) > 0;
    }

    public boolean needsOptimization() {

        if (isSlowQuery(Duration.ofMillis(100))) {
            return true;
        }

        if (getEfficiencyRatio() < 0.5) {
            return true;
        }

        if (!indexUsed && rowsScanned > 1000) {
            return true;
        }

        return false;
    }


    public PerformanceCategory getPerformanceCategory() {
        if (cacheHit) {
            return PerformanceCategory.EXCELLENT;
        }

        if (getEfficiencyRatio() > 0.8 && queryExecutionTime.toMillis() < 50) {
            return PerformanceCategory.GOOD;
        }

        if (getEfficiencyRatio() > 0.5 && queryExecutionTime.toMillis() < 200) {
            return PerformanceCategory.ACCEPTABLE;
        }

        if (queryExecutionTime.toMillis() < 1000) {
            return PerformanceCategory.POOR;
        }

        return PerformanceCategory.CRITICAL;
    }


    public String getSummary() {
        return String.format(
                "Query: %dms, Rows: %d/%d, Index: %s, Cache: %s, Efficiency: %.2f",
                queryExecutionTime.toMillis(),
                rowsReturned,
                rowsScanned,
                indexUsed ? "Yes" : "No",
                cacheHit ? "Hit" : "Miss",
                getEfficiencyRatio()
        );
    }


    public enum PerformanceCategory {

        EXCELLENT,

        GOOD,

        ACCEPTABLE,

        POOR,

        CRITICAL
    }


    public static class Builder {
        private Duration queryExecutionTime = Duration.ZERO;
        private Duration totalProcessingTime = Duration.ZERO;
        private long rowsScanned = 0;
        private long rowsReturned = 0;
        private boolean indexUsed = false;
        private boolean cacheHit = false;
        private Optional<String> executionPlan = Optional.empty();
        private Optional<String> optimizationApplied = Optional.empty();
        private Instant timestamp = Instant.now();

        public Builder withQueryExecutionTime(Duration queryExecutionTime) {
            this.queryExecutionTime = Objects.requireNonNull(queryExecutionTime);
            return this;
        }

        public Builder withTotalProcessingTime(Duration totalProcessingTime) {
            this.totalProcessingTime = Objects.requireNonNull(totalProcessingTime);
            return this;
        }

        public Builder withRowsScanned(long rowsScanned) {
            this.rowsScanned = Math.max(0, rowsScanned);
            return this;
        }

        public Builder withRowsReturned(long rowsReturned) {
            this.rowsReturned = Math.max(0, rowsReturned);
            return this;
        }

        public Builder withIndexUsed(boolean indexUsed) {
            this.indexUsed = indexUsed;
            return this;
        }

        public Builder withCacheHit(boolean cacheHit) {
            this.cacheHit = cacheHit;
            return this;
        }

        public Builder withExecutionPlan(String executionPlan) {
            this.executionPlan = Optional.ofNullable(executionPlan);
            return this;
        }

        public Builder withOptimizationApplied(String optimizationApplied) {
            this.optimizationApplied = Optional.ofNullable(optimizationApplied);
            return this;
        }

        public Builder withTimestamp(Instant timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp);
            return this;
        }

        public Builder autoCalculateTotalTime() {
            if (totalProcessingTime.isZero() && !queryExecutionTime.isZero()) {
                this.totalProcessingTime = queryExecutionTime;
            }
            return this;
        }

        public Builder validate() {
            if (rowsReturned > rowsScanned && rowsScanned > 0) {
                throw new IllegalStateException(
                        "Rows returned (" + rowsReturned + ") cannot exceed rows scanned (" + rowsScanned + ")"
                );
            }

            if (totalProcessingTime.compareTo(queryExecutionTime) < 0) {
                throw new IllegalStateException(
                        "Total processing time cannot be less than query execution time"
                );
            }

            return this;
        }

        public PageMetrics build() {
            return new PageMetrics(
                    queryExecutionTime, totalProcessingTime, rowsScanned, rowsReturned,
                    indexUsed, cacheHit, executionPlan, optimizationApplied, timestamp
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageMetrics)) return false;
        PageMetrics that = (PageMetrics) o;
        return rowsScanned == that.rowsScanned &&
                rowsReturned == that.rowsReturned &&
                indexUsed == that.indexUsed &&
                cacheHit == that.cacheHit &&
                Objects.equals(queryExecutionTime, that.queryExecutionTime) &&
                Objects.equals(totalProcessingTime, that.totalProcessingTime) &&
                Objects.equals(executionPlan, that.executionPlan) &&
                Objects.equals(optimizationApplied, that.optimizationApplied) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryExecutionTime, totalProcessingTime, rowsScanned, rowsReturned,
                indexUsed, cacheHit, executionPlan, optimizationApplied, timestamp);
    }

    @Override
    public String toString() {
        return String.format(
                "PageMetrics{queryTime=%dms, totalTime=%dms, rows=%d/%d, index=%s, cache=%s, efficiency=%.2f, category=%s}",
                queryExecutionTime.toMillis(),
                totalProcessingTime.toMillis(),
                rowsReturned,
                rowsScanned,
                indexUsed ? "Yes" : "No",
                cacheHit ? "Hit" : "Miss",
                getEfficiencyRatio(),
                getPerformanceCategory()
        );
    }
}