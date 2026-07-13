package com.scis.ui.teacher;

import com.scis.ui.Theme;

import com.scis.teacher.db.TeacherDataManager;
import com.scis.teacher.model.Teacher;
import com.scis.ui.UIFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * TeacherSubjectsTab — teacher manages which subjects they teach
 * and which class sections are assigned to each subject.
 */
public final class TeacherSubjectsTab {

    private TeacherSubjectsTab() {}

    public static JPanel build(Teacher teacher, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel hdr = new JLabel("My Subjects & Sections");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hdr.setForeground(Theme.text());

        // ── Add subject form ──────────────────────────────────────────────────
        JPanel formCard = buildCard("Add Subject / Section");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.card());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8); gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField subjectField = UIFactory.styledTextField("e.g. Data Structures");
        JTextField sectionField = UIFactory.styledTextField(
            "e.g. CS-A  (optional, press Enter to add more)");
        subjectField.setPreferredSize(new Dimension(260, 36));
        sectionField.setPreferredSize(new Dimension(260, 36));

        JButton addBtn    = buildBtn("Add / Update", Theme.TEACHER_TEAL);
        JButton removeBtn = buildBtn("Remove Subject", Theme.RED);

        gc.gridx=0; gc.gridy=0; form.add(formLbl("Subject Name:"), gc);
        gc.gridx=1; form.add(subjectField, gc);
        gc.gridx=2; form.add(formLbl("Class Section:"), gc);
        gc.gridx=3; form.add(sectionField, gc);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Theme.card());
        btnRow.add(addBtn); btnRow.add(removeBtn);
        gc.gridx=0; gc.gridy=1; gc.gridwidth=4; form.add(btnRow, gc);
        formCard.add(form, BorderLayout.CENTER);

        // ── Subjects table ────────────────────────────────────────────────────
        String[] cols = {"Subject", "Class Sections", "Students (est.)", "Assignments"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIFactory.styledTable(model);
        table.getTableHeader().setBackground(Theme.darkMode
            ? new Color(20, 50, 40) : new Color(236, 253, 245));
        table.setRowHeight(32);
        refreshTable(teacher, model);

        JPanel tableCard = buildCard("Current Subjects");
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Wire ──────────────────────────────────────────────────────────────
        addBtn.addActionListener(e -> {
            String subj = subjectField.getText().trim();
            String sec  = sectionField.getText().trim();
            if (subj.isEmpty()) {
                UIFactory.toast(panel, "Enter a subject name."); return;
            }
            teacher.addSubject(subj, sec.isEmpty() ? null : sec);
            TeacherDataManager.saveTeacher(teacher);
            refreshTable(teacher, model);
            subjectField.setText(""); sectionField.setText("");
            UIFactory.toast(panel, "Subject updated.");
            onDataChange.run();
        });

        removeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                // Try to remove by text field
                String subj = subjectField.getText().trim();
                if (subj.isEmpty()) { UIFactory.toast(panel, "Select a subject row or enter name."); return; }
                if (JOptionPane.showConfirmDialog(panel,
                        "Remove subject '" + subj + "' and ALL its data?",
                        "Confirm", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    teacher.removeSubject(subj);
                    TeacherDataManager.saveTeacher(teacher);
                    refreshTable(teacher, model);
                    onDataChange.run();
                }
            } else {
                String subj = (String) model.getValueAt(row, 0);
                if (JOptionPane.showConfirmDialog(panel,
                        "Remove subject '" + subj + "' and ALL its data?",
                        "Confirm", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    teacher.removeSubject(subj);
                    TeacherDataManager.saveTeacher(teacher);
                    refreshTable(teacher, model);
                    onDataChange.run();
                }
            }
        });

        JPanel topSec = new JPanel(new BorderLayout(0, 10));
        topSec.setBackground(Theme.bg());
        topSec.add(hdr,      BorderLayout.NORTH);
        topSec.add(formCard, BorderLayout.CENTER);

        panel.add(topSec,    BorderLayout.NORTH);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void refreshTable(Teacher teacher, DefaultTableModel model) {
        model.setRowCount(0);
        for (String subj : teacher.getSubjects()) {
            List<String> sections = teacher.getSectionsForSubject(subj);
            String secStr = sections.isEmpty() ? "All" : String.join(", ", sections);

            // Count distinct students in attendance log for this subject
            long students = 0;
            if (teacher.attendanceLog != null)
                students = teacher.attendanceLog.stream()
                    .filter(r -> subj.equals(r.subject))
                    .map(r -> r.studentId)
                    .distinct().count();

            long assignments = teacher.getAssignments().stream()
                .filter(a -> subj.equals(a.subject)).count();

            model.addRow(new Object[]{subj, secStr, students, assignments});
        }
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
}
