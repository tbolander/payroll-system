package payroll.security;

public class SecurityHashDemo {
    public static void main(String[] args) {
        String demoPassword = "ABCpayroll2026!";
        String hashedPassword = PasswordHasher.hashMd5(demoPassword);

        System.out.println("Security Module Hashing Demo");
        System.out.println("Password: " + demoPassword);
        System.out.println("MD5 Hash: " + hashedPassword);
    }
}
