package payroll.security;

import java.util.Locale;
import java.util.Optional;

public enum UserType {
    ADMIN("Admin"),
    EMPLOYEE("Employee");

    private final String displayName;

    UserType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<UserType> fromDisplayName(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String normalizedInput = input.trim().toUpperCase(Locale.ROOT);
        for (UserType userType : values()) {
            if (userType.name().equals(normalizedInput)) {
                return Optional.of(userType);
            }
        }

        return Optional.empty();
    }
}
