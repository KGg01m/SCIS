package com.scis.ui;

import java.awt.*;

/**
 * IconPainter — all custom vector-icon drawing routines in one place.
 * No Unicode glyphs; every icon is drawn programmatically so it looks
 * crisp at any screen density.
 */
public final class IconPainter {

    private IconPainter() {}

    // ── Sidebar navigation icons ──────────────────────────────────────────────

    /**
     * Paints a sidebar nav icon of the given {@code type} at (x, y)
     * with the specified {@code size} and {@code color}.
     *
     * @param type one of: "grid", "calendar", "chart", "check",
     *             "triangle", "brain", "search", "doc"
     */
    public static void drawNavIcon(Graphics2D g2, String type,
                                   int x, int y, int size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                                     BasicStroke.JOIN_ROUND));
        int s = size, h = size;

        switch (type) {
            case "grid": {
                int sq = s / 2 - 1;
                g2.fillRoundRect(x,         y,         sq, sq, 2, 2);
                g2.fillRoundRect(x + sq + 2,y,         sq, sq, 2, 2);
                g2.fillRoundRect(x,         y + sq + 2,sq, sq, 2, 2);
                g2.fillRoundRect(x + sq + 2,y + sq + 2,sq, sq, 2, 2);
                break;
            }
            case "calendar": {
                g2.drawRoundRect(x, y + 2, s - 1, h - 3, 3, 3);
                g2.drawLine(x, y + 6, x + s - 1, y + 6);
                g2.fillRect(x + 3,     y, 2, 5);
                g2.fillRect(x + s - 5, y, 2, 5);
                g2.fillRect(x + 3, y + 9, 3, 3);
                g2.fillRect(x + 8, y + 9, 3, 3);
                break;
            }
            case "chart": {
                g2.fillRect(x,          y + h - 5, 4, 5);
                g2.fillRect(x + 5,      y + h - 9, 4, 9);
                g2.fillRect(x + 10,     y + h - 6, 4, 6);
                g2.fillRect(x + s - 4,  y,          4, h);
                break;
            }
            case "check": {
                g2.drawRoundRect(x, y, s - 1, h - 1, 3, 3);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 3,         y + h / 2,    x + s / 2 - 1, y + h - 4);
                g2.drawLine(x + s / 2 - 1, y + h - 4,   x + s - 3,     y + 3);
                break;
            }
            case "triangle": {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));
                int[] xp = {x, x + s / 2, x + s};
                int[] yp = {y + h - 2, y + 3, y + h / 2};
                g2.drawPolyline(xp, yp, 3);
                g2.fillOval(x - 2,     y + h - 4,   4, 4);
                g2.fillOval(x + s / 2 - 2, y + 1,   4, 4);
                g2.fillOval(x + s - 2, y + h / 2 - 2, 4, 4);
                break;
            }
            case "brain": {
                int cx = x + s / 2, cy = y + h / 2, r = s / 2 - 1;
                int[] hx = new int[6], hy = new int[6];
                for (int i = 0; i < 6; i++) {
                    hx[i] = (int)(cx + r * Math.cos(Math.PI / 6 + i * Math.PI / 3));
                    hy[i] = (int)(cy + r * Math.sin(Math.PI / 6 + i * Math.PI / 3));
                }
                g2.drawPolygon(hx, hy, 6);
                g2.fillOval(cx - 2, cy - 2, 4, 4);
                break;
            }
            case "search": {
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));
                int sr = s / 2 - 2;
                g2.drawOval(x, y, sr * 2, sr * 2);
                g2.drawLine(x + sr * 2 - 1, y + sr * 2 - 1, x + s, y + h);
                break;
            }
            case "doc": {
                g2.drawRoundRect(x + 1, y, s - 3, h - 1, 3, 3);
                g2.drawLine(x + 4, y + 4,  x + s - 5, y + 4);
                g2.drawLine(x + 4, y + 7,  x + s - 5, y + 7);
                g2.drawLine(x + 4, y + 10, x + s - 8, y + 10);
                break;
            }
            default:
                g2.fillRoundRect(x + 2, y + 2, s - 4, h - 4, 4, 4);
        }

        g2.setStroke(new BasicStroke());
    }

    // ── Stat-card icons ───────────────────────────────────────────────────────

    /**
     * Paints the stat-card icon for the dashboard or ML tab.
     *
     * @param type "▦" (attendance), "▤" (performance),
     *             "▲" (CGPA), "⚠" (risk)
     */
    public static void drawStatIcon(Graphics2D g2, String type,
                                    int x, int y, int size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                                     BasicStroke.JOIN_ROUND));
        int s = size, h = size;

        switch (type) {
            case "▦": {                    // attendance calendar
                g2.drawRoundRect(x, y + 3, s - 1, h - 4, 4, 4);
                g2.fillRect(x, y + 9, s, 3);
                g2.fillRect(x + 4,  y, 3, 7);
                g2.fillRect(x + s - 7, y, 3, 7);
                g2.fillRect(x + 4,     y + 14, 4, 4);
                g2.fillRect(x + s - 8, y + 14, 4, 4);
                break;
            }
            case "▤": {                    // performance bar chart
                g2.fillRect(x,         y + h - 6,  5, 6);
                g2.fillRect(x + 7,     y + h - 12, 5, 12);
                g2.fillRect(x + 14,    y + h - 9,  5, 9);
                g2.fillRect(x + s - 5, y,           5, h);
                break;
            }
            case "▲": {                    // CGPA graduation triangle
                int[] tx = {x + s / 2, x,       x + s};
                int[] ty = {y,          y + h,   y + h};
                g2.fillPolygon(tx, ty, 3);
                g2.fillRect(x + s / 2 - 4, y + h, 8, 3);
                break;
            }
            case "⚠": {                    // warning / risk
                int[] wx = {x + s / 2, x + 1,     x + s - 1};
                int[] wy = {y + 1,     y + h - 1,  y + h - 1};
                g2.drawPolygon(wx, wy, 3);
                g2.fillRect(x + s / 2 - 1, y + 7,     3, 7);
                g2.fillRect(x + s / 2 - 2, y + h - 5, 4, 4);
                break;
            }
            default:
                g2.fillOval(x + 2, y + 2, s - 4, h - 4);
        }

        g2.setStroke(new BasicStroke());
    }
}
