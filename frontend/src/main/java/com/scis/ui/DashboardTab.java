package com.scis.ui;

import com.scis.model.Student;
import com.scis.ml.MLPredictor;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * DashboardTab — the analytics overview panel shown on login.
 * Displays stat cards, bar charts, smart alerts, and quick actions.
 */
public final class DashboardTab {

    private DashboardTab() {}

    public static JPanel build(Student student) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Stat cards row ────────────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setBackground(Theme.bg());

        double oa       = student.getOverallAttendance();
        double op       = student.getOverallPerformance();
        double failRisk = MLPredictor.calculateFailRisk(oa, op);
        double cgpa     = MLPredictor.predictCGPA(oa, op);

        statsRow.add(statCard("Overall Attendance",
            String.format("%.1f%%", oa),
            oa >= 75 ? Theme.GREEN : oa >= 65 ? Theme.ORANGE : Theme.RED, "▦"));
        statsRow.add(statCard("Avg Performance",
            String.format("%.1f%%", op),
            op >= 60 ? Theme.GREEN : op >= 40 ? Theme.ORANGE : Theme.RED, "▤"));
        statsRow.add(statCard("Predicted CGPA",
            String.format("%.2f / 10", cgpa), Theme.BLUE, "▲"));
        statsRow.add(statCard("Fail Risk",
            String.format("%.0f%%", failRisk * 100),
            Theme.riskColor(MLPredictor.getRiskLevel(failRisk)), "⚠"));

        // Replace 4th stat card with medical leave info if any pending
        long pendingML = student.getMedicalLeaves().stream()
            .filter(com.scis.model.MedicalLeave::isPending).count();
        long approvedML = student.getMedicalLeaves().stream()
            .filter(com.scis.model.MedicalLeave::isApproved).count();

        // ── Charts row ────────────────────────────────────────────────────────
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        chartsRow.setBackground(Theme.bg());
        chartsRow.add(UIFactory.wrapChart(
            "Attendance by Subject",   ChartPanel.attendanceChart(student)));
        chartsRow.add(UIFactory.wrapChart(
            "Performance by Subject",  ChartPanel.performanceChart(student)));

        // ── Bottom row ────────────────────────────────────────────────────────
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomRow.setBackground(Theme.bg());
        bottomRow.add(buildAlertsCard(student));
        bottomRow.add(buildQuickActionsCard(student));

        panel.add(statsRow,  BorderLayout.NORTH);
        panel.add(chartsRow, BorderLayout.CENTER);
        panel.add(bottomRow, BorderLayout.SOUTH);
        return panel;
    }

    // ── Stat card ─────────────────────────────────────────────────────────────

    private static JPanel statCard(String label, String value,
                                    Color accent, String iconKey) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBackground(Theme.card());

        JPanel iconPnl = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                IconPainter.drawStatIcon(g2, iconKey, 4, 4, 28, accent);
            }
        };
        iconPnl.setOpaque(false);
        iconPnl.setPreferredSize(new Dimension(36, 36));

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valLbl.setForeground(accent);

        top.add(iconPnl, BorderLayout.WEST);
        top.add(valLbl, BorderLayout.EAST);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(Theme.muted());

        // Accent bar at top
        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(0, 3));

        card.add(bar, BorderLayout.NORTH);
        card.add(top, BorderLayout.CENTER);
        card.add(lbl, BorderLayout.SOUTH);
        return card;
    }

    // ── Alerts card ───────────────────────────────────────────────────────────

    private static JPanel buildAlertsCard(Student student) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel title = new JLabel("Smart Alerts");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(Theme.text());

        JPanel alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBackground(Theme.card());

        List<String> alerts = student.getAlerts();
        for (String a : alerts) {
            Color bg = alertBg(a);
            Color fg = alertFg(a);

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(bg);
            row.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(fg, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            row.setMaximumSize(new Dimension(10000, 45));

            JLabel dot = new JLabel("*");
            dot.setForeground(fg);
            dot.setFont(new Font("Arial", Font.BOLD, 10));

            JLabel al = new JLabel(
                "<html><body style='width:220px'>" + a + "</body></html>");
            al.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            al.setForeground(Theme.text());

            row.add(dot, BorderLayout.WEST);
            row.add(al,  BorderLayout.CENTER);
            alertsPanel.add(row);
            alertsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        if (alerts.isEmpty()) {
            JLabel none = new JLabel("[v]  No alerts — you're on track!");
            none.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            none.setForeground(Theme.GREEN);
            alertsPanel.add(none);
        }

        JScrollPane scroll = new JScrollPane(alertsPanel);
        scroll.setBorder(null);
        scroll.setBackground(Theme.card());

        card.add(title,  BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private static Color alertBg(String a) {
        if (a.contains("[LOW") || a.contains("[OVERDUE") || a.contains("[STREAK"))
            return Theme.darkMode ? new Color(69, 10, 10)  : new Color(254, 242, 242);
        if (a.contains("[DUE SOON") || a.contains("[CAUTION"))
            return Theme.darkMode ? new Color(69, 43, 5)   : new Color(255, 251, 235);
        return Theme.darkMode ? new Color(6, 32, 18) : new Color(240, 255, 247);
    }

    private static Color alertFg(String a) {
        if (a.contains("[LOW") || a.contains("[OVERDUE")) return Theme.RED;
        if (a.contains("[DUE SOON") || a.contains("[CAUTION")) return Theme.ORANGE;
        return Theme.GREEN;
    }

    // ── Quick actions card ────────────────────────────────────────────────────

    private static JPanel buildQuickActionsCard(Student student) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel title = new JLabel("Quick Actions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(Theme.text());

        JPanel btns = new JPanel(new GridLayout(2, 2, 8, 8));
        btns.setBackground(Theme.card());

        JButton recBtn  = UIFactory.sideAction("Recommendations", Theme.PURPLE);
        JButton simBtn  = UIFactory.sideAction("What-If Simulator", Theme.TEAL);
        JButton mlBtn   = UIFactory.sideAction("Run ML Analysis",  Theme.BLUE);
        JButton repBtn  = UIFactory.sideAction("Generate Report",  Theme.GREEN);

        recBtn.addActionListener(e ->
            ReportsTab.showRecommendations(card, student));
        simBtn.addActionListener(e ->
            WhatIfDialog.show(card, student));
        mlBtn.addActionListener(e ->
            UIFactory.showTextDialog(card, "ML Analysis",
                MLPredictor.predict(student), 620, 500));
        repBtn.addActionListener(e ->
            UIFactory.showTextDialog(card, "Performance Report",
                MLPredictor.generatePDFReport(student), 620, 500));

        btns.add(recBtn); btns.add(simBtn);
        btns.add(mlBtn);  btns.add(repBtn);

        card.add(title, BorderLayout.NORTH);
        card.add(btns,  BorderLayout.CENTER);
        return card;
    }
}
