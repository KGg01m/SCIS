package com.scis.ui;

import com.scis.auth.EmailService;
import com.scis.db.DataManager;
import com.scis.model.Student;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * ForgotPasswordDialog — two-step password reset flow.
 *
 * <p>Step 1: User enters their email address → OTP is sent.
 * Step 2: {@link OtpVerificationDialog} verifies the code.
 * Step 3: User sets a new password (with live strength check).
 */
public final class ForgotPasswordDialog {

    private ForgotPasswordDialog() {}

    public static void show(Component parent) {
        // ── Step 1: email lookup ──────────────────────────────────────────────
        JDialog dlg = new JDialog(
            SwingUtilities.getWindowAncestor(parent),
            "Reset Password",
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 320);
        dlg.setLocationRelativeTo(parent);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.card());
        root.setBorder(BorderFactory.createEmptyBorder(32, 36, 28, 36));

        JLabel title = new JLabel("Forgot your password?");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Theme.text());

        JLabel sub = new JLabel(
            "<html>Enter the email you registered with.<br>"
            + "We'll send you a reset code.</html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(Theme.muted());

        JLabel emailLbl = UIFactory.loginFieldLabel("Email address");
        JTextField emailField = new JTextField();
        UIFactory.styleLoginField(emailField);

        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLbl.setForeground(Theme.RED);

        JButton sendBtn = UIFactory.bigButton("Send Reset Code", Theme.BLUE);

        // Assemble
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.card());
        form.add(title);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(sub);
        form.add(Box.createRigidArea(new Dimension(0, 24)));
        form.add(emailLbl);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(emailField);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(statusLbl);
        form.add(Box.createRigidArea(new Dimension(0, 16)));
        form.add(sendBtn);
        root.add(form, BorderLayout.CENTER);

        sendBtn.addActionListener(e -> {
            String email = emailField.getText().trim();
            if (email.isEmpty() || !email.contains("@")) {
                statusLbl.setText("Enter a valid email address.");
                return;
            }
            Student found = DataManager.findByEmail(email);
            if (found == null) {
                // Don't reveal whether the email exists — security best practice
                statusLbl.setForeground(Theme.muted());
                statusLbl.setText("If that email is registered, a code has been sent.");
                return;
            }
            // Send OTP
            try {
                EmailService.sendPasswordResetOtp(email);
            } catch (Exception ex) {
                statusLbl.setForeground(Theme.RED);
                statusLbl.setText("Could not send email: " + ex.getMessage());
                return;
            }
            dlg.dispose();

            // Step 2: verify OTP
            final Student target = found;
            OtpVerificationDialog.show(parent, email,
                () -> EmailService.sendPasswordResetOtp(email),
                verified -> {
                    if (Boolean.TRUE.equals(verified))
                        showResetStep(parent, target);
                });
        });

        emailField.addActionListener(e -> sendBtn.doClick());
        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── Step 3: new password form ─────────────────────────────────────────────

    private static void showResetStep(Component parent, Student student) {
        JDialog dlg = new JDialog(
            SwingUtilities.getWindowAncestor(parent),
            "Set New Password",
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 380);
        dlg.setLocationRelativeTo(parent);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.card());
        root.setBorder(BorderFactory.createEmptyBorder(32, 36, 28, 36));

        JLabel title = new JLabel("Set a new password");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Theme.text());

        JLabel sub = new JLabel("Choose a strong password for " + student.studentId);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(Theme.muted());

        JLabel pwLbl  = UIFactory.loginFieldLabel("New Password");
        JPasswordField pwField = new JPasswordField();
        UIFactory.styleLoginField(pwField);

        PasswordStrengthWidget strength = new PasswordStrengthWidget();
        strength.attach(pwField);

        JLabel conLbl = UIFactory.loginFieldLabel("Confirm Password");
        JPasswordField conField = new JPasswordField();
        UIFactory.styleLoginField(conField);

        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLbl.setForeground(Theme.RED);

        JButton saveBtn = UIFactory.bigButton("Save Password", Theme.GREEN);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.card());
        form.add(title);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(sub);
        form.add(Box.createRigidArea(new Dimension(0, 20)));
        form.add(pwLbl);
        form.add(Box.createRigidArea(new Dimension(0, 4)));
        form.add(pwField);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(strength);
        form.add(Box.createRigidArea(new Dimension(0, 12)));
        form.add(conLbl);
        form.add(Box.createRigidArea(new Dimension(0, 4)));
        form.add(conField);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(statusLbl);
        form.add(Box.createRigidArea(new Dimension(0, 16)));
        form.add(saveBtn);
        root.add(form, BorderLayout.CENTER);

        saveBtn.addActionListener(e -> {
            String pw  = new String(pwField.getPassword());
            String con = new String(conField.getPassword());
            if (pw.isEmpty()) {
                statusLbl.setText("Enter a new password.");
                return;
            }
            if (!strength.isSufficient()) {
                statusLbl.setText("Password is too weak. Please strengthen it.");
                return;
            }
            if (!pw.equals(con)) {
                statusLbl.setText("Passwords do not match.");
                return;
            }
            boolean ok = DataManager.updatePassword(student.studentId, pw);
            if (ok) {
                JOptionPane.showMessageDialog(dlg,
                    "Password updated successfully! Please log in.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
            } else {
                statusLbl.setText("Failed to update. Please try again.");
            }
        });

        dlg.add(root);
        dlg.setVisible(true);
    }
}
