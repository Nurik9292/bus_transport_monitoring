package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;


public final class StaffName extends ValueObject {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 50;
    private static final String NAME_PATTERN = "^[a-zA-Zа-яА-Я\\s\\-']+$";

    private final String firstName;
    private final String lastName;
    private final String middleName;

    private StaffName(String firstName, String lastName, String middleName) {
        this.firstName = normalizeNamePart(firstName);
        this.lastName = normalizeNamePart(lastName);
        this.middleName = middleName != null ? normalizeNamePart(middleName) : null;
        validate();
    }

    public static StaffName of(String firstName, String lastName) {
        return new StaffName(firstName, lastName, null);
    }

    public static StaffName of(String firstName, String lastName, String middleName) {
        return new StaffName(firstName, lastName, middleName);
    }

    private String normalizeNamePart(String namePart) {
        if (namePart == null || namePart.trim().isEmpty()) {
            return namePart;
        }
        String trimmed = namePart.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    @Override
    protected void validate() {
        validateNamePart(firstName, "First name");
        validateNamePart(lastName, "Last name");
        if (middleName != null) {
            validateNamePart(middleName, "Middle name");
        }
    }

    private void validateNamePart(String namePart, String partType) {
        if (namePart == null || namePart.trim().isEmpty()) {
            throw new InvalidStaffNameException(partType + " cannot be null or empty");
        }

        if (namePart.length() < MIN_NAME_LENGTH || namePart.length() > MAX_NAME_LENGTH) {
            throw new InvalidStaffNameException(
                    String.format("%s must be between %d and %d characters, got: %d",
                            partType, MIN_NAME_LENGTH, MAX_NAME_LENGTH, namePart.length()));
        }

        if (!namePart.matches(NAME_PATTERN)) {
            throw new InvalidStaffNameException(
                    partType + " contains invalid characters: " + namePart);
        }
    }

    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        fullName.append(lastName).append(" ").append(firstName);
        if (middleName != null) {
            fullName.append(" ").append(middleName);
        }
        return fullName.toString();
    }

    public String getShortName() {
        StringBuilder shortName = new StringBuilder();
        shortName.append(firstName.charAt(0)).append(".");
        if (middleName != null) {
            shortName.append(" ").append(middleName.charAt(0)).append(".");
        }
        shortName.append(" ").append(lastName);
        return shortName.toString();
    }

    public String getFormalName() {
        StringBuilder formalName = new StringBuilder();
        formalName.append(lastName).append(" ").append(firstName.charAt(0)).append(".");
        if (middleName != null) {
            formalName.append(middleName.charAt(0)).append(".");
        }
        return formalName.toString();
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getMiddleName() { return middleName; }
    public boolean hasMiddleName() { return middleName != null; }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{firstName, lastName, middleName};
    }

    @Override
    public String toString() {
        return getFullName();
    }
}