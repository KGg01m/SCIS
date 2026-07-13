package com.scis.ui;

import com.scis.model.Student;
import com.scis.ml.MLPredictor;
import com.scis.export.CSVExporter;
import com.scis.export.PDFExporter;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.Collections;

/**
 * MLTab — Machine Learning analytics panel.
 */
public final class MLTab {

    private MLTab() {}

    public static JPanel build(Student student) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top action bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topBar.setBackground(Theme.bg());
        JButton runBtn    = UIFactory.actionButton("Run Analysis", Theme.PURPLE);
        JButton csvBtn    = UIFactory.actionButton("Export CSV",   Theme.GREEN);
        JButton pdfBtn    = UIFactory.actionButton("Export PDF",   Theme.BLUE);
        JButton reportBtn = UIFactory.actionButton("Full Report",  Theme.TEAL);
        topBar.add(runBtn); topBar.add(csvBtn);
        topBar.add(pdfBtn); topBar.add(reportBtn);

        // Metric cards
        double att       = student.getOverallAttendance();
        double perf      = student.getOverallPerformance();
        double failRisk  = MLPredictor.calculateFailRisk(att, perf);
        double attDrop   = MLPredictor.calculateAttendanceDropRisk(student);
        double confidence = MLPredictor.calculateConfidence(student);
        double cgpa      = MLPredictor.predictCGPA(att, perf);
        String grade     = MLPredictor.predictGradeCategory(perf, att);

        JPanel cards = new JPanel(new GridLayout(1, 5, 10, 0));
        cards.setBackground(Theme.bg());
        cards.add(mlMetricCard("Predicted CGPA",
            String.format("%.2f/10", cgpa), Theme.TEAL, "▲"));
        cards.add(mlMetricCard("Grade Category",
            grade, Theme.gradeColor(grade), "▤"));
        cards.add(mlMetricCard("Fail Risk",
            String.format("%.0f%%", failRisk * 100),
            Theme.riskColor(MLPredictor.getRiskLevel(failRisk)), "⚠"));
        cards.add(mlMetricCard("Att Drop Risk",
            String.format("%.0f%%", attDrop * 100),
            attDrop > 0.5 ? Theme.RED : attDrop > 0.25 ? Theme.ORANGE : Theme.GREEN, "▦"));
        cards.add(mlMetricCard("Confidence",
            String.format("%.0f%%", confidence), Theme.PURPLE, "▲"));

        JPanel riskRow = buildRiskBar(failRisk, attDrop, grade);

        // Subject ML table
        String[] cols = {"Subject", "Attendance", "Performance",
                          "Predicted Grade", "Fail Risk", "Confidence", "Risk Status"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (String subj : student.getSubjects()) {
            double sa = student.getSubjectAttendance(subj);
            double sp = student.getSubjectPerformance(subj);
            double sr = MLPredictor.calculateFailRisk(sa, sp);
            double sc = Math.min(95, 45
                + (student.attendanceMap.getOrDefault(subj, Collections.emptyList()).size()
                   + student.marksMap.getOrDefault(subj, Collections.emptyList()).size()) * 2.5);
            tableModel.addRow(new Object[]{
                subj,
                String.format("%.1f%%", sa),
                String.format("%.1f%%", sp),
                MLPredictor.predictGradeCategory(sp, sa),
                String.format("%.0f%%", sr * 100),
                String.format("%.0f%%", sc),
                MLPredictor.getRiskIndicator(sr)
            });
        }
        JTable tbl = UIFactory.styledTable(tableModel);
        tbl.setDefaultRenderer(Object.class, riskRowRenderer());

        JTextArea resultArea = new JTextArea(
            "Click '[ML] Run Analysis' to see full ML predictions...");
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        resultArea.setEditable(false);
        resultArea.setBackground(Theme.card());
        resultArea.setForeground(Theme.text());
        resultArea.setMargin(new Insets(10, 10, 10, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            UIFactory.buildTableCard(tbl, "Subject ML Analysis"),
            new JScrollPane(resultArea));
        split.setResizeWeight(0.55);
        split.setDividerSize(6);

        // Actions
        runBtn.addActionListener(e -> {
            try { resultArea.setText(MLPredictor.predict(student)); }
            catch (Exception ex) { resultArea.setText("Error: " + ex.getMessage()); }
        });
        csvBtn.addActionListener(e -> exportCSV(panel, student));
        pdfBtn.addActionListener(e -> exportPDF(panel, student));
        reportBtn.addActionListener(e -> UIFactory.showTextDialog(panel,
            "Performance Report", MLPredictor.generatePDFReport(student), 640, 520));

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setBackground(Theme.bg());
        north.add(topBar, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);
        north.add(riskRow, BorderLayout.SOUTH);

        panel.add(north, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static JPanel mlMetricCard(String label, String value,
                                        Color color, String iconKey) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JPanel topRow = new JPanel(new BorderLayout(6, 0));
        topRow.setBackground(Theme.card());
        JPanel icPnl = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                IconPainter.drawStatIcon(g2, iconKey, 2, 4, 24, color);
            }
        };
        icPnl.setOpaque(false);
        icPnl.setPreferredSize(new Dimension(30, 30));
        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLbl.setForeground(color);
        valLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        topRow.add(icPnl, BorderLayout.WEST);
        topRow.add(valLbl, BorderLayout.EAST);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(Theme.muted());

        JPanel colorBar = new JPanel();
        colorBar.setBackground(color);
        colorBar.setPreferredSize(new Dimension(0, 3));

        card.add(colorBar, BorderLayout.NORTH);
        card.add(topRow,   BorderLayout.CENTER);
        card.add(lbl,      BorderLayout.SOUTH);
        return card;
    }

    private static JPanel buildRiskBar(double failRisk,
                                        double attDropRisk, String grade) {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(Theme.card());
        bar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setBackground(Theme.card());
        String riskLevel = MLPredictor.getRiskLevel(failRisk);
        Color riskC = Theme.riskColor(riskLevel);
        String circle = "LOW".equals(riskLevel)    ? "[OK] SAFE"
            : "MEDIUM".equals(riskLevel)           ? "[!] MODERATE RISK"
            :                                         "[!!] HIGH RISK";
        JLabel indicator = new JLabel(circle);
        indicator.setFont(new Font("Segoe UI", Font.BOLD, 13));
        indicator.setForeground(riskC);
        left.add(indicator);

        JLabel gradeChip = new JLabel("  Grade: " + grade + "  ");
        gradeChip.setOpaque(true);
        gradeChip.setBackground(Theme.gradeColor(grade));
        gradeChip.setForeground(Color.WHITE);
        gradeChip.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gradeChip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        left.add(gradeChip);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(Theme.card());
        int pct = (int)(failRisk * 100);
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(pct);
        pb.setStringPainted(true);
        pb.setString("Fail Risk: " + pct + "%");
        pb.setPreferredSize(new Dimension(220, 22));
        pb.setForeground(pct > 60 ? Theme.RED : pct > 30 ? Theme.ORANGE : Theme.GREEN);
        JLabel confLbl = new JLabel("Att Drop Risk: "
            + String.format("%.0f%%", attDropRisk * 100));
        confLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        confLbl.setForeground(Theme.muted());
        right.add(confLbl);
        right.add(pb);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private static DefaultTableCellRenderer riskRowRenderer() {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String risk = (String) t.getModel().getValueAt(r, 6);
                    if (risk.contains("HIGH"))
                        comp.setBackground(Theme.darkMode
                            ? new Color(69, 10, 10) : new Color(255, 241, 242));
                    else if (risk.contains("MODERATE"))
                        comp.setBackground(Theme.darkMode
                            ? new Color(69, 43, 5) : new Color(255, 251, 235));
                    else
                        comp.setBackground(Theme.darkMode
                            ? new Color(6, 32, 18) : new Color(240, 255, 245));
                    comp.setForeground(Theme.text());
                }
                return comp;
            }
        };
    }

    // ── Export helpers (delegated from ReportsTab too) ─────────────────────────

    static void exportCSV(Component parent, Student student) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV Report");
        chooser.setSelectedFile(new File(
            "SCIS_Report_" + student.studentId + "_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".csv")) path += ".csv";
                CSVExporter.exportPerformanceReport(student, path);
                UIFactory.toast(parent,
                    "CSV saved: " + chooser.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                    "Export failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static void exportPDF(Component parent, Student student) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save PDF Report");
        chooser.setSelectedFile(new File(
            "SCIS_Report_" + student.studentId + "_" + LocalDate.now() + ".pdf"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".pdf")) path += ".pdf";
                PDFExporter.exportReport(student, path);
                UIFactory.toast(parent,
                    "PDF saved: " + chooser.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                    "PDF export failed: " + ex.getMessage()
                    + "\n\nFalling back to text report.", null,
                    JOptionPane.ERROR_MESSAGE);
                UIFactory.showTextDialog(parent, "Performance Report",
                    MLPredictor.generatePDFReport(student), 640, 520);
            }
        }
    }
}
