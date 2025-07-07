package tm.ugur.ugur_v3.domain.staffManagement.services;

public final class PasswordPolicy {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return hasValidLength(password) &&
                hasUpperCase(password) &&
                hasLowerCase(password) &&
                hasDigit(password) &&
                hasSpecialChar(password) &&
                !hasCommonPattern(password);
    }

    public static java.util.List<String> getViolations(String password) {
        java.util.List<String> violations = new java.util.ArrayList<>();

        if (!hasValidLength(password)) {
            violations.add("Пароль должен содержать от " + MIN_LENGTH + " до " + MAX_LENGTH + " символов");
        }
        if (!hasUpperCase(password)) {
            violations.add("Пароль должен содержать хотя бы одну заглавную букву");
        }
        if (!hasLowerCase(password)) {
            violations.add("Пароль должен содержать хотя бы одну строчную букву");
        }
        if (!hasDigit(password)) {
            violations.add("Пароль должен содержать хотя бы одну цифру");
        }
        if (!hasSpecialChar(password)) {
            violations.add("Пароль должен содержать хотя бы один специальный символ");
        }
        if (hasCommonPattern(password)) {
            violations.add("Пароль слишком простой");
        }

        return violations;
    }

    private static boolean hasValidLength(String password) {
        return password != null &&
                password.length() >= MIN_LENGTH &&
                password.length() <= MAX_LENGTH;
    }

    private static boolean hasUpperCase(String password) {
        return password != null && password.matches(".*[A-ZА-Я].*");
    }

    private static boolean hasLowerCase(String password) {
        return password != null && password.matches(".*[a-zа-я].*");
    }

    private static boolean hasDigit(String password) {
        return password != null && password.matches(".*\\d.*");
    }

    private static boolean hasSpecialChar(String password) {
        return password != null && password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }

    private static boolean hasCommonPattern(String password) {
        if (password == null) return true;

        String lower = password.toLowerCase();
        return lower.contains("password") ||
                lower.contains("admin") ||
                lower.contains("123456") ||
                lower.equals("qwerty") ||
                isSequential(password);
    }

    private static boolean isSequential(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
        }
        return false;
    }
}