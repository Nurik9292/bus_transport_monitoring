package tm.ugur.ugur_v3.application.shared.pagination;

import lombok.Getter;

@Getter
public enum SortDirection {


    ASC("ASC"),


    DESC("DESC");

    private final String sqlKeyword;

    SortDirection(String sqlKeyword) {
        this.sqlKeyword = sqlKeyword;
    }


    public SortDirection reverse() {
        return this == ASC ? DESC : ASC;
    }


    public boolean isAscending() {
        return this == ASC;
    }

    public boolean isDescending() {
        return this == DESC;
    }

    public static SortDirection fromString(String direction) {
        if (direction == null || direction.trim().isEmpty()) {
            throw new IllegalArgumentException("Sort direction cannot be null or empty");
        }

        String normalized = direction.trim().toUpperCase();

        return switch (normalized) {
            case "ASC", "ASCENDING" -> ASC;
            case "DESC", "DESCENDING" -> DESC;
            default -> throw new IllegalArgumentException("Unknown sort direction: " + direction);
        };
    }


    public static SortDirection getDefaultFor(FieldType fieldType) {
        return switch (fieldType) {
            case TIMESTAMP, DATE -> DESC;
            case SCORE, RATING, PRIORITY -> DESC;
            case NAME, TEXT -> ASC;
            case ID, NUMBER -> ASC;
            case DISTANCE -> ASC;
        };
    }

    public enum FieldType {
        TIMESTAMP,
        DATE,
        SCORE,
        RATING,
        PRIORITY,
        NAME,
        TEXT,
        ID,
        NUMBER,
        DISTANCE
    }
}