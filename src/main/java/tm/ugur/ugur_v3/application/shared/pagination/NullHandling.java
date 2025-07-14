package tm.ugur.ugur_v3.application.shared.pagination;

public enum NullHandling {

    NATIVE(""),

    NULLS_FIRST("NULLS FIRST"),

    NULLS_LAST("NULLS LAST");

    private final String sqlKeyword;

    NullHandling(String sqlKeyword) {
        this.sqlKeyword = sqlKeyword;
    }


    public String getSqlKeyword() {
        return sqlKeyword;
    }


    public boolean requiresSqlKeyword() {
        return this != NATIVE;
    }


    public static NullHandling getRecommendedFor(FieldType fieldType, SortDirection sortDirection) {
        return switch (fieldType) {
            case REQUIRED_FIELD -> NATIVE;

            case GPS_COORDINATE -> NULLS_LAST;

            case TIMESTAMP -> switch (sortDirection) {
                case ASC -> NULLS_FIRST;
                case DESC -> NULLS_LAST;
            };

            case OPTIONAL_METRIC -> NULLS_LAST;

            case USER_INPUT -> NULLS_LAST;

            case CALCULATED_VALUE -> NULLS_LAST;

            case EXTERNAL_DATA -> NULLS_LAST;
        };
    }


    public static NullHandling fromString(String nullHandling) {
        if (nullHandling == null || nullHandling.trim().isEmpty()) {
            return NATIVE;
        }

        String normalized = nullHandling.trim().toUpperCase().replace(" ", "_");

        return switch (normalized) {
            case "NATIVE", "DEFAULT" -> NATIVE;
            case "NULLS_FIRST", "NULLSFIRST", "FIRST" -> NULLS_FIRST;
            case "NULLS_LAST", "NULLSLAST", "LAST" -> NULLS_LAST;
            default -> throw new IllegalArgumentException("Unknown null handling: " + nullHandling);
        };
    }


    public static NullHandling forIncompleteData(boolean showIncompleteDataFirst) {
        return showIncompleteDataFirst ? NULLS_FIRST : NULLS_LAST;
    }


    public NullHandling inverse() {
        return switch (this) {
            case NULLS_FIRST -> NULLS_LAST;
            case NULLS_LAST -> NULLS_FIRST;
            case NATIVE -> NATIVE;
        };
    }


    public boolean isSupportedBy(DatabaseType databaseType) {
        if (this == NATIVE) {
            return true;
        }

        return switch (databaseType) {
            case POSTGRESQL, ORACLE, SQL_SERVER -> true;
            case MYSQL -> false;
            case H2, HSQLDB -> true;
        };
    }

    public NullHandling getFallbackFor(DatabaseType databaseType) {
        if (isSupportedBy(databaseType)) {
            return this;
        }

        return NATIVE;
    }


    public enum FieldType {

        REQUIRED_FIELD,

        GPS_COORDINATE,

        TIMESTAMP,

        OPTIONAL_METRIC,

        USER_INPUT,

        CALCULATED_VALUE,

        EXTERNAL_DATA
    }

    public enum DatabaseType {
        POSTGRESQL,
        MYSQL,
        ORACLE,
        SQL_SERVER,
        H2,
        HSQLDB
    }

    @Override
    public String toString() {
        return switch (this) {
            case NATIVE -> "NATIVE";
            case NULLS_FIRST -> "NULLS FIRST";
            case NULLS_LAST -> "NULLS LAST";
        };
    }
}