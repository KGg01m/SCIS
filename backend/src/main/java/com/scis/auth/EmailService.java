package com.scis.auth;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.UnsupportedEncodingException;

import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * EmailService — OTP generation, delivery, and verification.
 *
 * <h3>Configuration</h3>
 * Set SMTP credentials via system properties or environment variables
 * before the first call:
 * <pre>
 *   scis.smtp.host     (default: smtp.gmail.com)
 *   scis.smtp.port     (default: 587)
 *   scis.smtp.user     — sender email address  (REQUIRED)
 *   scis.smtp.pass     — sender app password   (REQUIRED)
 *   scis.smtp.from     (default: same as user)
 * </pre>
 *
 * <h3>OTP lifecycle</h3>
 * <ol>
 *   <li>Call {@link #sendVerificationOtp(String)} or
 *       {@link #sendPasswordResetOtp(String)} — returns the 6-digit code
 *       (also sent by email). The code expires after
 *       {@link #OTP_EXPIRY_MINUTES} minutes.</li>
 *   <li>Call {@link #verifyOtp(String, String)} to confirm.</li>
 *   <li>Verified OTPs are consumed (single-use).</li>
 * </ol>
 */
public final class EmailService {

    public static final int OTP_EXPIRY_MINUTES = 10;

    private static final int OTP_LENGTH = 6;
    private static final SecureRandom RNG = new SecureRandom();

    // email -> OtpEntry
    private static final ConcurrentHashMap<String, OtpEntry> OTP_STORE =
        new ConcurrentHashMap<>();

    private static final ScheduledExecutorService CLEANER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "otp-cleaner");
            t.setDaemon(true);
            return t;
        });

    static {
        // Purge expired OTPs every minute
        CLEANER.scheduleAtFixedRate(
            () -> OTP_STORE.entrySet().removeIf(e -> e.getValue().isExpired()),
            1, 1, TimeUnit.MINUTES);
    }

    private EmailService() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates a 6-digit OTP, stores it, and emails it to {@code email}.
     *
     * @param email recipient address
     * @return the OTP string (so tests / UI can display it if SMTP is not configured)
     * @throws MessagingException if the email could not be sent
     */
    public static String sendVerificationOtp(String email) throws MessagingException, UnsupportedEncodingException {
        String otp = generateOtp();
        OTP_STORE.put(normalise(email),
            new OtpEntry(otp, OtpPurpose.VERIFICATION));

        String subject = "SCIS — Email Verification Code";
        String body    = buildEmailBody(
            "Email Verification",
            "Use the code below to verify your email address for SCIS.",
            otp,
            "This code expires in " + OTP_EXPIRY_MINUTES + " minutes.");

        send(email, subject, body);
        return otp;
    }

    /**
     * Generates a 6-digit OTP for password reset and emails it.
     *
     * @param email recipient address
     * @return the OTP string
     * @throws MessagingException if the email could not be sent
     */
    public static String sendPasswordResetOtp(String email) throws MessagingException, UnsupportedEncodingException {
        String otp = generateOtp();
        OTP_STORE.put(normalise(email),
            new OtpEntry(otp, OtpPurpose.PASSWORD_RESET));

        String subject = "SCIS — Password Reset Code";
        String body    = buildEmailBody(
            "Password Reset",
            "A password reset was requested for your SCIS account.",
            otp,
            "If you did not request this, ignore this email. "
            + "Code expires in " + OTP_EXPIRY_MINUTES + " minutes.");

        send(email, subject, body);
        return otp;
    }

    /**
     * Verifies an OTP for the given email.
     *
     * @param email      the address the OTP was sent to
     * @param enteredOtp the code the user typed
     * @return {@code true} if correct, not expired, and not yet consumed
     */
    public static boolean verifyOtp(String email, String enteredOtp) {
        OtpEntry entry = OTP_STORE.get(normalise(email));
        if (entry == null || entry.isExpired()) return false;
        if (!entry.otp.equals(enteredOtp.trim())) return false;
        OTP_STORE.remove(normalise(email));   // consume — single use
        return true;
    }

    /**
     * Returns {@code true} if there is a live (non-expired) OTP on record
     * for this email address.
     */
    public static boolean hasPendingOtp(String email) {
        OtpEntry entry = OTP_STORE.get(normalise(email));
        return entry != null && !entry.isExpired();
    }

    /**
     * Checks whether SMTP is properly configured (user + pass set).
     * The registration dialog shows a warning when this returns false.
     */
    public static boolean isConfigured() {
        String user = prop("scis.smtp.user", "");
        String pass = prop("scis.smtp.pass", "");
        
        // Return true ONLY when real SMTP credentials are present
        boolean realCreds = !user.isEmpty() && !pass.isEmpty()
                            && !pass.contains("PLACE_YOUR_REAL_APP_PASSWORD_HERE");
        if (realCreds) {
            System.out.println("[EmailService] SMTP configured — real emails will be sent via: " + user);
        } else {
            System.out.println("[EmailService] SMTP not configured — OTP printed to console (mock mode).");
        }
        return realCreds;
    }

    // ── SMTP send ─────────────────────────────────────────────────────────────

    private static void send(String to, String subject, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {
        String host = prop("scis.smtp.host", "smtp.gmail.com");
        int    port = Integer.parseInt(prop("scis.smtp.port", "587"));
        String user = prop("scis.smtp.user", "");
        String pass = prop("scis.smtp.pass", "");
        String from = prop("scis.smtp.from", user);

        // Mock mode for testing when SMTP is not configured or using placeholder
        if (user.isEmpty() || pass.isEmpty() || pass.contains("PLACE_YOUR_REAL_APP_PASSWORD_HERE")) {
            System.out.println("[EmailService] MOCK EMAIL SENT");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + htmlBody.replaceAll("<[^>]*>", "")); // Strip HTML for console
            System.out.println("---");
            return;
        }

        if (user.isEmpty() || pass.isEmpty())
            throw new MessagingException(
                "SMTP not configured. Set scis.smtp.user and scis.smtp.pass.");

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",             host);
        props.put("mail.smtp.port",             String.valueOf(port));
        props.put("mail.smtp.ssl.protocols",   "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from, "SCIS System"));
        msg.setRecipients(Message.RecipientType.TO,
            InternetAddress.parse(to));
        msg.setSubject(subject);
        msg.setContent(htmlBody, "text/html; charset=utf-8");
        Transport.send(msg);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String generateOtp() {
        int code = 100_000 + RNG.nextInt(900_000);
        return String.valueOf(code);
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String prop(String key, String def) {
        String v = System.getProperty(key);
        if (v != null && !v.isEmpty()) return v;
        // Also check environment variable: SCIS_SMTP_USER etc.
        String env = key.replace('.', '_').replace('-', '_').toUpperCase();
        v = System.getenv(env);
        return (v != null && !v.isEmpty()) ? v : def;
    }

    private static String buildEmailBody(String heading, String intro,
                                          String otp, String footer) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,sans-serif;"
            + "background:#f8fafc;padding:40px'>"
            + "<div style='max-width:480px;margin:0 auto;background:#fff;"
            + "border-radius:12px;padding:36px;box-shadow:0 4px 24px rgba(0,0,0,.08)'>"
            + "<h2 style='color:#3b82f6;margin-top:0'>SCIS — " + heading + "</h2>"
            + "<p style='color:#374151'>" + intro + "</p>"
            + "<div style='background:#f1f5f9;border-radius:8px;padding:20px;"
            + "text-align:center;margin:24px 0'>"
            + "<span style='font-size:36px;font-weight:700;letter-spacing:12px;"
            + "color:#1e293b'>" + otp + "</span></div>"
            + "<p style='color:#64748b;font-size:13px'>" + footer + "</p>"
            + "<hr style='border:none;border-top:1px solid #e2e8f0;margin:20px 0'>"
            + "<p style='color:#94a3b8;font-size:11px'>Smart Campus Intelligence System</p>"
            + "</div></body></html>";
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public enum OtpPurpose { VERIFICATION, PASSWORD_RESET }

    private static final class OtpEntry {
        final String     otp;
        final OtpPurpose purpose;
        final long       expiresAt;

        OtpEntry(String otp, OtpPurpose purpose) {
            this.otp       = otp;
            this.purpose   = purpose;
            this.expiresAt = System.currentTimeMillis()
                           + OTP_EXPIRY_MINUTES * 60_000L;
        }

        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
}
