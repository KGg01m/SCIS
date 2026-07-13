package com.scis.ui.teacher;

import com.scis.ui.Theme;

import com.scis.db.DataManager;
import com.scis.model.Student;
import com.scis.teacher.db.TeacherDataManager;
import com.scis.teacher.model.Teacher;
import com.scis.teacher.model.TeacherMarksRecord;
import com.scis.ui.UIFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * TeacherMarksTab — teacher enters marks for students, views logs,
 * edits and deletes entries. Marks are mirrored into the student's record.
 */
public final class TeacherMarksTab {

    private TeacherMarksTab() {}

    public static JPanel build(Teacher teacher, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Entry form card ───────────────────────────────────────────────────
        JPanel formCard = buildCard("Enter Marks");
        JPanel form     = new JPanel(new GridBagLayout());
        form.setBackground(Theme.card());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        JTextField studentIdField  = UIFactory.styledTextField("e.g. STU001");
        JTextField studentNameField= UIFactory.styledTextField("(auto-filled)");
        studentNameField.setEditable(false);
        studentNameField.setBackground(Theme.surface());

        JComboBox<String> subjectCombo = new JComboBox<>(teacher.getSubjects());
        styleCombo(subjectCombo);
        JComboBox<String> sectionCombo = new JComboBox<>();
        styleCombo(sectionCombo);
        refreshSections(teacher, subjectCombo, sectionCombo);
        subjectCombo.addActionListener(e ->
            refreshSections(teacher, subjectCombo, sectionCombo));

        JComboBox<String> examTypeCombo =
            new JComboBox<>(TeacherMarksRecord.EXAM_TYPES);
        styleCombo(examTypeCombo);

        JTextField marksField    = UIFactory.styledTextField("e.g. 78");
        JTextField maxMarksField = UIFactory.styledTextField("e.g. 100");
        JTextField examDateField = UIFactory.styledTextField(LocalDate.now().toString());
        JTextField remarksField  = UIFactory.styledTextField("Optional remarks...");

        // Auto-fill student name when ID field loses focus, also validate same college
        studentIdField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                String sid = studentIdField.getText().trim();
                if (!sid.isEmpty()) {
                    Student s = DataManager.loadStudentById(sid);
                    if (s == null) {
                        studentNameField.setText("(Not found)");
                    } else if (teacher.collegeName != null
                            && !teacher.collegeName.equalsIgnoreCase("Unknown College")
                            && !teacher.collegeName.equalsIgnoreCase(s.collegeName)) {
                        studentNameField.setText("(Not your institution)");
                        UIFactory.toast(panel,
                            "Student " + sid + " belongs to a different institution.");
                    } else {
                        studentNameField.setText(s.name);
                    }
                }
            }
        });

        // Form layout
        gc.gridx=0; gc.gridy=0; form.add(formLbl("Student ID:"), gc);
        gc.gridx=1; form.add(studentIdField, gc);
        gc.gridx=2; form.add(formLbl("Student Name:"), gc);
        gc.gridx=3; form.add(studentNameField, gc);
        gc.gridx=0; gc.gridy=1; form.add(formLbl("Subject:"), gc);
        gc.gridx=1; form.add(subjectCombo, gc);
        gc.gridx=2; form.add(formLbl("Section:"), gc);
        gc.gridx=3; form.add(sectionCombo, gc);
        gc.gridx=0; gc.gridy=2; form.add(formLbl("Exam Type:"), gc);
        gc.gridx=1; form.add(examTypeCombo, gc);
        gc.gridx=2; form.add(formLbl("Exam Date:"), gc);
        gc.gridx=3; form.add(examDateField, gc);
        gc.gridx=0; gc.gridy=3; form.add(formLbl("Marks:"), gc);
        gc.gridx=1; form.add(marksField, gc);
        gc.gridx=2; form.add(formLbl("Max Marks:"), gc);
        gc.gridx=3; form.add(maxMarksField, gc);
        gc.gridx=0; gc.gridy=4; form.add(formLbl("Remarks:"), gc);
        gc.gridx=1; gc.gridwidth=3; form.add(remarksField, gc);
        gc.gridwidth=1;

        JButton saveBtn   = buildBtn("Save",      Theme.EMERALD);
        JButton editBtn   = buildBtn("Edit",       Theme.AMBER);
        JButton deleteBtn = buildBtn("Delete",     Theme.RED);
        JButton clearBtn  = buildBtn("Clear Form", new Color(100, 110, 120));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Theme.card());
        btnRow.add(saveBtn); btnRow.add(editBtn);
        btnRow.add(deleteBtn); btnRow.add(clearBtn);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=4; form.add(btnRow, gc);
        formCard.add(form, BorderLayout.CENTER);

        // ── Marks log table ───────────────────────────────────────────────────
        String[] cols = {"Student ID","Student Name","Subject","Section",
                          "Exam Type","Marks","Max","Grade","Date","Remarks"};
        DefaultTableModel logModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable logTable = UIFactory.styledTable(logModel);
        logTable.getTableHeader().setBackground(Theme.darkMode
            ? new Color(20, 50, 40) : new Color(236, 253, 245));
        logTable.setDefaultRenderer(Object.class, gradeRowRenderer());
        refreshLogTable(teacher, logModel);

        JPanel logCard = buildCard("Marks Log");
        logCard.add(new JScrollPane(logTable), BorderLayout.CENTER);

        // ── Class performance summary ─────────────────────────────────────────
        JPanel summaryCard = buildCard("Class Performance Summary");
        JTextArea summaryArea = new JTextArea(buildSummary(teacher));
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        summaryArea.setEditable(false);
        summaryArea.setBackground(Theme.card());
        summaryArea.setForeground(Theme.text());
        summaryArea.setMargin(new Insets(8, 8, 8, 8));
        summaryCard.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        summaryCard.setPreferredSize(new Dimension(0, 180));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            logCard, summaryCard);
        split.setResizeWeight(0.65);
        split.setDividerSize(6);

        // ── Button actions ────────────────────────────────────────────────────

        saveBtn.addActionListener(e -> {
            String sid    = studentIdField.getText().trim();
            String subj   = (String) subjectCombo.getSelectedItem();
            String section= (String) sectionCombo.getSelectedItem();
            String etype  = (String) examTypeCombo.getSelectedItem();
            if (sid.isEmpty() || subj == null) {
                UIFactory.toast(panel, "Student ID and Subject are required."); return;
            }
            double marks, max;
            LocalDate examDate;
            try {
                marks    = Double.parseDouble(marksField.getText().trim());
                max      = Double.parseDouble(maxMarksField.getText().trim());
                examDate = LocalDate.parse(examDateField.getText().trim());
            } catch (Exception ex) {
                UIFactory.toast(panel, "Check marks/date format."); return;
            }
            if (max <= 0 || marks < 0 || marks > max) {
                UIFactory.toast(panel, "Marks must be between 0 and max."); return;
            }

            String sname = studentNameField.getText().trim();
            if (sname.isEmpty() || sname.equals("(Not found)") || sname.equals("(Not your institution)")) {
                Student s = DataManager.loadStudentById(sid);
                if (s == null) { UIFactory.toast(panel, "Student not found."); return; }
                // Block saving marks for students from a different institution
                if (teacher.collegeName != null
                        && !teacher.collegeName.equalsIgnoreCase("Unknown College")
                        && !teacher.collegeName.equalsIgnoreCase(s.collegeName)) {
                    UIFactory.toast(panel,
                        "Cannot save marks: student belongs to a different institution.");
                    return;
                }
                sname = s.name;
            }

            TeacherMarksRecord rec = new TeacherMarksRecord(
                sid, sname, subj, section, etype, marks, max, examDate);
            rec.remarks = remarksField.getText().trim();

            if (teacher.marksLog == null) teacher.marksLog = new ArrayList<>();
            teacher.marksLog.add(rec);
            TeacherDataManager.saveTeacher(teacher);

            // Mirror into student record
            Student s = DataManager.loadStudentById(sid);
            if (s != null) {
                s.addMarks(subj, etype, marks, max);
                DataManager.saveStudent(s);
            }

            refreshLogTable(teacher, logModel);
            summaryArea.setText(buildSummary(teacher));
            clearForm(studentIdField, studentNameField, marksField,
                maxMarksField, remarksField, examDateField);
            UIFactory.toast(panel, "Marks saved!");
            onDataChange.run();
        });

        editBtn.addActionListener(e -> {
            int row = logTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select a row to edit."); return; }
            // Pre-fill form from selected row
            studentIdField.setText((String) logModel.getValueAt(row, 0));
            studentNameField.setText((String) logModel.getValueAt(row, 1));
            subjectCombo.setSelectedItem(logModel.getValueAt(row, 2));
            examTypeCombo.setSelectedItem(logModel.getValueAt(row, 4));
            marksField.setText((String) logModel.getValueAt(row, 5));
            maxMarksField.setText((String) logModel.getValueAt(row, 6));
            examDateField.setText((String) logModel.getValueAt(row, 8));
            remarksField.setText((String) logModel.getValueAt(row, 9));
            // Delete old entry so Save re-creates it
            deleteEntryAtRow(teacher, logModel, row);
            refreshLogTable(teacher, logModel);
        });

        deleteBtn.addActionListener(e -> {
            int row = logTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select a row to delete."); return; }
            if (JOptionPane.showConfirmDialog(panel, "Delete this marks entry?",
                    "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                deleteEntryAtRow(teacher, logModel, row);
                TeacherDataManager.saveTeacher(teacher);
                refreshLogTable(teacher, logModel);
                summaryArea.setText(buildSummary(teacher));
                onDataChange.run();
            }
        });

        clearBtn.addActionListener(e ->
            clearForm(studentIdField, studentNameField, marksField,
                maxMarksField, remarksField, examDateField));

        panel.add(formCard, BorderLayout.NORTH);
        panel.add(split,    BorderLayout.CENTER);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void refreshSections(Teacher teacher,
                                         JComboBox<String> subCombo,
                                         JComboBox<String> secCombo) {
        secCombo.removeAllItems();
        String subj = (String) subCombo.getSelectedItem();
        if (subj == null) return;
        List<String> sections = teacher.getSectionsForSubject(subj);
        if (sections.isEmpty()) secCombo.addItem("All");
        else sections.forEach(secCombo::addItem);
    }

    private static void refreshLogTable(Teacher teacher,
                                         DefaultTableModel model) {
        model.setRowCount(0);
        if (teacher.marksLog == null) return;
        List<TeacherMarksRecord> sorted = new ArrayList<>(teacher.marksLog);
        sorted.sort((a, b) -> {
            if (a.examDate == null && b.examDate == null) return 0;
            if (a.examDate == null) return 1;
            if (b.examDate == null) return -1;
            return b.examDate.compareTo(a.examDate);
        });
        for (TeacherMarksRecord r : sorted)
            model.addRow(new Object[]{
                r.studentId, r.studentName, r.subject, r.classSection,
                r.examType,
                String.format("%.1f", r.marksObtained),
                String.format("%.0f", r.maxMarks),
                r.getGrade(),
                r.examDate != null ? r.examDate.toString() : "",
                r.remarks != null ? r.remarks : ""
            });
    }

    private static void deleteEntryAtRow(Teacher teacher,
                                          DefaultTableModel model, int row) {
        String sid   = (String) model.getValueAt(row, 0);
        String subj  = (String) model.getValueAt(row, 2);
        String etype = (String) model.getValueAt(row, 4);
        String mStr  = (String) model.getValueAt(row, 5);
        if (teacher.marksLog == null) return;
        teacher.marksLog.removeIf(r ->
            r.studentId.equals(sid)
            && r.subject.equals(subj)
            && r.examType.equals(etype)
            && String.format("%.1f", r.marksObtained).equals(mStr));
    }

    private static String buildSummary(Teacher teacher) {
        if (teacher.marksLog == null || teacher.marksLog.isEmpty())
            return "No marks data yet.";
        StringBuilder sb = new StringBuilder("CLASS PERFORMANCE SUMMARY\n");
        sb.append("=".repeat(50)).append("\n");
        // Group by subject + examType
        Map<String, List<Double>> grouped = new LinkedHashMap<>();
        for (TeacherMarksRecord r : teacher.marksLog) {
            String key = r.subject + " — " + r.examType;
            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                .add(r.getPercentage());
        }
        for (Map.Entry<String, List<Double>> entry : grouped.entrySet()) {
            List<Double> pcts = entry.getValue();
            double avg = pcts.stream().mapToDouble(d -> d).average().orElse(0);
            double max = pcts.stream().mapToDouble(d -> d).max().orElse(0);
            double min = pcts.stream().mapToDouble(d -> d).min().orElse(0);
            long passed = pcts.stream().filter(p -> p >= 40).count();
            sb.append(String.format("%-40s | Students: %2d | Avg: %5.1f%% | "
                + "Max: %5.1f%% | Min: %5.1f%% | Pass: %d/%d\n",
                entry.getKey(), pcts.size(), avg, max, min,
                passed, pcts.size()));
        }
        return sb.toString();
    }

    private static void clearForm(JTextField... fields) {
        for (JTextField f : fields) f.setText("");
    }

    private static DefaultTableCellRenderer gradeRowRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String grade = (String) t.getModel().getValueAt(r, 7);
                    if ("F".equals(grade))
                        comp.setBackground(Theme.statusRowBg(false));
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
        cb.setMaximumSize(new Dimension(200, 36));
        cb.setPreferredSize(new Dimension(160, 36));
    }
}
