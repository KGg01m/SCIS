package com.scis.ui;

import com.scis.model.Student;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * SidebarPanel — builds the left navigation panel.
 *
 * <p>After construction, register nav-item listeners via
 * {@link #onNavSelect(NavSelectCallback)}.
 */
public final class SidebarPanel {

    public interface NavSelectCallback {
        void onSelect(String navItem);
    }

    /** Nav items displayed in order. */
    public static final String[] NAV_ITEMS = {
        "Dashboard", "Attendance", "Marks", "Tasks",
        "Performance", "ML Analytics", "Medical Leave", "Search & Filter", "Reports"
    };

    private static final String[] NAV_ICON_TYPES = {
        "grid", "calendar", "chart", "check",
        "triangle", "brain", "doc", "search", "doc"
    };

    private SidebarPanel() {}

    // ── Build ─────────────────────────────────────────────────────────────────

    public static JPanel build(Student student,
                                String activeItem,
                                NavSelectCallback callback) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.sidebar());
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(
            0, 0, 0, 1, new Color(30, 41, 59)));

        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        // Student avatar section
        sidebar.add(buildAvatarSection(student));
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));
        sidebar.add(UIFactory.sidebarSeparator());
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        // Nav buttons
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String item     = NAV_ITEMS[i];
            final String iconType = NAV_ICON_TYPES[i];
            final boolean active  = item.equals(activeItem);

            JButton btn = buildNavButton(item, iconType, active);
            btn.addActionListener(e -> callback.onSelect(item));
            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 2)));
        }

        sidebar.add(Box.createVerticalGlue());

        // Attendance mini-bar
        sidebar.add(buildAttBar(student.getOverallAttendance()));
        sidebar.add(Box.createRigidArea(new Dimension(0, 16)));

        return sidebar;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static JPanel buildAvatarSection(Student student) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(Theme.sidebar());
        section.setMaximumSize(new Dimension(220, 100));

        // Avatar circle with initial
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(59, 130, 246, 200));
                g2.fillOval(4, 0, 52, 52);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                String init = student.name.length() > 0
                    ? String.valueOf(student.name.charAt(0)).toUpperCase()
                    : "S";
                g2.drawString(init,
                    4 + (52 - fm.stringWidth(init)) / 2,
                    (52 + fm.getAscent()) / 2 - 2);
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(60, 52));
        avatar.setMaximumSize(new Dimension(60, 52));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        String displayName = student.name.length() > 16
            ? student.name.substring(0, 16) + "…"
            : student.name;
        JLabel nameLbl = new JLabel(displayName);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel idLbl = new JLabel("ID: " + student.studentId);
        idLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        idLbl.setForeground(new Color(148, 163, 184));
        idLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        section.add(avatar);
        section.add(Box.createRigidArea(new Dimension(0, 4)));
        section.add(nameLbl);
        section.add(Box.createRigidArea(new Dimension(0, 2)));
        section.add(idLbl);
        return section;
    }

    private static JButton buildNavButton(String item,
                                           String iconType,
                                           boolean isActive) {
        JButton btn = new JButton(item) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                Color iconColor = isActive
                    ? Color.WHITE : new Color(148, 163, 184);
                IconPainter.drawNavIcon(g2, iconType,
                    18, (getHeight() - 16) / 2, 16, iconColor);
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    44, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setActionCommand(item);
        btn.setFont(new Font("Segoe UI",
            isActive ? Font.BOLD : Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 44));
        btn.setPreferredSize(new Dimension(220, 44));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(isActive ? Theme.BLUE : Theme.sidebar());
        btn.setForeground(isActive ? Color.WHITE : new Color(148, 163, 184));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!btn.getBackground().equals(Theme.BLUE))
                    btn.setBackground(new Color(30, 41, 59));
            }
            public void mouseExited(MouseEvent e) {
                if (!btn.getBackground().equals(Theme.BLUE))
                    btn.setBackground(Theme.sidebar());
            }
        });
        return btn;
    }

    private static JPanel buildAttBar(double oa) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.sidebar());
        p.setMaximumSize(new Dimension(220, 75));
        p.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));

        JLabel label = new JLabel("Overall Attendance");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(new Color(148, 163, 184));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel barBg = new JPanel(null);
        barBg.setBackground(new Color(30, 41, 59));
        barBg.setMaximumSize(new Dimension(188, 8));
        barBg.setPreferredSize(new Dimension(188, 8));
        barBg.setOpaque(true);

        int fillW = (int)(oa / 100.0 * 188);
        JPanel fill = new JPanel();
        fill.setBounds(0, 0, fillW, 8);
        fill.setBackground(oa >= 75 ? Theme.GREEN : oa >= 65 ? Theme.ORANGE : Theme.RED);
        fill.setOpaque(true);
        barBg.add(fill);
        barBg.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pctLbl = new JLabel(String.format("%.1f%%", oa));
        pctLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pctLbl.setForeground(oa >= 75 ? Theme.GREEN : oa >= 65 ? Theme.ORANGE : Theme.RED);
        pctLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(label);
        p.add(Box.createRigidArea(new Dimension(0, 5)));
        p.add(barBg);
        p.add(Box.createRigidArea(new Dimension(0, 4)));
        p.add(pctLbl);
        return p;
    }
}
