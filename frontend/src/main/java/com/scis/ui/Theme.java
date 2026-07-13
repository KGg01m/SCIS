package com.scis.ui;

import java.awt.Color;

/**
 * Theme — centralises all color constants and dark/light mode helpers.
 * Every UI module reads colors through this class, so switching themes
 * only requires toggling {@link #darkMode} and setting the {@link #currentRole}.
 */
public final class Theme {

    public enum Role {
        STUDENT, TEACHER
    }

    public static Role currentRole = Role.STUDENT;

    // ── Global properties ───────────────────────────────────────────────────
    public static boolean darkMode = false;

    // ── Accent palette (Combined) ───────────────────────────────────────────
    public static final Color BLUE       = new Color(59,  130, 246);
    public static final Color BLUE_DARK  = new Color(37,   99, 235);
    public static final Color EMERALD    = new Color(16,  185, 129);
    public static final Color GREEN      = new Color(34,  197,  94);
    public static final Color ORANGE     = new Color(251, 146,  60);
    public static final Color TEACHER_ORANGE = new Color(249, 115,  22);
    public static final Color RED        = new Color(239,  68,  68);
    public static final Color PURPLE     = new Color(168,  85, 247);
    public static final Color TEACHER_PURPLE = new Color(139,  92, 246);
    public static final Color TEAL       = new Color(20,  184, 166);
    public static final Color TEACHER_TEAL = new Color(13,  148, 136);
    public static final Color TEACHER_TEAL_DARK = new Color(15,  118, 110);
    public static final Color YELLOW     = new Color(234, 179,   8);
    public static final Color AMBER      = new Color(245, 158,  11);

    // ── Light mode ─────────────────────────────────────────────────────────
    private static final Color LT_BG_S      = new Color(248, 250, 252);
    private static final Color LT_BG_T      = new Color(240, 253, 250);
    private static final Color LT_SURFACE   = Color.WHITE;
    private static final Color LT_SIDEBAR   = new Color(15, 23, 42); // Both
    private static final Color LT_CARD      = Color.WHITE;
    private static final Color LT_BORDER_S  = new Color(226, 232, 240);
    private static final Color LT_BORDER_T  = new Color(209, 250, 229);
    private static final Color LT_TEXT      = new Color(15, 23, 42);
    private static final Color LT_MUTED     = new Color(100, 116, 139);

    // ── Dark mode ──────────────────────────────────────────────────────────
    private static final Color DK_BG_S      = new Color(8, 12, 24);
    private static final Color DK_BG_T      = new Color(7, 16, 14);
    private static final Color DK_SURFACE_S = new Color(20, 29, 46);
    private static final Color DK_SURFACE_T = new Color(16, 30, 26);
    private static final Color DK_SIDEBAR_S = new Color(5, 8, 18);
    private static final Color DK_SIDEBAR_T = new Color(4, 12, 10);
    private static final Color DK_CARD_S    = new Color(24, 34, 53);
    private static final Color DK_CARD_T    = new Color(19, 35, 30);
    private static final Color DK_BORDER_S  = new Color(79, 94, 121);
    private static final Color DK_BORDER_T  = new Color(54, 86, 79);
    private static final Color DK_TEXT_S    = new Color(248, 251, 255);
    private static final Color DK_TEXT_T    = new Color(242, 255, 249);
    private static final Color DK_MUTED_S   = new Color(188, 200, 222);
    private static final Color DK_MUTED_T   = new Color(166, 206, 194);

    private Theme() {}

    public static Color bg()      { return darkMode ? (currentRole == Role.STUDENT ? DK_BG_S : DK_BG_T) : (currentRole == Role.STUDENT ? LT_BG_S : LT_BG_T); }
    public static Color surface() { return darkMode ? (currentRole == Role.STUDENT ? DK_SURFACE_S : DK_SURFACE_T) : LT_SURFACE; }
    public static Color sidebar() { return darkMode ? (currentRole == Role.STUDENT ? DK_SIDEBAR_S : DK_SIDEBAR_T) : LT_SIDEBAR; }
    public static Color card()    { return darkMode ? (currentRole == Role.STUDENT ? DK_CARD_S : DK_CARD_T) : LT_CARD; }
    public static Color border()  { return darkMode ? (currentRole == Role.STUDENT ? DK_BORDER_S : DK_BORDER_T) : (currentRole == Role.STUDENT ? LT_BORDER_S : LT_BORDER_T); }
    public static Color text()    { return darkMode ? (currentRole == Role.STUDENT ? DK_TEXT_S : DK_TEXT_T) : LT_TEXT; }
    public static Color muted()   { return darkMode ? (currentRole == Role.STUDENT ? DK_MUTED_S : DK_MUTED_T) : LT_MUTED; }

    public static Color getPrimary() { return currentRole == Role.STUDENT ? BLUE : TEACHER_TEAL; }

    // ── Semantic helpers ──────────────────────────────────────────────────────
    public static Color riskColor(String level) {
        if (level == null) return GREEN;
        switch (level.toUpperCase()) {
            case "HIGH": case "CRITICAL": return RED;
            case "MEDIUM": return ORANGE;
            default: return GREEN;
        }
    }

    public static Color gradeColor(String g) {
        switch (g) {
            case "A": return currentRole == Role.STUDENT ? GREEN : EMERALD;
            case "B": return currentRole == Role.STUDENT ? TEAL : TEACHER_TEAL;
            case "C": return ORANGE;
            case "D": return new Color(234, 88, 12);
            default:  return RED;
        }
    }

    public static Color statusRowBg(boolean passed) {
        if (currentRole == Role.STUDENT) {
            return passed
                ? (darkMode ? new Color(6, 32, 18)   : new Color(240, 255, 245))
                : (darkMode ? new Color(69, 10, 10)  : new Color(255, 241, 242));
        } else {
            return passed
                ? (darkMode ? new Color(6, 35, 25)   : new Color(236, 253, 245))
                : (darkMode ? new Color(60, 10, 10)  : new Color(255, 241, 242));
        }
    }

    public static void syncUIManager() {
        Color b = bg(), c = card(), f = text();
        javax.swing.UIManager.put("Panel.background", b);
        javax.swing.UIManager.put("Label.foreground", f);
        javax.swing.UIManager.put("OptionPane.background", c);
        javax.swing.UIManager.put("OptionPane.messageForeground", f);
        javax.swing.UIManager.put("OptionPane.foreground", f);
        javax.swing.UIManager.put("TextArea.background", c);
        javax.swing.UIManager.put("TextArea.foreground", f);
        javax.swing.UIManager.put("TextArea.caretForeground", f);
        javax.swing.UIManager.put("TextPane.background", c);
        javax.swing.UIManager.put("TextPane.foreground", f);
        javax.swing.UIManager.put("EditorPane.background", c);
        javax.swing.UIManager.put("EditorPane.foreground", f);
        javax.swing.UIManager.put("ScrollPane.background", b);
        javax.swing.UIManager.put("Viewport.background", b);
        javax.swing.UIManager.put("Button.background", c);
        javax.swing.UIManager.put("Button.foreground", f);
        javax.swing.UIManager.put("ToggleButton.background", c);
        javax.swing.UIManager.put("ToggleButton.foreground", f);
        javax.swing.UIManager.put("CheckBox.background", b);
        javax.swing.UIManager.put("CheckBox.foreground", f);
        javax.swing.UIManager.put("RadioButton.background", b);
        javax.swing.UIManager.put("RadioButton.foreground", f);
        javax.swing.UIManager.put("List.background", c);
        javax.swing.UIManager.put("List.foreground", f);
        javax.swing.UIManager.put("List.selectionBackground", BLUE);
        javax.swing.UIManager.put("List.selectionForeground", Color.WHITE);
        javax.swing.UIManager.put("ComboBox.background", c);
        javax.swing.UIManager.put("ComboBox.foreground", f);
        javax.swing.UIManager.put("ComboBox.selectionBackground", BLUE);
        javax.swing.UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        javax.swing.UIManager.put("TextField.background", c);
        javax.swing.UIManager.put("TextField.foreground", f);
        javax.swing.UIManager.put("TextField.caretForeground", f);
        javax.swing.UIManager.put("PasswordField.background", c);
        javax.swing.UIManager.put("PasswordField.foreground", f);
        javax.swing.UIManager.put("PasswordField.caretForeground", f);
        javax.swing.UIManager.put("TableHeader.background", surface());
        javax.swing.UIManager.put("TableHeader.foreground", f);
        javax.swing.UIManager.put("Table.background", c);
        javax.swing.UIManager.put("Table.foreground", f);
        javax.swing.UIManager.put("Table.selectionBackground", BLUE);
        javax.swing.UIManager.put("Table.selectionForeground", Color.WHITE);
    }
}
