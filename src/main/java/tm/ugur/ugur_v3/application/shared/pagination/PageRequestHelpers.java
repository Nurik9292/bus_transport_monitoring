/**
 * COMPONENT: PageRequestHelpers
 * LAYER: Application/Shared/Pagination
 * PURPOSE: Utility methods для создания PageRequest в различных сценариях
 * PERFORMANCE TARGET: Zero allocation для default requests
 * SCALABILITY: Reusable patterns для pagination
 */
package tm.ugur.ugur_v3.application.shared.pagination;

import java.util.List;

public final class PageRequestHelpers {



    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int SMALL_PAGE_SIZE = 10;
    public static final int LARGE_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 100;



    private static final PageRequest DEFAULT_FIRST_PAGE = PageRequest.first(DEFAULT_PAGE_SIZE);
    private static final PageRequest SMALL_FIRST_PAGE = PageRequest.first(SMALL_PAGE_SIZE);
    private static final PageRequest LARGE_FIRST_PAGE = PageRequest.first(LARGE_PAGE_SIZE);



    public static PageRequest defaultFirst() {
        return DEFAULT_FIRST_PAGE;
    }

    public static PageRequest smallFirst() {
        return SMALL_FIRST_PAGE;
    }

    public static PageRequest largeFirst() {
        return LARGE_FIRST_PAGE;
    }

    public static PageRequest forEmptyResult() {
        return DEFAULT_FIRST_PAGE;
    }

    public static PageRequest forErrorResult() {
        return PageRequest.first(1);
    }



    public static PageRequest safe(int pageNumber, int pageSize) {
        int safePage = Math.max(0, pageNumber);
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);

        return PageRequest.ofPage(safePage, safeSize);
    }

    public static PageRequest withSort(int pageNumber, int pageSize,
                                       String sortProperty, SortDirection direction) {
        SortOrder sortOrder = SortOrder.of(sortProperty, direction);
        return PageRequest.ofPage(pageNumber, pageSize, List.of(sortOrder));
    }

    public static PageRequest forRealTime(int pageNumber) {
        return PageRequest.ofPage(pageNumber, SMALL_PAGE_SIZE);
    }

    public static PageRequest forBulkOperation(int pageNumber) {
        return PageRequest.ofPage(pageNumber, LARGE_PAGE_SIZE);
    }



    public static PageRequest cursorBased(String cursor, int pageSize) {
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        return PageRequest.ofCursor(cursor, safeSize);
    }

    public static PageRequest firstCursor(int pageSize) {
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        return PageRequest.first(safeSize);
    }



    public static PageRequest adaptive(int pageNumber, DataType dataType) {
        return switch (dataType) {
            case LIGHTWEIGHT -> PageRequest.ofPage(pageNumber, LARGE_PAGE_SIZE);
            case MEDIUM -> PageRequest.ofPage(pageNumber, DEFAULT_PAGE_SIZE);
            case HEAVY -> PageRequest.ofPage(pageNumber, SMALL_PAGE_SIZE);
            case REAL_TIME -> PageRequest.ofPage(pageNumber, 5);
        };
    }

    public static PageRequest forLoad(int pageNumber, LoadLevel loadLevel) {
        return switch (loadLevel) {
            case LOW -> PageRequest.ofPage(pageNumber, LARGE_PAGE_SIZE);
            case MEDIUM -> PageRequest.ofPage(pageNumber, DEFAULT_PAGE_SIZE);
            case HIGH -> PageRequest.ofPage(pageNumber, SMALL_PAGE_SIZE);
            case CRITICAL -> PageRequest.ofPage(pageNumber, 5);
        };
    }



    public static boolean isValid(PageRequest request) {
        if (request == null) return false;

        return request.getPageNumber() >= 0 &&
                request.getPageSize() > 0 &&
                request.getPageSize() <= MAX_PAGE_SIZE;
    }

    public static PageRequest normalize(PageRequest request) {
        if (request == null) {
            return defaultFirst();
        }

        if (isValid(request)) {
            return request;
        }


        int safePage = Math.max(0, request.getPageNumber());
        int safeSize = Math.min(Math.max(1, request.getPageSize()), MAX_PAGE_SIZE);

        if (request.isCursorBased() && request.getCursor().isPresent()) {
            return PageRequest.ofCursor(request.getCursor().get(), safeSize,
                    request.getSortOrders());
        }

        return PageRequest.ofPage(safePage, safeSize, request.getSortOrders());
    }



    public static PageRequest forPerformance(int pageNumber) {
        return PageRequest.ofPage(pageNumber, DEFAULT_PAGE_SIZE);
    }

    public static PageRequest forBatch(int pageNumber) {
        return PageRequest.ofPage(pageNumber, MAX_PAGE_SIZE);
    }



    public enum DataType {
        LIGHTWEIGHT,
        MEDIUM,
        HEAVY,
        REAL_TIME
    }

    public enum LoadLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }



    public static final class PageSizes {
        public static final int MOBILE = 5;
        public static final int TABLET = 10;
        public static final int DESKTOP = 20;
        public static final int BULK = 50;
        public static final int EXPORT = 100;
    }

    private PageRequestHelpers() {
        throw new UnsupportedOperationException("Utility class");
    }
}