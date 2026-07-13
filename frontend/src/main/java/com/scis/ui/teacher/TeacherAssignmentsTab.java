package com.scis.ui.teacher;

import com.scis.ui.Theme;

import com.scis.teacher.db.TeacherDataManager;
import com.scis.teacher.model.Teacher;
import com.scis.teacher.model.TeacherAssignment;
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
 * TeacherAssignmentsTab — create assignments, view submissions,
 * and grade individual student work.
 */
public final class TeacherAssignmentsTab {

    private TeacherAssignmentsTab() {}

    public static JPanel build(Teacher teacher, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Create Assignment form ────────────────────────────────────────────
        JPanel formCard = buildCard("Create New Assignment");
        JPanel form     = new JPanel(new GridBagLayout());
        form.setBackground(Theme.card());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        JTextField titleField = UIFactory.styledTextField("Assignment title...");
        JTextArea  descArea   = new JTextArea(2, 20);
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descArea.setLineWrap(true); descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(209,213,219),1,true),
            BorderFactory.createEmptyBorder(4,8,4,8)));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setPreferredSize(new Dimension(300, 60));

        JComboBox<String> subjectCombo = new JComboBox<>(teacher.getSubjects());
        styleCombo(subjectCombo);
        JComboBox<String> sectionCombo = new JComboBox<>();
        styleCombo(sectionCombo);
        refreshSections(teacher, subjectCombo, sectionCombo);
        subjectCombo.addActionListener(e ->
            refreshSections(teacher, subjectCombo, sectionCombo));

        JComboBox<String> typeCombo =
            new JComboBox<>(TeacherAssignment.ASSIGNMENT_TYPES);
        styleCombo(typeCombo);

        JTextField maxMarksField = UIFactory.styledTextField("e.g. 20");
        JTextField dueDateField  = UIFactory.styledTextField(
            LocalDate.now().plusDays(7).toString());

        gc.gridx=0; gc.gridy=0; form.add(formLbl("Title:"), gc);
        gc.gridx=1; gc.gridwidth=3; form.add(titleField, gc); gc.gridwidth=1;
        gc.gridx=0; gc.gridy=1; form.add(formLbl("Description:"), gc);
        gc.gridx=1; gc.gridwidth=3; form.add(descScroll, gc); gc.gridwidth=1;
        gc.gridx=0; gc.gridy=2; form.add(formLbl("Subject:"), gc);
        gc.gridx=1; form.add(subjectCombo, gc);
        gc.gridx=2; form.add(formLbl("Section:"), gc);
        gc.gridx=3; form.add(sectionCombo, gc);
        gc.gridx=0; gc.gridy=3; form.add(formLbl("Type:"), gc);
        gc.gridx=1; form.add(typeCombo, gc);
        gc.gridx=2; form.add(formLbl("Max Marks:"), gc);
        gc.gridx=3; form.add(maxMarksField, gc);
        gc.gridx=0; gc.gridy=4; form.add(formLbl("Due Date:"), gc);
        gc.gridx=1; form.add(dueDateField, gc);

        JButton createBtn = buildBtn("Create Assignment", Theme.TEACHER_TEAL);
        JButton deleteBtn = buildBtn("Delete Selected",   Theme.RED);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Theme.card());
        btnRow.add(createBtn); btnRow.add(deleteBtn);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=4; form.add(btnRow, gc);
        formCard.add(form, BorderLayout.CENTER);

        // ── Assignments table ─────────────────────────────────────────────────
        String[] cols = {"ID","Title","Subject","Section","Type",
                          "Max Marks","Due Date","Status","Submitted","Graded"};
        DefaultTableModel asgnModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable asgnTable = UIFactory.styledTable(asgnModel);
        asgnTable.getTableHeader().setBackground(Theme.darkMode
            ? new Color(20, 50, 40) : new Color(236, 253, 245));
        asgnTable.setDefaultRenderer(Object.class, statusRenderer());
        refreshAsgnTable(teacher, asgnModel);

        JPanel asgnCard = buildCard("Assignments");
        asgnCard.add(new JScrollPane(asgnTable), BorderLayout.CENTER);

        // ── Grading panel (shown when assignment is selected) ─────────────────
        JPanel gradingCard = buildCard("Grade Submissions");
        JPanel gradingPanel = buildGradingPanel(teacher, asgnTable, asgnModel, onDataChange);
        gradingCard.add(gradingPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            asgnCard, gradingCard);
        split.setResizeWeight(0.55);
        split.setDividerSize(6);

        // ── Create assignment action ───────────────────────────────────────────
        createBtn.addActionListener(e -> {
            String t  = titleField.getText().trim();
            String subj  = (String) subjectCombo.getSelectedItem();
            String sec   = (String) sectionCombo.getSelectedItem();
            String type  = (String) typeCombo.getSelectedItem();
            if (t.isEmpty() || subj == null) {
                UIFactory.toast(panel, "Title and Subject are required."); return;
            }
            double maxM;
            LocalDate due;
            try {
                maxM = Double.parseDouble(maxMarksField.getText().trim());
                due  = LocalDate.parse(dueDateField.getText().trim());
            } catch (Exception ex) {
                UIFactory.toast(panel, "Check max marks and due date."); return;
            }
            TeacherAssignment asgn = new TeacherAssignment(
                t, descArea.getText().trim(), subj, sec, type, maxM, due);
            teacher.getAssignments().add(asgn);
            TeacherDataManager.saveTeacher(teacher);
            refreshAsgnTable(teacher, asgnModel);
            titleField.setText(""); descArea.setText(""); maxMarksField.setText("");
            dueDateField.setText(LocalDate.now().plusDays(7).toString());
            UIFactory.toast(panel, "Assignment created!");
            onDataChange.run();
        });

        deleteBtn.addActionListener(e -> {
            int row = asgnTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select an assignment."); return; }
            String id = (String) asgnModel.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(panel,
                    "Delete this assignment?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                teacher.getAssignments().removeIf(a -> a.id.equals(id));
                TeacherDataManager.saveTeacher(teacher);
                refreshAsgnTable(teacher, asgnModel);
                onDataChange.run();
            }
        });

        panel.add(formCard, BorderLayout.NORTH);
        panel.add(split,    BorderLayout.CENTER);
        return panel;
    }

    // ── Grading panel ─────────────────────────────────────────────────────────

    private static JPanel buildGradingPanel(Teacher teacher,
                                             JTable asgnTable,
                                             DefaultTableModel asgnModel,
                                             Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Theme.card());

        JLabel hint = new JLabel("Select an assignment above to grade submissions.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(Theme.muted());
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        String[] gradeCols = {"Student ID","Student Name","Submitted","Marks","Max","Grade","Feedback"};
        DefaultTableModel gradeModel = new DefaultTableModel(gradeCols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 3 || c == 6; // Marks and Feedback editable
            }
        };
        JTable gradeTable = UIFactory.styledTable(gradeModel);
        gradeTable.getTableHeader().setBackground(Theme.darkMode
            ? new Color(20, 50, 40) : new Color(236, 253, 245));

        JTextField addStudentField = UIFactory.styledTextField("Student ID to add...");
        JButton addStudentBtn  = buildBtn("Add Student", Theme.TEACHER_PURPLE);
        JButton markSubmitBtn  = buildBtn("Mark Submitted", Theme.EMERALD);
        JButton saveGradeBtn   = buildBtn("Save Grades", Theme.TEACHER_TEAL);

        JPanel gBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        gBtnRow.setBackground(Theme.card());
        gBtnRow.add(addStudentField); gBtnRow.add(addStudentBtn);
        gBtnRow.add(markSubmitBtn);   gBtnRow.add(saveGradeBtn);

        panel.add(hint,                     BorderLayout.NORTH);
        panel.add(new JScrollPane(gradeTable), BorderLayout.CENTER);
        panel.add(gBtnRow,                  BorderLayout.SOUTH);

        // Load grading table when assignment selected
        asgnTable.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = asgnTable.getSelectedRow();
            if (row == -1) return;
            String id = (String) asgnModel.getValueAt(row, 0);
            TeacherAssignment asgn = findAssignment(teacher, id);
            if (asgn == null) return;
            hint.setText("Grading: " + asgn.title
                + " · Max: " + asgn.maxMarks + " marks");
            refreshGradeTable(asgn, gradeModel);
        });

        addStudentBtn.addActionListener(e -> {
            int row = asgnTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select an assignment first."); return; }
            String asgnId = (String) asgnModel.getValueAt(row, 0);
            TeacherAssignment asgn = findAssignment(teacher, asgnId);
            if (asgn == null) return;
            String sid = addStudentField.getText().trim();
            if (sid.isEmpty()) return;
            com.scis.model.Student s = com.scis.db.DataManager.loadStudentById(sid);
            // Block students from a different institution
            if (s != null && teacher.collegeName != null
                    && !teacher.collegeName.equalsIgnoreCase("Unknown College")
                    && !teacher.collegeName.equalsIgnoreCase(s.collegeName)) {
                UIFactory.toast(panel,
                    "Student " + sid + " belongs to a different institution.");
                return;
            }
            String sname = s != null ? s.name : "(Unknown)";
            // Add to grade table if not already there
            for (int r = 0; r < gradeModel.getRowCount(); r++)
                if (sid.equals(gradeModel.getValueAt(r, 0))) { UIFactory.toast(panel,"Already added."); return; }
            gradeModel.addRow(new Object[]{sid, sname, "No", "", asgn.maxMarks, "-", ""});
            addStudentField.setText("");
        });

        markSubmitBtn.addActionListener(e -> {
            int row = gradeTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select a student row."); return; }
            gradeModel.setValueAt("Yes", row, 2);
        });

        saveGradeBtn.addActionListener(e -> {
            int asgnRow = asgnTable.getSelectedRow();
            if (asgnRow == -1) { UIFactory.toast(panel, "No assignment selected."); return; }
            String asgnId = (String) asgnModel.getValueAt(asgnRow, 0);
            TeacherAssignment asgn = findAssignment(teacher, asgnId);
            if (asgn == null) return;
            for (int r = 0; r < gradeModel.getRowCount(); r++) {
                String sid      = (String) gradeModel.getValueAt(r, 0);
                String submitted= (String) gradeModel.getValueAt(r, 2);
                String marksStr = (String) gradeModel.getValueAt(r, 3);
                String feedback = (String) gradeModel.getValueAt(r, 6);
                if ("Yes".equals(submitted)) asgn.markSubmitted(sid);
                if (marksStr != null && !marksStr.isEmpty()) {
                    try {
                        double m = Double.parseDouble(marksStr);
                        asgn.awardMarks(sid, m, feedback);
                        // Update grade cell
                        double pct = m / asgn.maxMarks * 100;
                        String grade = pct>=90?"A+":pct>=80?"A":pct>=70?"B":pct>=60?"C":pct>=50?"D":"F";
                        gradeModel.setValueAt(grade, r, 5);
                    } catch (NumberFormatException ignored) {}
                }
            }
            TeacherDataManager.saveTeacher(teacher);
            refreshAsgnTable(teacher, asgnModel);
            UIFactory.toast(panel, "Grades saved!");
            onDataChange.run();
        });

        return panel;
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    private static void refreshAsgnTable(Teacher teacher, DefaultTableModel m) {
        m.setRowCount(0);
        for (TeacherAssignment a : teacher.getAssignments())
            m.addRow(new Object[]{
                a.id, a.title, a.subject, a.classSection,
                a.assignmentType,
                String.format("%.0f", a.maxMarks),
                a.dueDate != null ? a.dueDate.toString() : "N/A",
                a.getStatusLabel(),
                String.valueOf(a.getSubmittedCount()),
                String.valueOf(a.getGradedCount())
            });
    }

    private static void refreshGradeTable(TeacherAssignment asgn,
                                            DefaultTableModel m) {
        m.setRowCount(0);
        Set<String> students = new LinkedHashSet<>(asgn.submissionMap.keySet());
        students.addAll(asgn.marksMap.keySet());
        for (String sid : students) {
            boolean sub   = asgn.isSubmitted(sid);
            double marks  = asgn.getStudentMarks(sid);
            String mStr   = marks >= 0 ? String.format("%.1f", marks) : "";
            double pct    = marks >= 0 && asgn.maxMarks > 0
                ? marks / asgn.maxMarks * 100 : -1;
            String grade  = pct>=90?"A+":pct>=80?"A":pct>=70?"B":pct>=60?"C":pct>=50?"D":pct>=0?"F":"-";
            String fb     = asgn.feedbackMap.getOrDefault(sid, "");
            com.scis.model.Student s = com.scis.db.DataManager.loadStudentById(sid);
            String sname  = s != null ? s.name : "(Unknown)";
            m.addRow(new Object[]{sid, sname, sub?"Yes":"No", mStr,
                String.format("%.0f", asgn.maxMarks), grade, fb});
        }
    }

    private static TeacherAssignment findAssignment(Teacher teacher, String id) {
        for (TeacherAssignment a : teacher.getAssignments())
            if (a.id.equals(id)) return a;
        return null;
    }

    private static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String status = (String) t.getModel().getValueAt(r, 7);
                    if ("Closed".equals(status))
                        comp.setBackground(Theme.darkMode
                            ? new Color(40, 20, 20) : new Color(255, 241, 242));
                    else if ("Due Today".equals(status) || "Due Soon".equals(status))
                        comp.setBackground(Theme.darkMode
                            ? new Color(50, 40, 5) : new Color(255, 251, 235));
                    else
                        comp.setBackground(Theme.statusRowBg(true));
                    comp.setForeground(Theme.text());
                }
                return comp;
            }
        };
    }

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
