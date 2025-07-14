package tm.ugur.ugur_v3.application.shared.pagination;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


public final class PageRequest {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final int MAX_OFFSET = 100000;

    private final int pageNumber;
    private final int pageSize;
    private final long offset;
    private final List<SortOrder> sortOrders;
    private final Optional<String> cursor;
    private final PaginationType paginationType;


    private PageRequest(int pageNumber,
                        int pageSize,
                        long offset,
                        List<SortOrder> sortOrders,
                        Optional<String> cursor,
                        PaginationType paginationType) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.offset = offset;
        this.sortOrders = List.copyOf(sortOrders);
        this.cursor = cursor;
        this.paginationType = paginationType;
    }

    public static PageRequest ofPage(int pageNumber, int pageSize) {
        validatePageNumber(pageNumber);
        validatePageSize(pageSize);

        long offset = (long) pageNumber * pageSize;
        if (offset > MAX_OFFSET) {
            throw new IllegalArgumentException(
                    "Page offset too large: " + offset + ". Consider using cursor-based pagination."
            );
        }

        return new PageRequest(
                pageNumber,
                pageSize,
                offset,
                List.of(),
                Optional.empty(),
                PaginationType.OFFSET_BASED
        );
    }


    public static PageRequest ofPage(int pageNumber, int pageSize, List<SortOrder> sortOrders) {
        PageRequest baseRequest = ofPage(pageNumber, pageSize);
        return new PageRequest(
                baseRequest.pageNumber,
                baseRequest.pageSize,
                baseRequest.offset,
                sortOrders,
                Optional.empty(),
                PaginationType.OFFSET_BASED
        );
    }


    public static PageRequest ofCursor(String cursor, int pageSize) {
        validatePageSize(pageSize);
        Objects.requireNonNull(cursor, "Cursor cannot be null");

        return new PageRequest(
                0,
                pageSize,
                0,
                List.of(),
                Optional.of(cursor),
                PaginationType.CURSOR_BASED
        );
    }

    public static PageRequest ofCursor(String cursor, int pageSize, List<SortOrder> sortOrders) {
        PageRequest baseRequest = ofCursor(cursor, pageSize);
        return new PageRequest(
                baseRequest.pageNumber,
                baseRequest.pageSize,
                baseRequest.offset,
                sortOrders,
                baseRequest.cursor,
                PaginationType.CURSOR_BASED
        );
    }

    public static PageRequest first() {
        return ofPage(0, DEFAULT_PAGE_SIZE);
    }


    public static PageRequest first(int pageSize) {
        return ofPage(0, pageSize);
    }

    public PageRequest next() {
        if (paginationType == PaginationType.CURSOR_BASED) {
            throw new IllegalStateException("Use nextCursor() for cursor-based pagination");
        }
        return ofPage(pageNumber + 1, pageSize, sortOrders);
    }


    public PageRequest previous() {
        if (paginationType == PaginationType.CURSOR_BASED) {
            throw new IllegalStateException("Use previousCursor() for cursor-based pagination");
        }
        if (pageNumber == 0) {
            throw new IllegalStateException("Cannot go to previous page from first page");
        }
        return ofPage(pageNumber - 1, pageSize, sortOrders);
    }


    public PageRequest nextCursor(String nextCursor) {
        if (paginationType != PaginationType.CURSOR_BASED) {
            throw new IllegalStateException("This method only works with cursor-based pagination");
        }
        return ofCursor(nextCursor, pageSize, sortOrders);
    }

    public boolean isFirst() {
        return pageNumber == 0 && paginationType == PaginationType.OFFSET_BASED;
    }


    public boolean isCursorBased() {
        return paginationType == PaginationType.CURSOR_BASED;
    }


    public QueryOptimizationHints getOptimizationHints() {
        return QueryOptimizationHints.builder()
                .withPaginationType(paginationType)
                .withExpectedResultSize(pageSize)
                .withSortComplexity(sortOrders.size())
                .withDeepPagination(offset > 10000)
                .build();
    }


    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public long getOffset() { return offset; }
    public List<SortOrder> getSortOrders() { return sortOrders; }
    public Optional<String> getCursor() { return cursor; }
    public PaginationType getPaginationType() { return paginationType; }

    private static void validatePageNumber(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number cannot be negative: " + pageNumber);
        }
    }

    private static void validatePageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive: " + pageSize);
        }
        if (pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size too large: " + pageSize + ". Maximum allowed: " + MAX_PAGE_SIZE
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageRequest)) return false;
        PageRequest that = (PageRequest) o;
        return pageNumber == that.pageNumber &&
                pageSize == that.pageSize &&
                offset == that.offset &&
                Objects.equals(sortOrders, that.sortOrders) &&
                Objects.equals(cursor, that.cursor) &&
                paginationType == that.paginationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, pageSize, offset, sortOrders, cursor, paginationType);
    }

    @Override
    public String toString() {
        if (paginationType == PaginationType.CURSOR_BASED) {
            return String.format("PageRequest{cursor='%s', pageSize=%d, sortOrders=%s}",
                    cursor.orElse("none"), pageSize, sortOrders);
        } else {
            return String.format("PageRequest{pageNumber=%d, pageSize=%d, offset=%d, sortOrders=%s}",
                    pageNumber, pageSize, offset, sortOrders);
        }
    }


    public enum PaginationType {

        OFFSET_BASED,

        CURSOR_BASED
    }

    public static class QueryOptimizationHints {
        private final PaginationType paginationType;
        private final int expectedResultSize;
        private final int sortComplexity;
        private final boolean isDeepPagination;

        private QueryOptimizationHints(PaginationType paginationType,
                                       int expectedResultSize,
                                       int sortComplexity,
                                       boolean isDeepPagination) {
            this.paginationType = paginationType;
            this.expectedResultSize = expectedResultSize;
            this.sortComplexity = sortComplexity;
            this.isDeepPagination = isDeepPagination;
        }

        public static Builder builder() {
            return new Builder();
        }

        public PaginationType getPaginationType() { return paginationType; }
        public int getExpectedResultSize() { return expectedResultSize; }
        public int getSortComplexity() { return sortComplexity; }
        public boolean isDeepPagination() { return isDeepPagination; }

        public boolean shouldUseSpecialIndexes() {
            return isDeepPagination || sortComplexity > 2;
        }


        public boolean canUseReadReplica() {
            return true;
        }

        public static class Builder {
            private PaginationType paginationType = PaginationType.OFFSET_BASED;
            private int expectedResultSize = DEFAULT_PAGE_SIZE;
            private int sortComplexity = 0;
            private boolean isDeepPagination = false;

            public Builder withPaginationType(PaginationType paginationType) {
                this.paginationType = paginationType;
                return this;
            }

            public Builder withExpectedResultSize(int expectedResultSize) {
                this.expectedResultSize = expectedResultSize;
                return this;
            }

            public Builder withSortComplexity(int sortComplexity) {
                this.sortComplexity = sortComplexity;
                return this;
            }

            public Builder withDeepPagination(boolean isDeepPagination) {
                this.isDeepPagination = isDeepPagination;
                return this;
            }

            public QueryOptimizationHints build() {
                return new QueryOptimizationHints(
                        paginationType,
                        expectedResultSize,
                        sortComplexity,
                        isDeepPagination
                );
            }
        }
    }
}