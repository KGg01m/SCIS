package com.scis.ui;

import com.scis.auth.PasswordUtils;
import com.scis.auth.PasswordUtils.StrengthResult;
import com.scis.auth.PasswordUtils.Strength;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * PasswordStrengthWidget — a live password-strength indicator that updates
 * every time the user types.
 *
 * <p>Attach it to any {@link JPasswordField} by calling
 * {@link #attach(JPasswordField)}.  Drop the widget panel into your form
 * layout immediately below the password field.
 *
 * <p>Visual elements:
 * <ul>
 *   <li>Four segmented strength bars (very weak → very strong)</li>
 *   <li>Strength label ("Very Weak", "Strong", etc.)</li>
 *   <li>First actionable tip (e.g. "Add uppercase letters")</li>
 * </ul>
 */
public final class PasswordStrengthWidget extends JPanel {

    // Segment colours per strength level
    private static final Color[] SEG_COLORS = {
        new Color(239, 68,  68),   // very weak  — red
        new Color(249, 115, 22),   // weak        — orange
        new Color(234, 179,  8),   // fair        — yellow
        new Color(34,  197, 94),   // strong      — green
    };
    private static final Color INACTIVE = new Color(226, 232, 240);

    // Segment panels (4 bars)
    private final JPanel[]  segments  = new JPanel[4];
    private final JLabel    strengthLabel;
    private final JLabel    tipLabel;

    private StrengthResult  lastResult;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PasswordStrengthWidget() {
        setLayout(new BorderLayout(0, 4));
        setOpaque(false);
        setMaximumSize(new Dimension(340, 52));
        setPreferredSize(new Dimension(340, 52));

        // Segment bar row
        JPanel barsRow = new JPanel(new GridLayout(1, 4, 4, 0));
        barsRow.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            segments[i] = new JPanel();
            segments[i].setBackground(INACTIVE);
            segments[i].setOpaque(true);
            segments[i].setBorder(new LineBorder(new Color(0, 0, 0, 12), 1, true));
            segments[i].setPreferredSize(new Dimension(0, 6));
            barsRow.add(segments[i]);
        }

        // Labels row
        JPanel labelsRow = new JPanel(new BorderLayout(6, 0));
        labelsRow.setOpaque(false);

        strengthLabel = new JLabel("");
        strengthLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        strengthLabel.setForeground(INACTIVE);

        tipLabel = new JLabel("");
        tipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tipLabel.setForeground(Theme.muted());
        tipLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        labelsRow.add(strengthLabel, BorderLayout.WEST);
        labelsRow.add(tipLabel,      BorderLayout.EAST);

        add(barsRow,    BorderLayout.NORTH);
        add(labelsRow,  BorderLayout.CENTER);

        // Start blank
        update("");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Wires this widget to {@code field} — typing in the field automatically
     * updates the strength display.
     */
    public void attach(JPasswordField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(field); }
            public void removeUpdate(DocumentEvent e) { refresh(field); }
            public void changedUpdate(DocumentEvent e) { refresh(field); }
        });
    }

    /** Returns the most recently computed strength result (may be null before first keystroke). */
    public StrengthResult getLastResult() { return lastResult; }

    /** Returns {@code true} when the password meets at least FAIR strength. */
    public boolean isSufficient() {
        return lastResult != null
            && lastResult.strength.score >= Strength.FAIR.score;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void refresh(JPasswordField field) {
        update(new String(field.getPassword()));
    }

    private void update(String password) {
        lastResult = PasswordUtils.analyse(password);

        if (password.isEmpty()) {
            for (JPanel seg : segments) seg.setBackground(INACTIVE);
            strengthLabel.setText("");
            strengthLabel.setForeground(INACTIVE);
            tipLabel.setText("");
            return;
        }

        int filledSegments;
        Color activeColor;
        switch (lastResult.strength) {
            case VERY_WEAK:   filledSegments = 1; activeColor = SEG_COLORS[0]; break;
            case WEAK:        filledSegments = 2; activeColor = SEG_COLORS[1]; break;
            case FAIR:        filledSegments = 3; activeColor = SEG_COLORS[2]; break;
            case STRONG:      filledSegments = 4; activeColor = SEG_COLORS[3]; break;
            case VERY_STRONG: filledSegments = 4; activeColor = SEG_COLORS[3]; break;
            default:          filledSegments = 1; activeColor = SEG_COLORS[0];
        }

        for (int i = 0; i < 4; i++) {
            segments[i].setBackground(i < filledSegments ? activeColor : INACTIVE);
        }

        strengthLabel.setText(lastResult.strength.label);
        strengthLabel.setForeground(activeColor);

        // Show first tip (if any non-"Great" tip exists)
        String tip = "";
        if (lastResult.tips.length > 0
                && !lastResult.tips[0].startsWith("Great")) {
            tip = lastResult.tips[0];
        }
        tipLabel.setText(tip);

        revalidate();
        repaint();
    }
}
