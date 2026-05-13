package payroll.security;

public class UserAccount {
    private String userId;
    private UserType userType;
    private String passwordHash;
    private String passwordSalt;
    private String displayName;
    private String employeeId;

    public UserAccount(String userId, UserType userType, String passwordHash, String passwordSalt, String displayName, String employeeId) {
        this.userId = userId;
        this.userType = userType;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.displayName = displayName;
        this.employeeId = employeeId;
    }

    public String getUserId() {
        return userId;
    }

    public UserType getUserType() {
        return userType;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}
