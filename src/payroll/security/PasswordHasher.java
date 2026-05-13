package payroll.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordHasher {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String hashMd5(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = messageDigest.digest(plainTextPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hashText = new StringBuilder();

            for (byte hashByte : hashBytes) {
                hashText.append(String.format("%02x", hashByte));
            }

            return hashText.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 is not available.", exception);
        }
    }

    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        SECURE_RANDOM.nextBytes(saltBytes);
        return toHex(saltBytes);
    }

    public static String hashMd5WithSalt(String plainTextPassword, String salt) {
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("Password salt is required.");
        }

        return hashMd5(salt + ":" + plainTextPassword);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder text = new StringBuilder();

        for (byte value : bytes) {
            text.append(String.format("%02x", value));
        }

        return text.toString();
    }
}
