package com.scis.ui;

import com.scis.export.CSVExporter;
import com.scis.model.Student;
import com.scis.ml.MLPredictor;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDate;

/**
 * ReportsTab — report generation panel with download buttons and preview.
 */
public final class ReportsTab {

    private ReportsTab() {}

    public static JPanel build(Student student) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel hdr = new JLabel("Report Generation");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hdr.setForeground(Theme.text());

        // Report cards grid
        JPanel cardsGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        cardsGrid.setBackground(Theme.bg());

        cardsGrid.add(reportCard(
            "[G] Full Performance Report", "CSV",
            "Complete student data with ML predictions", Theme.GREEN,
            e -> MLTab.exportCSV(panel, student)));
        cardsGrid.add(reportCard(
            "[F] PDF Performance Report", "PDF",
            "Formatted PDF with predictions & recommendations", Theme.BLUE,
            e -> MLTab.exportPDF(panel, student)));
        cardsGrid.add(reportCard(
            "[C] Attendance Report", "CSV",
            "All attendance records subject-wise", Theme.TEAL,
            e -> exportAttendanceCSV(panel, student)));
        cardsGrid.add(reportCard(
            "[G] Marks Report", "CSV",
            "All marks records with grades", Theme.PURPLE,
            e -> exportMarksCSV(panel, student)));
        cardsGrid.add(reportCard(
            "[ML] ML Analysis Text", "TXT",
            "Full ML prediction report as text", Theme.ORANGE,
            e -> UIFactory.showTextDialog(panel, "ML Analysis",
                MLPredictor.predict(student), 640, 520)));
        cardsGrid.add(reportCard(
            "Recommendations", "VIEW",
            "Smart recommendations based on performance", Theme.YELLOW,
            e -> showRecommendations(panel, student)));

        JPanel topSec = new JPanel(new BorderLayout(0, 14));
        topSec.setBackground(Theme.bg());
        topSec.add(hdr, BorderLayout.NORTH);
        topSec.add(cardsGrid, BorderLayout.CENTER);

        // Preview pane
        JPanel previewCard = UIFactory.buildCard("[R]  Report Preview");
        JTextArea preview = new JTextArea(MLPredictor.generatePDFReport(student));
        preview.setFont(new Font("Monospaced", Font.PLAIN, 11));
        preview.setEditable(false);
        preview.setBackground(Theme.card());
        preview.setForeground(Theme.text());
        preview.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane previewScroll = new JScrollPane(preview);
        previewScroll.setPreferredSize(new Dimension(0, 220));
        previewCard.add(previewScroll, BorderLayout.CENTER);

        panel.add(topSec, BorderLayout.NORTH);
        panel.add(previewCard, BorderLayout.CENTER);
        return panel;
    }

    // ── Report card ───────────────────────────────────────────────────────────

    private static JPanel reportCard(String title, String format,
                                      String desc, Color color,
                                      ActionListener action) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBackground(Theme.card());
        JLabel fmt = new JLabel("  " + format + "  ");
        fmt.setOpaque(true);
        fmt.setBackground(color);
        fmt.setForeground(Color.WHITE);
        fmt.setFont(new Font("Segoe UI", Font.BOLD, 10));
        fmt.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        JLabel tl = new JLabel(title);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tl.setForeground(Theme.text());
        top.add(tl, BorderLayout.CENTER);
        top.add(fmt, BorderLayout.EAST);

        JLabel dl = new JLabel(
            "<html><body style='width:150px;color:gray;'>" + desc + "</body></html>");
        dl.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JButton btn = UIFactory.actionButton("Download / Generate", color);
        btn.addActionListener(action);

        card.add(top, BorderLayout.NORTH);
        card.add(dl,  BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }

    // ── Export helpers ────────────────────────────────────────────────────────

    private static void exportAttendanceCSV(Component parent, Student student) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(
            "SCIS_Attendance_" + student.studentId + "_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".csv")) path += ".csv";
                CSVExporter.exportAttendance(student, path);
                UIFactory.toast(parent, "Attendance CSV saved!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                    "Export failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void exportMarksCSV(Component parent, Student student) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(
            "SCIS_Marks_" + student.studentId + "_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".csv")) path += ".csv";
                CSVExporter.exportMarks(student, path);
                UIFactory.toast(parent, "Marks CSV saved!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                    "Export failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Recommendation dialog ─────────────────────────────────────────────────

    static void showRecommendations(Component parent, Student student) {
        StringBuilder sb = new StringBuilder(
            "Smart Recommendations for " + student.name + ":\n\n");
        java.util.List<String> r = student.getRecommendations();
        for (int i = 0; i < r.size(); i++)
            sb.append((i + 1)).append(". ").append(r.get(i)).append("\n\n");
        UIFactory.showTextDialog(parent, "Smart Recommendations",
            sb.toString(), 500, 380);
    }
}
