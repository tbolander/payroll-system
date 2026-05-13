package payroll.security;

import java.util.Optional;
import payroll.data.PayrollDatabase;

public class AuthService {
    private final PayrollDatabase database;

    public AuthService(PayrollDatabase database) {
        this.database = database;
    }

    public Optional<UserAccount> authenticate(String userId, String password, UserType requestedUserType) {
        if (userId == null || password == null || requestedUserType == null) {
            return Optional.empty();
        }

        Optional<UserAccount> account = database.findUserAccount(userId.trim());
        if (account.isEmpty() || account.get().getUserType() != requestedUserType) {
            return Optional.empty();
        }

        String salt = account.get().getPasswordSalt();
        String enteredHash = salt == null || salt.isBlank()
                ? PasswordHasher.hashMd5(password)
                : PasswordHasher.hashMd5WithSalt(password, salt);

        if (!enteredHash.equalsIgnoreCase(account.get().getPasswordHash())) {
            return Optional.empty();
        }

        return account;
    }
}
