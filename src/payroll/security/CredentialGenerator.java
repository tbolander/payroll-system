package payroll.security;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CredentialGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public String generateEmployeeUserId(String companyEmail) {
        validateEmail(companyEmail);
        return companyEmail.trim().toLowerCase(Locale.ROOT);
    }

    public String generateInitialEmployeePassword(String companyEmail, LocalDate dateOfBirth) {
        validateEmail(companyEmail);

        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required.");
        }

        String emailName = companyEmail.substring(0, companyEmail.indexOf("@")).toLowerCase(Locale.ROOT);
        return emailName + "!" + dateOfBirth.format(DATE_FORMAT);
    }

    private void validateEmail(String companyEmail) {
        if (companyEmail == null || !companyEmail.contains("@") || companyEmail.startsWith("@")) {
            throw new IllegalArgumentException("A valid company email is required.");
        }
    }
}
