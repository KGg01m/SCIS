package com.scis.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * LoginThrottle — rate-limits login attempts per student ID to prevent
 * brute-force attacks.
 *
 * <p>After {@link #MAX_ATTEMPTS} consecutive failures the account is locked
 * for {@link #LOCKOUT_MINUTES} minutes.  A successful login resets the counter.
 */
public final class LoginThrottle {

    /** Maximum consecutive failures before lockout. */
    public static final int MAX_ATTEMPTS = 5;

    /** Lockout duration in minutes. */
    public static final int LOCKOUT_MINUTES = 15;

    private static final ConcurrentHashMap<String, AttemptRecord> RECORDS =
        new ConcurrentHashMap<>();

    private static final ScheduledExecutorService CLEANER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lockout-cleaner");
            t.setDaemon(true);
            return t;
        });

    static {
        CLEANER.scheduleAtFixedRate(
            () -> RECORDS.entrySet().removeIf(e -> e.getValue().isExpired()),
            5, 5, TimeUnit.MINUTES);
    }

    private LoginThrottle() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Records a failed login attempt for {@code studentId}.
     * Call this whenever authentication fails.
     */
    public static void recordFailure(String studentId) {
        RECORDS.compute(normalise(studentId), (k, rec) -> {
            if (rec == null) rec = new AttemptRecord();
            rec.increment();
            return rec;
        });
    }

    /**
     * Resets the failure counter for {@code studentId}.
     * Call this on successful authentication.
     */
    public static void recordSuccess(String studentId) {
        RECORDS.remove(normalise(studentId));
    }

    /**
     * Returns {@code true} if the account is currently locked out.
     */
    public static boolean isLockedOut(String studentId) {
        AttemptRecord rec = RECORDS.get(normalise(studentId));
        return rec != null && rec.isLockedOut();
    }

    /**
     * Returns the number of consecutive failures so far.
     */
    public static int failureCount(String studentId) {
        AttemptRecord rec = RECORDS.get(normalise(studentId));
        return rec == null ? 0 : rec.count;
    }

    /**
     * Returns how many minutes remain in the lockout period,
     * or 0 if not locked out.
     */
    public static long minutesRemaining(String studentId) {
        AttemptRecord rec = RECORDS.get(normalise(studentId));
        if (rec == null || !rec.isLockedOut()) return 0;
        long remaining = rec.lockedUntil - System.currentTimeMillis();
        return Math.max(0, (remaining + 59_999) / 60_000);  // ceiling
    }

    /**
     * Returns how many attempts remain before lockout.
     */
    public static int attemptsRemaining(String studentId) {
        return Math.max(0, MAX_ATTEMPTS - failureCount(studentId));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private static String normalise(String id) {
        return id == null ? "" : id.trim().toLowerCase();
    }

    private static final class AttemptRecord {
        int  count      = 0;
        long lockedUntil = 0;

        void increment() {
            count++;
            if (count >= MAX_ATTEMPTS)
                lockedUntil = System.currentTimeMillis()
                            + LOCKOUT_MINUTES * 60_000L;
        }

        boolean isLockedOut() {
            return count >= MAX_ATTEMPTS
                && System.currentTimeMillis() < lockedUntil;
        }

        /** Entry can be purged once the lockout has expired. */
        boolean isExpired() {
            return count >= MAX_ATTEMPTS
                && System.currentTimeMillis() >= lockedUntil;
        }
    }
}
