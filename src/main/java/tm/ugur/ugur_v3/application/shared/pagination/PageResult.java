package tm.ugur.ugur_v3.application.shared.pagination;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PageResult<T> {

    private final List<T> content;
    private final PageRequest originalRequest;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;
    private final Optional<String> nextCursor;
    private final Optional<String> previousCursor;
    private final PageMetrics metrics;


    private PageResult(List<T> content, PageRequest originalRequest, long totalElements,
                       int totalPages, boolean hasNext, boolean hasPrevious,
                       Optional<String> nextCursor, Optional<String> previousCursor,
                       PageMetrics metrics) {
        this.content = List.copyOf(content); // Defensive copy
        this.originalRequest = originalRequest;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.nextCursor = nextCursor;
        this.previousCursor = previousCursor;
        this.metrics = metrics;
    }


    public static <T> PageResult<T> of(List<T> content, PageRequest request, long totalElements) {
        return of(content, request, totalElements, PageMetrics.empty());
    }


    public static <T> PageResult<T> of(List<T> content, PageRequest request,
                                       long totalElements, PageMetrics metrics) {
        if (request.isCursorBased()) {
            throw new IllegalArgumentException("Use ofCursor() for cursor-based pagination");
        }

        int totalPages = calculateTotalPages(totalElements, request.getPageSize());
        boolean hasNext = (request.getPageNumber() + 1) < totalPages;
        boolean hasPrevious = request.getPageNumber() > 0;

        return new PageResult<>(
                content, request, totalElements, totalPages,
                hasNext, hasPrevious,
                Optional.empty(), Optional.empty(), // No cursors for offset-based
                metrics
        );
    }


    public static <T> PageResult<T> ofCursor(List<T> content, PageRequest request,
                                             boolean hasNext, Optional<String> nextCursor) {
        return ofCursor(content, request, hasNext, nextCursor, PageMetrics.empty());
    }


    public static <T> PageResult<T> ofCursor(List<T> content, PageRequest request,
                                             boolean hasNext, Optional<String> nextCursor,
                                             PageMetrics metrics) {
        if (!request.isCursorBased()) {
            throw new IllegalArgumentException("Use of() for offset-based pagination");
        }

        boolean hasPrevious = request.getCursor().isPresent();

        return new PageResult<>(
                content, request, -1, -1,
                hasNext, hasPrevious,
                nextCursor, request.getCursor(),
                metrics
        );
    }


    public static <T> PageResult<T> empty(PageRequest request) {
        if (request.isCursorBased()) {
            return ofCursor(List.of(), request, false, Optional.empty());
        } else {
            return of(List.of(), request, 0);
        }
    }


    public <U> PageResult<U> map(Function<T, U> mapper) {
        List<U> mappedContent = content.stream()
                .map(mapper)
                .collect(Collectors.toList());

        return new PageResult<>(
                mappedContent, originalRequest, totalElements, totalPages,
                hasNext, hasPrevious, nextCursor, previousCursor, metrics
        );
    }


    public PageRequest getNextPageRequest() {
        if (!hasNext) {
            throw new IllegalStateException("No next page available");
        }

        if (originalRequest.isCursorBased()) {
            if (nextCursor.isEmpty()) {
                throw new IllegalStateException("Next cursor not available");
            }
            return originalRequest.nextCursor(nextCursor.get());
        } else {
            return originalRequest.next();
        }
    }


    public PageRequest getPreviousPageRequest() {
        if (!hasPrevious) {
            throw new IllegalStateException("No previous page available");
        }

        if (originalRequest.isCursorBased()) {
            if (previousCursor.isEmpty()) {
                throw new IllegalStateException("Previous cursor not available");
            }
            return PageRequest.ofCursor(previousCursor.get(), originalRequest.getPageSize(),
                    originalRequest.getSortOrders());
        } else {
            return originalRequest.previous();
        }
    }


    public PageRequest getFirstPageRequest() {
        if (originalRequest.isCursorBased()) {

            return PageRequest.first(originalRequest.getPageSize());
        } else {
            return PageRequest.ofPage(0, originalRequest.getPageSize(),
                    originalRequest.getSortOrders());
        }
    }


    public PageRequest getLastPageRequest() {
        if (originalRequest.isCursorBased()) {
            throw new IllegalStateException("Last page not supported for cursor-based pagination");
        }

        if (totalPages == 0) {
            return getFirstPageRequest();
        }

        return PageRequest.ofPage(totalPages - 1, originalRequest.getPageSize(),
                originalRequest.getSortOrders());
    }


    public boolean isFirst() {
        return !hasPrevious;
    }


    public boolean isLast() {
        return !hasNext;
    }


    public boolean isEmpty() {
        return content.isEmpty();
    }


    public int getSize() {
        return content.size();
    }


    public int getPageNumber() {
        return originalRequest.isCursorBased() ? -1 : originalRequest.getPageNumber();
    }


    private static int calculateTotalPages(long totalElements, int pageSize) {
        if (totalElements == 0 || pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / pageSize);
    }

    public List<T> getContent() { return content; }
    public PageRequest getOriginalRequest() { return originalRequest; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean hasNext() { return hasNext; }
    public boolean hasPrevious() { return hasPrevious; }
    public Optional<String> getNextCursor() { return nextCursor; }
    public Optional<String> getPreviousCursor() { return previousCursor; }
    public PageMetrics getMetrics() { return metrics; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageResult<?> that)) return false;
        return totalElements == that.totalElements &&
                totalPages == that.totalPages &&
                hasNext == that.hasNext &&
                hasPrevious == that.hasPrevious &&
                Objects.equals(content, that.content) &&
                Objects.equals(originalRequest, that.originalRequest) &&
                Objects.equals(nextCursor, that.nextCursor) &&
                Objects.equals(previousCursor, that.previousCursor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, originalRequest, totalElements, totalPages,
                hasNext, hasPrevious, nextCursor, previousCursor);
    }

    @Override
    public String toString() {
        if (originalRequest.isCursorBased()) {
            return String.format("PageResult{size=%d, hasNext=%s, hasPrevious=%s, cursor-based}",
                    content.size(), hasNext, hasPrevious);
        } else {
            return String.format("PageResult{page=%d, size=%d, totalElements=%d, totalPages=%d}",
                    originalRequest.getPageNumber(), content.size(), totalElements, totalPages);
        }
    }
}