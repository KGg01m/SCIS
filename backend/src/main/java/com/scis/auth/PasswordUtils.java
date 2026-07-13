package com.scis.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * PasswordUtils — BCrypt hashing + password strength analysis.
 *
 * <p>All passwords are stored as BCrypt hashes (work factor 12).
 * Plain-text passwords are NEVER persisted.
 *
 * <p>The {@link Strength} enum and {@link #analyse(String)} method
 * are used by the registration UI to give live feedback.
 */
public final class PasswordUtils {

    /** BCrypt work factor — 12 is ~250 ms on a modern CPU (good balance). */
    private static final int WORK_FACTOR = 12;

    private PasswordUtils() {}

    // ── Hashing ───────────────────────────────────────────────────────────────

    /**
     * Hashes a plain-text password with BCrypt.
     *
     * @param plainText raw password entered by the user
     * @return BCrypt hash string safe to store in the database
     */
    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * @param plainText   raw password from the login form
     * @param storedHash  BCrypt hash from the database
     * @return {@code true} if the password matches
     */
    public static boolean verify(String plainText, String storedHash) {
        if (plainText == null || storedHash == null) return false;
        try {
            return BCrypt.checkpw(plainText, storedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the stored value looks like a BCrypt hash
     * (legacy plain-text passwords start with anything except "$2").
     */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith("$2");
    }

    // ── Strength analysis ─────────────────────────────────────────────────────

    public enum Strength {
        VERY_WEAK  ("Very Weak",   0),
        WEAK       ("Weak",        1),
        FAIR       ("Fair",        2),
        STRONG     ("Strong",      3),
        VERY_STRONG("Very Strong", 4);

        public final String label;
        public final int    score;

        Strength(String label, int score) {
            this.label = label;
            this.score = score;
        }
    }

    /** Detailed result returned by {@link #analyse(String)}. */
    public static class StrengthResult {
        public final Strength strength;
        public final int      score;      // 0-100
        public final String[] tips;       // actionable feedback messages

        StrengthResult(Strength strength, int score, String[] tips) {
            this.strength = strength;
            this.score    = score;
            this.tips     = tips;
        }
    }

    /**
     * Analyses a password and returns a {@link StrengthResult} with a
     * score (0–100), a {@link Strength} category, and improvement tips.
     */
    public static StrengthResult analyse(String password) {
        if (password == null || password.isEmpty())
            return new StrengthResult(Strength.VERY_WEAK, 0,
                new String[]{"Enter a password"});

        int score = 0;
        java.util.List<String> tips = new java.util.ArrayList<>();

        // Length scoring
        int len = password.length();
        if (len >= 8)  score += 10; else tips.add("Use at least 8 characters");
        if (len >= 10) score += 10;
        if (len >= 12) score += 10;
        if (len >= 16) score += 10;

        // Character variety
        boolean hasLower   = password.matches(".*[a-z].*");
        boolean hasUpper   = password.matches(".*[A-Z].*");
        boolean hasDigit   = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");

        if (hasLower)   score += 10; else tips.add("Add lowercase letters");
        if (hasUpper)   score += 10; else tips.add("Add uppercase letters");
        if (hasDigit)   score += 10; else tips.add("Add numbers");
        if (hasSpecial) score += 15; else tips.add("Add special characters (!@#$...)");

        // Bonus: all four types
        int typeCount = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0)
                      + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        if (typeCount == 4) score += 5;

        // Penalty: sequential characters
        if (hasSequential(password)) {
            score -= 10;
            tips.add("Avoid sequential characters (abc, 123)");
        }

        // Penalty: common passwords
        if (isCommon(password)) {
            score = Math.min(score, 20);
            tips.add("Avoid common passwords");
        }

        score = Math.max(0, Math.min(100, score));

        Strength strength;
        if      (score < 20) strength = Strength.VERY_WEAK;
        else if (score < 40) strength = Strength.WEAK;
        else if (score < 60) strength = Strength.FAIR;
        else if (score < 80) strength = Strength.STRONG;
        else                 strength = Strength.VERY_STRONG;

        if (tips.isEmpty()) tips.add("Great password!");
        return new StrengthResult(strength, score,
            tips.toArray(new String[0]));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static boolean hasSequential(String p) {
        for (int i = 0; i < p.length() - 2; i++) {
            int a = p.charAt(i), b = p.charAt(i + 1), c = p.charAt(i + 2);
            if (b == a + 1 && c == a + 2) return true;
            if (b == a - 1 && c == a - 2) return true;
        }
        return false;
    }

    private static final java.util.Set<String> COMMON = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "password", "password1", "123456", "12345678", "qwerty",
            "abc123", "letmein", "iloveyou", "admin", "welcome",
            "monkey", "dragon", "master", "sunshine", "princess",
            "football", "shadow", "superman", "michael", "password123"
        ));

    private static boolean isCommon(String p) {
        return COMMON.contains(p.toLowerCase());
    }
}
