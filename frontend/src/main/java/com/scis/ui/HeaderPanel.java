package com.scis.ui;

import com.scis.model.Student;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * HeaderPanel — builds the top application bar containing the app name,
 * risk badge, dark-mode toggle, user info, and logout button.
 */
public final class HeaderPanel {

    public interface LogoutCallback  { void onLogout(); }
    public interface ThemeCallback   { void onToggle(); }

    private HeaderPanel() {}

    public static JPanel build(Student student,
                                LogoutCallback onLogout,
                                ThemeCallback  onThemeToggle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.surface());
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        // Left — app name
        JLabel appLbl = new JLabel("SCIS");
        appLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        appLbl.setForeground(Theme.BLUE);

        // Right controls
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(Theme.surface());

        right.add(buildRiskBadge(student.getRiskLevel()));
        right.add(buildDarkToggle(onThemeToggle));
        right.add(buildUserLabel(student));
        right.add(buildLogoutButton(student, onLogout));

        header.add(appLbl, BorderLayout.WEST);
        header.add(right,  BorderLayout.EAST);
        return header;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static JPanel buildRiskBadge(String risk) {
        final Color riskC = Theme.riskColor(risk);
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(riskC);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.fillOval(8, getHeight() / 2 - 5, 10, 10);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                String label = risk + " RISK";
                g2.drawString(label, 24,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(110, 30));
        return badge;
    }

    private static JToggleButton buildDarkToggle(ThemeCallback callback) {
        JToggleButton toggle = new JToggleButton(
            Theme.darkMode ? "Light Mode" : "Dark Mode");
        toggle.setSelected(Theme.darkMode);
        toggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        toggle.setBackground(Theme.darkMode
            ? new Color(55, 65, 81) : new Color(241, 245, 249));
        toggle.setForeground(Theme.text());
        toggle.setFocusPainted(false);
        toggle.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> callback.onToggle());
        return toggle;
    }

    private static JLabel buildUserLabel(Student student) {
        JLabel lbl = new JLabel(student.name + "  |  "
            + student.department + " Sem " + student.semester);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(Theme.muted());
        return lbl;
    }

    private static JButton buildLogoutButton(Student student,
                                              LogoutCallback callback) {
        JButton btn = new JButton("Logout");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(new Color(239, 68, 68));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(btn, "Logout?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                callback.onLogout();
            }
        });
        return btn;
    }
}
