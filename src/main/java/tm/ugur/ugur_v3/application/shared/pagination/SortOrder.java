package tm.ugur.ugur_v3.application.shared.pagination;

import lombok.Getter;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
public final class SortOrder {

    private static final Pattern VALID_FIELD_NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_.]*$");
    private static final int MAX_FIELD_PATH_LENGTH = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "vehicle.id", "vehicle.licensePlate", "vehicle.type", "vehicle.capacity",
            "vehicle.location.latitude", "vehicle.location.longitude", "vehicle.lastUpdateTime",

            "route.id", "route.name", "route.number", "route.startTime", "route.endTime",

            "stop.id", "stop.name", "stop.sequence", "stop.arrivalTime",

            "tracking.timestamp", "tracking.speed", "tracking.heading", "tracking.accuracy",

            "id", "createdAt", "updatedAt", "name", "status"
    );

    private final String fieldName;
    private final SortDirection direction;
    private final NullHandling nullHandling;
    private final boolean ignoreCase;


    private SortOrder(String fieldName,
                      SortDirection direction,
                      NullHandling nullHandling,
                      boolean ignoreCase) {
        this.fieldName = fieldName;
        this.direction = direction;
        this.nullHandling = nullHandling;
        this.ignoreCase = ignoreCase;
    }


    public static SortOrder asc(String fieldName) {
        return of(fieldName, SortDirection.ASC);
    }


    public static SortOrder desc(String fieldName) {
        return of(fieldName, SortDirection.DESC);
    }


    public static SortOrder of(String fieldName, SortDirection direction) {
        validateFieldName(fieldName);
        return new SortOrder(fieldName, direction, NullHandling.NATIVE, false);
    }


    public static SortOrder ofIgnoreCase(String fieldName, SortDirection direction) {
        validateFieldName(fieldName);
        return new SortOrder(fieldName, direction, NullHandling.NATIVE, true);
    }


    public static SortOrder ofWithNulls(String fieldName, SortDirection direction,
                                        NullHandling nullHandling) {
        validateFieldName(fieldName);
        return new SortOrder(fieldName, direction, nullHandling, false);
    }


    public static class CommonSorts {

        public static SortOrder byNewestFirst() {
            return desc("updatedAt");
        }

        public static SortOrder byOldestFirst() {
            return asc("createdAt");
        }

        public static SortOrder byVehicleId() {
            return asc("vehicle.id");
        }

        public static SortOrder byLicensePlate() {
            return asc("vehicle.licensePlate");
        }

        public static SortOrder byLastUpdateTime() {
            return desc("vehicle.lastUpdateTime");
        }

        public static SortOrder byRouteNumber() {
            return asc("route.number");
        }

        public static SortOrder byRouteName() {
            return ofIgnoreCase("route.name", SortDirection.ASC);
        }

        public static SortOrder byDistanceFromPoint(double latitude, double longitude) {

            String spatialField = String.format("ST_Distance(location, ST_Point(%f, %f))",
                    longitude, latitude);
            return new SortOrder(spatialField, SortDirection.ASC, NullHandling.NULLS_LAST, false);
        }

        public static SortOrder bySpeed() {
            return desc("tracking.speed");
        }

        public static SortOrder byAccuracy() {
            return desc("tracking.accuracy");
        }
    }

    public SortOrder reverse() {
        SortDirection reversedDirection = direction == SortDirection.ASC ?
                SortDirection.DESC : SortDirection.ASC;
        return new SortOrder(fieldName, reversedDirection, nullHandling, ignoreCase);
    }


    public boolean isSpatialSort() {
        return fieldName.contains("ST_Distance") ||
                fieldName.contains("location") ||
                fieldName.contains("latitude") ||
                fieldName.contains("longitude");
    }


    public boolean canUseIndex() {

        if (ignoreCase && !isTextualField()) {
            return false;
        }

        if (isSpatialSort()) {
            return true;
        }

        return ALLOWED_SORT_FIELDS.contains(fieldName);
    }


    public String toSqlFragment() {
        StringBuilder sql = new StringBuilder();

        if (ignoreCase && isTextualField()) {
            sql.append("LOWER(").append(fieldName).append(")");
        } else {
            sql.append(fieldName);
        }

        sql.append(" ").append(direction.getSqlKeyword());

        if (nullHandling != NullHandling.NATIVE) {
            sql.append(" ").append(nullHandling.getSqlKeyword());
        }

        return sql.toString();
    }


    private boolean isTextualField() {
        return fieldName.contains("name") ||
                fieldName.contains("licensePlate") ||
                fieldName.contains("description");
    }

    private static void validateFieldName(String fieldName) {
        Objects.requireNonNull(fieldName, "Field name cannot be null");

        if (fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be empty");
        }

        if (fieldName.length() > MAX_FIELD_PATH_LENGTH) {
            throw new IllegalArgumentException(
                    "Field name too long: " + fieldName.length() + " chars. Max: " + MAX_FIELD_PATH_LENGTH
            );
        }

        if (!VALID_FIELD_NAME.matcher(fieldName).matches()) {
            throw new IllegalArgumentException("Invalid field name format: " + fieldName);
        }

        if (!ALLOWED_SORT_FIELDS.contains(fieldName) && !fieldName.contains("ST_Distance")) {
            throw new IllegalArgumentException(
                    "Field not allowed for sorting: " + fieldName +
                            ". Allowed fields: " + ALLOWED_SORT_FIELDS
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SortOrder sortOrder)) return false;
        return ignoreCase == sortOrder.ignoreCase &&
                Objects.equals(fieldName, sortOrder.fieldName) &&
                direction == sortOrder.direction &&
                nullHandling == sortOrder.nullHandling;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, direction, nullHandling, ignoreCase);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fieldName);
        if (ignoreCase) sb.append(" (ignore case)");
        sb.append(" ").append(direction);
        if (nullHandling != NullHandling.NATIVE) {
            sb.append(" ").append(nullHandling);
        }
        return sb.toString();
    }
}