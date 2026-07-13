package com.scis.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * ChartPanel — a reusable {@link JPanel} that renders a vertical bar chart.
 *
 * <p>Use the two static factory methods to get attendance or performance
 * charts for a given set of subjects:
 *
 * <pre>
 *   JPanel chart = ChartPanel.attendanceChart(student);
 *   JPanel chart = ChartPanel.performanceChart(student);
 * </pre>
 */
public final class ChartPanel extends JPanel {

    @FunctionalInterface
    public interface ValueSupplier { double get(int index); }

    private final String[]      subjects;
    private final ValueSupplier valueSupplier;
    private final Color[]       barColors;
    private final double        threshold;
    private final String        thresholdLabel;

    public ChartPanel(String[] subjects,
                      ValueSupplier valueSupplier,
                      Color[] barColors,
                      double threshold,
                      String thresholdLabel) {
        this.subjects       = subjects;
        this.valueSupplier  = valueSupplier;
        this.barColors      = barColors;
        this.threshold      = threshold;
        this.thresholdLabel = thresholdLabel;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        setBackground(Theme.card());

        if (subjects.length == 0) {
            drawNoData(g2, getWidth(), getHeight());
            return;
        }
        drawBars(g2, getWidth(), getHeight());
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void drawBars(Graphics2D g2, int w, int h) {
        int pad = 40;
        int barW = Math.min(36, (w - pad - 20) / (subjects.length + 1) - 4);
        int spacing = (w - pad - 20) / (subjects.length + 1);

        // Axes
        Color gridColor = Theme.darkMode
            ? new Color(55, 65, 81) : new Color(226, 232, 240);
        g2.setColor(gridColor);
        g2.drawLine(pad, pad, pad, h - pad);
        g2.drawLine(pad, h - pad, w - 10, h - pad);

        // Grid lines & Y labels
        for (int i = 0; i <= 100; i += 20) {
            int y = h - pad - (int)(i * (h - 2 * pad) / 100.0);
            g2.setColor(Theme.darkMode
                ? new Color(40, 50, 65) : new Color(241, 245, 249));
            g2.drawLine(pad, y, w - 10, y);
            g2.setColor(Theme.muted());
            g2.setFont(new Font("Arial", Font.PLAIN, 8));
            g2.drawString("" + i, 4, y + 4);
        }

        // Bars
        for (int i = 0; i < subjects.length; i++) {
            double val = valueSupplier.get(i);
            int bh  = (int)(val * (h - 2 * pad) / 100.0);
            int x   = pad + spacing * (i + 1) - barW / 2;
            int y   = h - pad - bh;
            Color col = barColors[i % barColors.length];

            // Drop shadow
            g2.setColor(new Color(0, 0, 0, 20));
            g2.fillRoundRect(x + 2, y + 3, barW, bh, 6, 6);

            // Gradient bar
            GradientPaint gp = new GradientPaint(x, y, col.brighter(),
                                                  x, y + bh, col);
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, barW, bh, 6, 6);

            // Value label above bar
            g2.setColor(Theme.text());
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            String pct = String.format("%.0f%%", val);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pct, x + (barW - fm.stringWidth(pct)) / 2,
                          Math.max(y - 3, pad + 10));

            // Rotated subject label below bar
            g2.setColor(Theme.muted());
            g2.setFont(new Font("Arial", Font.PLAIN, 8));
            String label = subjects[i].length() > 10
                ? subjects[i].substring(0, 10) + ".."
                : subjects[i];
            drawRotated(g2, label, x + barW / 2, h - pad);
        }

        // Threshold line
        g2.setColor(Theme.RED.brighter());
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 0, new float[]{5, 4}, 0));
        int ty = h - pad - (int)(threshold * (h - 2 * pad) / 100.0);
        g2.drawLine(pad, ty, w - 10, ty);
        g2.setStroke(new BasicStroke());
        g2.setColor(Theme.RED);
        g2.setFont(new Font("Arial", Font.PLAIN, 8));
        g2.drawString(thresholdLabel, w - 30, ty - 2);
    }

    private void drawRotated(Graphics2D g2, String text, int cx, int baseY) {
        AffineTransform old = g2.getTransform();
        FontMetrics fm = g2.getFontMetrics();
        g2.rotate(-Math.toRadians(38), cx, baseY + 5);
        g2.drawString(text, cx - fm.stringWidth(text) / 2, baseY + 20);
        g2.setTransform(old);
    }

    private void drawNoData(Graphics2D g2, int w, int h) {
        g2.setColor(Theme.muted());
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        String msg = "No data available";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    // ── Color palettes ────────────────────────────────────────────────────────

    private static final Color[] ATT_PALETTE = {
        Theme.BLUE, Theme.GREEN, Theme.PURPLE,
        Theme.TEAL, Theme.ORANGE, Theme.RED, Theme.YELLOW
    };
    private static final Color[] PERF_PALETTE = {
        Theme.RED, Theme.BLUE, Theme.GREEN,
        Theme.ORANGE, Theme.PURPLE, Theme.TEAL, Theme.YELLOW
    };

    // ── Factories ─────────────────────────────────────────────────────────────

    /**
     * Creates an attendance bar chart for the given student.
     * Threshold line is drawn at 75 %.
     */
    public static ChartPanel attendanceChart(com.scis.model.Student student) {
        String[] subj = student.getSubjects();
        return new ChartPanel(subj,
            i -> student.getSubjectAttendance(subj[i]),
            ATT_PALETTE, 75.0, "75%");
    }

    /**
     * Creates a performance bar chart for the given student.
     * Threshold line is drawn at 60 %.
     */
    public static ChartPanel performanceChart(com.scis.model.Student student) {
        String[] subj = student.getSubjects();
        return new ChartPanel(subj,
            i -> student.getSubjectPerformance(subj[i]),
            PERF_PALETTE, 60.0, "60%");
    }
}
