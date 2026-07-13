package com.scis.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;

/**
 * UIFactory — static factory methods that produce consistently styled
 * Swing components.  All methods read colours from {@link Theme} so they
 * automatically respect the current dark / light mode.
 */
public final class UIFactory {

    private UIFactory() {}

    private static Color readableTextOn(Color bg) {
        double luminance = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
        return luminance > 0.6 ? new Color(17, 24, 39) : Color.WHITE;
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    /** Standard action button used in tab forms. */
    public static JButton actionButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(readableTextOn(bg));
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHover(b, bg, bg.darker());
        return b;
    }

    /** Larger button used in dialogs (e.g. Register, Simulate). */
    public static JButton bigButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(readableTextOn(bg));
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(340, 44));
        b.setMaximumSize(new Dimension(340, 44));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Dashboard "quick action" sized button. */
    public static JButton sideAction(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(readableTextOn(bg));
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHover(b, bg, bg.darker());
        return b;
    }

    private static void addHover(JButton b, Color normal, Color hover) {
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            @Override
            public void mouseExited(MouseEvent e)  { b.setBackground(normal); }
        });
    }

    // ── Text fields ───────────────────────────────────────────────────────────

    public static JTextField styledTextField(String placeholder) {
        JTextField tf = new JTextField(22);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setForeground(Theme.text());
        tf.setBackground(Theme.card());
        tf.setMaximumSize(new Dimension(320, 36));
        tf.setPreferredSize(new Dimension(220, 36));
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return tf;
    }

    /** Login-screen text field with focus-highlight. */
    public static void styleLoginField(JComponent tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setPreferredSize(new Dimension(340, 44));
        tf.setMaximumSize(new Dimension(340, 44));
        tf.setAlignmentX(Component.CENTER_ALIGNMENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        tf.setBackground(Theme.surface());
        tf.setForeground(Theme.text());
        if (tf instanceof JTextField)
            ((JTextField) tf).setCaretColor(Theme.BLUE);
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.BLUE, 2, true),
                    BorderFactory.createEmptyBorder(7, 13, 7, 13)));
                tf.setBackground(Theme.card());
            }
            @Override
            public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.border(), 1, true),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
                tf.setBackground(Theme.surface());
            }
        });
    }

    // ── Combo boxes ───────────────────────────────────────────────────────────

    public static void styleCombo(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setEditable(false);
        cb.setBackground(Theme.card());
        cb.setForeground(Theme.text());
        cb.setOpaque(true);
        cb.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        cb.setMaximumSize(new Dimension(260, 36));
        cb.setPreferredSize(new Dimension(180, 36));
        cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI());
        
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setOpaque(true);
                if (index == -1) {
                    l.setBackground(Theme.card());
                    l.setForeground(Theme.text());
                    return l;
                }
                if (isSelected) {
                    l.setBackground(Theme.BLUE);
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(Theme.card());
                    l.setForeground(Theme.text());
                }
                return l;
            }
        });
    }

    // ── Tables ────────────────────────────────────────────────────────────────

    public static JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(28);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setGridColor(Theme.border());
        t.setBackground(Theme.card());
        t.setForeground(Theme.text());
        t.setShowGrid(true);
        javax.swing.table.JTableHeader th = t.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(Theme.card());
        th.setForeground(Theme.text());
        th.setOpaque(true);
        th.setReorderingAllowed(false);
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                l.setOpaque(true);
                l.setBackground(Theme.card());
                l.setForeground(Theme.text());
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.border()),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setSelectionBackground(Theme.BLUE);
        t.setSelectionForeground(Color.WHITE);

        // Override the default cell renderer so every cell respects dark/light mode.
        // Without this, Swing's DefaultTableCellRenderer resets fg/bg to its own
        // system defaults on every repaint, making text invisible in dark mode.
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(Theme.BLUE);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(Theme.card());
                    c.setForeground(Theme.text());
                }
                return c;
            }
        };
        t.setDefaultRenderer(Object.class, cellRenderer);
        return t;
    }

    // ── Cards ─────────────────────────────────────────────────────────────────

    /** Plain card with a bold title header. */
    public static JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(Theme.text());
        card.add(lbl, BorderLayout.NORTH);
        return card;
    }

    /** Card that wraps a scrollable table. */
    public static JPanel buildTableCard(JTable table, String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(Theme.text());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(Theme.border(), 1));
        scroll.setBackground(Theme.card());
        scroll.getViewport().setBackground(Theme.card());
        card.add(lbl, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** Chart wrapper card with a title label above the chart panel. */
    public static JPanel wrapChart(String title, JPanel chart) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(Theme.card());
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(Theme.text());
        wrapper.add(titleLbl, BorderLayout.NORTH);
        wrapper.add(chart, BorderLayout.CENTER);
        wrapper.setPreferredSize(new Dimension(0, 220));
        return wrapper;
    }

    // ── Labels ────────────────────────────────────────────────────────────────

    public static JLabel formLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(Theme.darkMode ? Theme.text() : Theme.muted());
        return l;
    }

    public static JLabel loginFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(Theme.text());
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(340, 22));
        return l;
    }

    public static JLabel badge(String text, Color bg) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(readableTextOn(bg));
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return l;
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    public static JSeparator sidebarSeparator() {
        JSeparator s = new JSeparator();
        s.setMaximumSize(new Dimension(190, 1));
        s.setForeground(Theme.border());
        return s;
    }

    /** Shows a plain informational toast dialog. */
    public static void toast(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Info",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /** Shows a scrollable text-area dialog. */
    public static void showTextDialog(Component parent, String title,
                                      String text, int w, int h) {
        JTextArea ta = new JTextArea(text);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setEditable(false);
        ta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ta.setBackground(Theme.card());
        ta.setForeground(Theme.text());
        ta.setCaretColor(Theme.text());
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(w, h));
        sp.getViewport().setBackground(Theme.card());
        JOptionPane.showMessageDialog(parent, sp, title,
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Recursively enforces theme colors across Swing component trees.
     * This avoids OS look-and-feel components painting unreadable defaults.
     */
    public static void applyThemeTree(Component root) {
        if (root == null) return;

        if (root instanceof JPanel || root instanceof JViewport) {
            root.setBackground(Theme.bg());
            root.setForeground(Theme.text());
        }
        if (root instanceof JLabel) {
            root.setForeground(Theme.text());
        }
        if (root instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) root;
            tc.setBackground(Theme.card());
            tc.setForeground(Theme.text());
            tc.setCaretColor(Theme.text());
        }
        if (root instanceof JComboBox) {
            @SuppressWarnings("unchecked")
            JComboBox<Object> cb = (JComboBox<Object>) root;
            cb.setBackground(Theme.card());
            cb.setForeground(Theme.text());
            cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI());
        }
        if (root instanceof JTable) {
            JTable t = (JTable) root;
            t.setBackground(Theme.card());
            t.setForeground(Theme.text());
            t.setGridColor(Theme.border());
            t.setSelectionBackground(Theme.BLUE);
            t.setSelectionForeground(Color.WHITE);
            if (t.getTableHeader() != null) {
                t.getTableHeader().setOpaque(true);
                t.getTableHeader().setBackground(Theme.card());
                t.getTableHeader().setForeground(Theme.text());
            }
        }
        if (root instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) root;
            sp.setBackground(Theme.card());
            if (sp.getViewport() != null) {
                sp.getViewport().setBackground(Theme.card());
                sp.getViewport().setForeground(Theme.text());
            }
        }

        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                applyThemeTree(child);
            }
        }
    }
}
