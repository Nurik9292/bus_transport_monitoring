package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class HashedPassword extends ValueObject {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32; // 256 бит
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String hash;
    private final String salt;

    private HashedPassword(String hash, String salt) {
        this.hash = hash;
        this.salt = salt;
        validate();
    }

    public static HashedPassword fromPlainPassword(PlainPassword plainPassword) {
        String salt = generateSalt();
        String hash = hashPassword(plainPassword.getValue(), salt);
        return new HashedPassword(hash, salt);
    }

    public static HashedPassword fromHashAndSalt(String hash, String salt) {
        return new HashedPassword(hash, salt);
    }

    private static String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    private static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt.getBytes());
            byte[] hashBytes = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available: " + HASH_ALGORITHM, e);
        }
    }

    public boolean matches(PlainPassword plainPassword) {
        String candidateHash = hashPassword(plainPassword.getValue(), this.salt);
        return constantTimeEquals(this.hash, candidateHash);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    @Override
    protected void validate() {
        if (hash == null || hash.isEmpty()) {
            throw new IllegalArgumentException("Hash cannot be null or empty");
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be null or empty");
        }
    }

    public String getHash() { return hash; }
    public String getSalt() { return salt; }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{hash, salt};
    }

    @Override
    public String toString() {
        return "HashedPassword{hash=*****, salt=*****}";
    }
}