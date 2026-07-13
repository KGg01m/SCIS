package com.scis.ui.teacher;

import com.scis.ui.Theme;

import com.scis.teacher.db.TeacherDataManager;
import com.scis.teacher.model.Teacher;
import com.scis.teacher.model.TeacherAnnouncement;
import com.scis.ui.UIFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * TeacherAnnouncementsTab — post, view, and manage announcements/notices.
 */
public final class TeacherAnnouncementsTab {

    private TeacherAnnouncementsTab() {}

    public static JPanel build(Teacher teacher, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Form card ─────────────────────────────────────────────────────────
        JPanel formCard = buildCard("Post New Announcement");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.card());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField titleField = UIFactory.styledTextField("Announcement title...");
        JTextArea  msgArea    = new JTextArea(3, 30);
        msgArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        msgArea.setLineWrap(true); msgArea.setWrapStyleWord(true);
        msgArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JComboBox<String> subjectCombo = new JComboBox<>();
        subjectCombo.addItem("All Subjects");
        for (String s : teacher.getSubjects()) subjectCombo.addItem(s);
        styleCombo(subjectCombo);

        JComboBox<String> priorityCombo =
            new JComboBox<>(TeacherAnnouncement.PRIORITIES);
        styleCombo(priorityCombo);

        JTextField expiryField = UIFactory.styledTextField(
            LocalDate.now().plusDays(14).toString() + "  (or leave blank)");

        gc.gridx=0; gc.gridy=0; form.add(formLbl("Title:"), gc);
        gc.gridx=1; gc.gridwidth=3; form.add(titleField, gc); gc.gridwidth=1;
        gc.gridx=0; gc.gridy=1; form.add(formLbl("Message:"), gc);
        gc.gridx=1; gc.gridwidth=3;
        form.add(new JScrollPane(msgArea), gc); gc.gridwidth=1;
        gc.gridx=0; gc.gridy=2; form.add(formLbl("Subject:"), gc);
        gc.gridx=1; form.add(subjectCombo, gc);
        gc.gridx=2; form.add(formLbl("Priority:"), gc);
        gc.gridx=3; form.add(priorityCombo, gc);
        gc.gridx=0; gc.gridy=3; form.add(formLbl("Expiry Date:"), gc);
        gc.gridx=1; gc.gridwidth=3; form.add(expiryField, gc); gc.gridwidth=1;

        JButton postBtn   = buildBtn("Post Announcement", Theme.TEACHER_TEAL);
        JButton deleteBtn = buildBtn("Delete Selected",   Theme.RED);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Theme.card());
        btnRow.add(postBtn); btnRow.add(deleteBtn);
        gc.gridx=0; gc.gridy=4; gc.gridwidth=4; form.add(btnRow, gc);
        formCard.add(form, BorderLayout.CENTER);

        // ── Announcements table ───────────────────────────────────────────────
        String[] cols = {"Title", "Subject", "Priority", "Posted", "Expiry", "Status", "Message"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIFactory.styledTable(model);
        table.getTableHeader().setBackground(Theme.darkMode
            ? new Color(20, 50, 40) : new Color(236, 253, 245));
        table.setDefaultRenderer(Object.class, priorityRenderer());
        // Wide message column
        table.getColumnModel().getColumn(6).setPreferredWidth(280);
        refreshTable(teacher, model);

        // Detail preview panel
        JTextArea preview = new JTextArea("Select an announcement to preview.");
        preview.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        preview.setEditable(false); preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setBackground(Theme.card());
        preview.setForeground(Theme.text());
        preview.setMargin(new Insets(8, 8, 8, 8));
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0) return;
            String t  = (String) model.getValueAt(row, 0);
            String msg= (String) model.getValueAt(row, 6);
            String pri= (String) model.getValueAt(row, 2);
            String subj=(String) model.getValueAt(row, 1);
            preview.setText("[" + pri.toUpperCase() + "] " + t
                + "\nSubject: " + subj + "\n\n" + msg);
        });

        JPanel previewCard = buildCard("Preview");
        previewCard.add(new JScrollPane(preview), BorderLayout.CENTER);
        previewCard.setPreferredSize(new Dimension(0, 160));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            buildCard("All Announcements", new JScrollPane(table)),
            previewCard);
        split.setResizeWeight(0.7);
        split.setDividerSize(6);

        // ── Actions ───────────────────────────────────────────────────────────
        postBtn.addActionListener(e -> {
            String t = titleField.getText().trim();
            String msg = msgArea.getText().trim();
            if (t.isEmpty() || msg.isEmpty()) {
                UIFactory.toast(panel, "Title and Message are required."); return;
            }
            String subj  = (String) subjectCombo.getSelectedItem();
            String pri   = (String) priorityCombo.getSelectedItem();
            LocalDate expiry = null;
            String expiryStr = expiryField.getText().trim();
            if (!expiryStr.isEmpty() && !expiryStr.startsWith("(")) {
                try { expiry = LocalDate.parse(expiryStr.split("\\s")[0]); }
                catch (Exception ignored) {
                    UIFactory.toast(panel, "Invalid expiry date. Use YYYY-MM-DD or leave blank."); return;
                }
            }
            TeacherAnnouncement an = new TeacherAnnouncement(
                t, msg,
                "All Subjects".equals(subj) ? null : subj,
                null, pri, expiry);
            teacher.getAnnouncements().add(an);
            TeacherDataManager.saveTeacher(teacher);
            refreshTable(teacher, model);
            titleField.setText(""); msgArea.setText("");
            UIFactory.toast(panel, "Announcement posted!");
            onDataChange.run();
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select an announcement."); return; }
            if (JOptionPane.showConfirmDialog(panel,
                    "Delete this announcement?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                // Match by title + posted date (sufficient for uniqueness)
                String t2   = (String) model.getValueAt(row, 0);
                String posted=(String) model.getValueAt(row, 3);
                teacher.getAnnouncements().removeIf(an ->
                    an.title.equals(t2)
                    && (an.postedDate != null
                        && an.postedDate.toString().equals(posted)));
                TeacherDataManager.saveTeacher(teacher);
                refreshTable(teacher, model);
                onDataChange.run();
            }
        });

        panel.add(formCard, BorderLayout.NORTH);
        panel.add(split,    BorderLayout.CENTER);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void refreshTable(Teacher teacher, DefaultTableModel model) {
        model.setRowCount(0);
        List<TeacherAnnouncement> list = teacher.getAnnouncements();
        for (int i = list.size() - 1; i >= 0; i--) {
            TeacherAnnouncement an = list.get(i);
            String status = an.isExpired() ? "Expired" : "Active";
            model.addRow(new Object[]{
                an.title,
                an.subject != null ? an.subject : "All",
                an.priority,
                an.postedDate != null ? an.postedDate.toString() : "",
                an.expiryDate != null ? an.expiryDate.toString() : "Never",
                status,
                an.message
            });
        }
    }

    private static DefaultTableCellRenderer priorityRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String priority = (String) t.getModel().getValueAt(r, 2);
                    String status   = (String) t.getModel().getValueAt(r, 5);
                    if ("Expired".equals(status))
                        comp.setBackground(Theme.darkMode
                            ? new Color(30, 30, 35) : new Color(248, 248, 252));
                    else if ("High".equals(priority))
                        comp.setBackground(Theme.darkMode
                            ? new Color(55, 20, 5) : new Color(255, 251, 235));
                    else
                        comp.setBackground(Theme.statusRowBg(true));
                    comp.setForeground(Theme.text());
                }
                return comp;
            }
        };
    }

    private static JPanel buildCard(String title) {
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

    private static JPanel buildCard(String title, JComponent content) {
        JPanel card = buildCard(title);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JButton buildBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE); b.setBackground(bg);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JLabel formLbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(Theme.muted());
        return l;
    }

    private static void styleCombo(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setBackground(Theme.card());
        cb.setForeground(Theme.text());
        cb.setPreferredSize(new Dimension(160, 34));
    }
}
