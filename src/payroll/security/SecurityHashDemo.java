package payroll.security;

public class SecurityHashDemo {
    public static void main(String[] args) {
        String demoPassword = "ABCpayroll2026!";
        PasswordHasher.hashMd5(demoPassword);
    }
}
