package com.scis.ui;

import com.scis.auth.EmailService;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OtpVerificationDialog — a modal dialog that prompts the user to enter
 * the 6-digit OTP sent to their email address.
 *
 * <p>Features:
 * <ul>
 *   <li>Live countdown timer (counts down from 10 minutes)</li>
 *   <li>"Resend Code" button with 60-second cooldown</li>
 *   <li>Animated digit boxes that shake on wrong input</li>
 *   <li>Calls back on verified success or cancellation</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   OtpVerificationDialog.show(parent, email, resendAction, onVerified);
 * </pre>
 */
public final class OtpVerificationDialog {

    public interface ResendAction {
        /** Called when the user hits "Resend". Should send a new OTP. */
        void resend() throws Exception;
    }

    private OtpVerificationDialog() {}

    /**
     * Shows the OTP dialog.
     *
     * @param parent       parent component for positioning
     * @param email        the address the OTP was sent to (shown in the UI)
     * @param resendAction called when the user requests a new code
     * @param onVerified   called with {@code true} on success, {@code false} on cancel
     */
    public static void show(Component parent,
                             String email,
                             ResendAction resendAction,
                             Consumer<Boolean> onVerified) {

        JDialog dlg = new JDialog(
            SwingUtilities.getWindowAncestor(parent),
            "Email Verification",
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 400);
        dlg.setLocationRelativeTo(parent);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.card());
        root.setBorder(BorderFactory.createEmptyBorder(32, 36, 28, 36));

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.card());

        JLabel icon = buildEmailIcon();
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Check your email");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Theme.text());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        String truncated = truncateEmail(email);
        JLabel sub = new JLabel(
            "<html><div style='text-align:center'>We sent a 6-digit code to<br>"
            + "<b>" + truncated + "</b></div></html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(Theme.muted());
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(icon);
        header.add(Box.createRigidArea(new Dimension(0, 12)));
        header.add(title);
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        header.add(sub);

        // ── OTP input row (6 digit boxes) ─────────────────────────────────────
        JTextField[] boxes = new JTextField[6];
        JPanel boxRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        boxRow.setBackground(Theme.card());
        for (int i = 0; i < 6; i++) {
            boxes[i] = buildDigitBox();
            final int idx = i;
            boxes[i].getDocument().addDocumentListener(
                new DocumentAdapter() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        if (boxes[idx].getText().length() > 1)
                            boxes[idx].setText(
                                boxes[idx].getText().substring(0, 1));
                        if (boxes[idx].getText().length() == 1 && idx < 5)
                            boxes[idx + 1].requestFocusInWindow();
                    }
                });
            // Backspace moves focus back
            boxes[i].addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE
                            && boxes[idx].getText().isEmpty() && idx > 0) {
                        boxes[idx - 1].requestFocusInWindow();
                    }
                }
            });
            boxRow.add(boxes[i]);
        }

        // ── Status / timer ────────────────────────────────────────────────────
        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLbl.setForeground(Theme.RED);
        statusLbl.setHorizontalAlignment(SwingConstants.CENTER);
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel timerLbl = new JLabel(formatTime(EmailService.OTP_EXPIRY_MINUTES * 60));
        timerLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timerLbl.setForeground(Theme.muted());
        timerLbl.setHorizontalAlignment(SwingConstants.CENTER);

        // Countdown timer
        int[] secondsLeft = {EmailService.OTP_EXPIRY_MINUTES * 60};
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        ScheduledFuture<?>[] timerHandle = {null};
        timerHandle[0] = timer.scheduleAtFixedRate(() -> {
            secondsLeft[0]--;
            SwingUtilities.invokeLater(() -> {
                if (secondsLeft[0] > 0) {
                    timerLbl.setText("Code expires in " + formatTime(secondsLeft[0]));
                } else {
                    timerLbl.setText("Code expired — please resend.");
                    timerLbl.setForeground(Theme.RED);
                    if (timerHandle[0] != null) timerHandle[0].cancel(false);
                }
            });
        }, 1, 1, TimeUnit.SECONDS);

        // ── Buttons ───────────────────────────────────────────────────────────
        JButton verifyBtn = UIFactory.actionButton("Verify", Theme.BLUE);
        verifyBtn.setMaximumSize(new Dimension(340, 44));
        verifyBtn.setPreferredSize(new Dimension(340, 44));
        verifyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel resendRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        resendRow.setBackground(Theme.card());
        JLabel resendLbl = new JLabel("Didn't receive it?");
        resendLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resendLbl.setForeground(Theme.muted());
        JButton resendBtn = new JButton("Resend code");
        resendBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resendBtn.setForeground(Theme.BLUE);
        resendBtn.setBackground(Theme.card());
        resendBtn.setBorderPainted(false);
        resendBtn.setFocusPainted(false);
        resendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resendBtn.setContentAreaFilled(false);
        resendRow.add(resendLbl);
        resendRow.add(resendBtn);

        // ── Resend cooldown ───────────────────────────────────────────────────
        int[] resendCooldown = {0};
        resendBtn.addActionListener(ev -> {
            if (resendCooldown[0] > 0) return;
            try {
                resendAction.resend();
                secondsLeft[0] = EmailService.OTP_EXPIRY_MINUTES * 60;
                timerLbl.setForeground(Theme.muted());
                statusLbl.setForeground(Theme.GREEN);
                statusLbl.setText("New code sent!");
                resendCooldown[0] = 60;
                resendBtn.setEnabled(false);
                ScheduledExecutorService cooldownSvc =
                    Executors.newSingleThreadScheduledExecutor(
                        r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
                cooldownSvc.scheduleAtFixedRate(() -> {
                    resendCooldown[0]--;
                    SwingUtilities.invokeLater(() -> {
                        if (resendCooldown[0] <= 0) {
                            resendBtn.setEnabled(true);
                            resendBtn.setText("Resend code");
                            cooldownSvc.shutdown();
                        } else {
                            resendBtn.setText("Resend (" + resendCooldown[0] + "s)");
                        }
                    });
                }, 1, 1, TimeUnit.SECONDS);
            } catch (Exception ex) {
                statusLbl.setForeground(Theme.RED);
                statusLbl.setText("Failed to send: " + ex.getMessage());
            }
        });

        // ── Verify action ─────────────────────────────────────────────────────
        Runnable doVerify = () -> {
            StringBuilder sb = new StringBuilder();
            for (JTextField box : boxes) sb.append(box.getText().trim());
            String entered = sb.toString();
            if (entered.length() < 6) {
                statusLbl.setForeground(Theme.RED);
                statusLbl.setText("Enter all 6 digits.");
                return;
            }
            if (EmailService.verifyOtp(email, entered)) {
                timer.shutdownNow();
                dlg.dispose();
                onVerified.accept(true);
            } else {
                statusLbl.setForeground(Theme.RED);
                statusLbl.setText("Incorrect code. Please try again.");
                shakeBoxes(boxRow);
                for (JTextField box : boxes) box.setText("");
                boxes[0].requestFocusInWindow();
            }
        };
        verifyBtn.addActionListener(e -> doVerify.run());
        // Allow Enter key in last box
        boxes[5].addActionListener(e -> doVerify.run());

        // ── Assemble ──────────────────────────────────────────────────────────
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBackground(Theme.card());
        centre.add(header);
        centre.add(Box.createRigidArea(new Dimension(0, 24)));
        centre.add(boxRow);
        centre.add(Box.createRigidArea(new Dimension(0, 6)));
        centre.add(statusLbl);
        centre.add(Box.createRigidArea(new Dimension(0, 4)));
        centre.add(timerLbl);
        centre.add(Box.createRigidArea(new Dimension(0, 20)));
        centre.add(verifyBtn);
        centre.add(Box.createRigidArea(new Dimension(0, 10)));
        centre.add(resendRow);

        root.add(centre, BorderLayout.CENTER);

        dlg.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                timer.shutdownNow();
                onVerified.accept(false);
            }
        });

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JTextField buildDigitBox() {
        JTextField tf = new JTextField(1);
        tf.setFont(new Font("Segoe UI", Font.BOLD, 22));
        tf.setHorizontalAlignment(JTextField.CENTER);
        tf.setPreferredSize(new Dimension(44, 52));
        tf.setMaximumSize(new Dimension(44, 52));
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        tf.setBackground(Theme.surface());
        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.BLUE, 2, true),
                    BorderFactory.createEmptyBorder(3, 3, 3, 3)));
                tf.setBackground(Theme.card());
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.border(), 1, true),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)));
                tf.setBackground(Theme.surface());
            }
        });
        return tf;
    }

    private static JLabel buildEmailIcon() {
        JLabel lbl = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Circle background
                g2.setColor(new Color(239, 246, 255));
                g2.fillOval(0, 0, 56, 56);
                // Envelope body
                g2.setColor(Theme.BLUE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(8, 18, 40, 26, 4, 4);
                // Envelope flap
                g2.drawLine(8, 18, 28, 34);
                g2.drawLine(48, 18, 28, 34);
            }
        };
        lbl.setPreferredSize(new Dimension(56, 56));
        lbl.setMaximumSize(new Dimension(56, 56));
        return lbl;
    }

    /** Briefly shake the box row to indicate a wrong code. */
    private static void shakeBoxes(JPanel row) {
        Point origin = row.getLocation();
        Timer t = new Timer(30, null);
        int[] step = {0};
        int[] offsets = {0, -8, 8, -6, 6, -4, 4, -2, 2, 0};
        t.addActionListener(e -> {
            if (step[0] >= offsets.length) {
                row.setLocation(origin);
                ((Timer)e.getSource()).stop();
                return;
            }
            row.setLocation(origin.x + offsets[step[0]++], origin.y);
        });
        t.start();
    }

    private static String formatTime(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private static String truncateEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }
}
