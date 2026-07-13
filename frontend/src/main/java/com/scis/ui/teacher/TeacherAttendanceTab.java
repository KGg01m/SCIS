package com.scis.ui.teacher;

import com.scis.ui.Theme;

import com.scis.db.DataManager;
import com.scis.model.AttendanceRecord;
import com.scis.model.Student;
import com.scis.teacher.db.TeacherDataManager;
import com.scis.teacher.model.Teacher;
import com.scis.teacher.model.TeacherAttendanceRecord;
import com.scis.ui.UIFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TeacherAttendanceTab — lets a teacher:
 *  - Select subject + class section + date
 *  - Mark each enrolled student as Present / Absent with optional remarks
 *  - Bulk-save the entire class in one click (writes to teacher log AND student records)
 *  - View / edit / delete past attendance entries they have logged
 */
public final class TeacherAttendanceTab {

    private TeacherAttendanceTab() {}

    public static JPanel build(Teacher teacher, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Top: session selector ─────────────────────────────────────────────
        JPanel sessionCard = buildCard("Mark Attendance — Select Session");

        JPanel sessionForm = new JPanel(new GridBagLayout());
        sessionForm.setBackground(Theme.card());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        // Subject combo
        JComboBox<String> subjectCombo = new JComboBox<>(teacher.getSubjects());
        styleCombo(subjectCombo);

        // Class/section combo (populated from teacher's subject map)
        JComboBox<String> sectionCombo = new JComboBox<>();
        styleCombo(sectionCombo);
        refreshSections(teacher, subjectCombo, sectionCombo);
        subjectCombo.addActionListener(e ->
            refreshSections(teacher, subjectCombo, sectionCombo));

        // Date picker (text field defaulting to today)
        JTextField dateField = UIFactory.styledTextField(LocalDate.now().toString());

        // Student ID list (comma-separated or one per line)
        JTextField studentIdsField = UIFactory.styledTextField(
            "e.g. STU001,STU002,STU003  (or leave blank to enter manually)");
        studentIdsField.setPreferredSize(new Dimension(340, 36));

        JButton loadBtn = buildBtn("Load Students", Theme.TEACHER_TEAL);

        gc.gridx = 0; gc.gridy = 0; sessionForm.add(formLbl("Subject:"), gc);
        gc.gridx = 1; sessionForm.add(subjectCombo, gc);
        gc.gridx = 2; sessionForm.add(formLbl("Section:"), gc);
        gc.gridx = 3; sessionForm.add(sectionCombo, gc);
        gc.gridx = 0; gc.gridy = 1; sessionForm.add(formLbl("Date (YYYY-MM-DD):"), gc);
        gc.gridx = 1; gc.gridwidth = 2; sessionForm.add(dateField, gc);
        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 2; sessionForm.add(formLbl("Student IDs:"), gc);
        gc.gridx = 1; gc.gridwidth = 2; sessionForm.add(studentIdsField, gc);
        gc.gridwidth = 1;
        gc.gridx = 3; gc.gridy = 2; sessionForm.add(loadBtn, gc);
        sessionCard.add(sessionForm, BorderLayout.CENTER);

        // ── Middle: class roll table ──────────────────────────────────────────
        String[] cols = {"Student ID", "Student Name", "Status", "Remarks"};
        DefaultTableModel rollModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 2 || c == 3; // Status and Remarks editable
            }
        };
        rollModel.addColumn("__id"); // hidden column for reference
        JTable rollTable = buildRollTable(rollModel);

        JScrollPane rollScroll = new JScrollPane(rollTable);
        rollScroll.setBorder(new LineBorder(Theme.border(), 1));

        JPanel rollCard = buildCard("Class Roll — Mark Present / Absent");
        rollCard.add(rollScroll, BorderLayout.CENTER);

        // Action buttons below roll
        JButton markAllPresentBtn = buildBtn("Mark All Present",  Theme.EMERALD);
        JButton markAllAbsentBtn  = buildBtn("Mark All Absent",   Theme.RED);
        JButton saveSessionBtn    = buildBtn("Save Session",       Theme.TEACHER_TEAL);
        JButton clearRollBtn      = buildBtn("Clear Roll",         new Color(100, 110, 120));

        JPanel rollBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rollBtns.setBackground(Theme.card());
        rollBtns.add(markAllPresentBtn); rollBtns.add(markAllAbsentBtn);
        rollBtns.add(saveSessionBtn);    rollBtns.add(clearRollBtn);
        rollCard.add(rollBtns, BorderLayout.SOUTH);

        // ── Bottom: log of past attendance entries ────────────────────────────
        String[] logCols = {"Date", "Subject", "Section", "Student ID", "Student Name", "Status", "Remarks"};
        DefaultTableModel logModel = new DefaultTableModel(logCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable logTable = UIFactory.styledTable(logModel);
        logTable.getTableHeader().setBackground(Theme.darkMode
            ? new Color(20, 50, 40) : new Color(236, 253, 245));
        logTable.setDefaultRenderer(Object.class, logRowRenderer());
        refreshLogTable(teacher, logModel);

        JPanel logCard = buildCard("Attendance Log");
        JButton deleteLogBtn = buildBtn("Delete Selected", Theme.RED);
        JPanel logBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logBtnRow.setBackground(Theme.card());
        logBtnRow.add(deleteLogBtn);
        logCard.add(new JScrollPane(logTable), BorderLayout.CENTER);
        logCard.add(logBtnRow, BorderLayout.SOUTH);

        // ── Split: roll on top, log on bottom ─────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            rollCard, logCard);
        split.setResizeWeight(0.5);
        split.setDividerSize(6);

        // ── Wire actions ──────────────────────────────────────────────────────

        // Load students into roll
        loadBtn.addActionListener(e -> {
            String dateStr = dateField.getText().trim();
            String subj    = (String) subjectCombo.getSelectedItem();
            String section = (String) sectionCombo.getSelectedItem();
            if (subj == null) { UIFactory.toast(panel, "Select a subject first."); return; }
            try { LocalDate.parse(dateStr); } catch (Exception ex) {
                UIFactory.toast(panel, "Invalid date — use YYYY-MM-DD."); return;
            }
            rollModel.setRowCount(0);
            java.util.Set<String> addedIds = new java.util.LinkedHashSet<>();

            // If teacher typed specific IDs, use those
            String raw = studentIdsField.getText().trim();
            if (!raw.isEmpty() && !raw.startsWith("e.g.")) {
                for (String sid : raw.split("[,\\s]+")) {
                    sid = sid.trim(); if (sid.isEmpty()) continue;
                    addedIds.add(sid);
                }
            }

            // Always augment with students enrolled in this subject from DB,
            // scoped to the same institution as this teacher
            java.util.List<String> dbIds;
            if (teacher.collegeName != null
                    && !teacher.collegeName.equalsIgnoreCase("Unknown College")) {
                dbIds = DataManager.findStudentsBySubjectAndCollege(subj, teacher.collegeName)
                    .stream()
                    .map(s -> s.studentId)
                    .collect(java.util.stream.Collectors.toList());
            } else {
                dbIds = DataManager.findStudentIdsBySubject(subj);
            }
            addedIds.addAll(dbIds);

            if (addedIds.isEmpty()) {
                UIFactory.toast(panel, "No students found for subject '" + subj
                    + "'. Enter IDs manually or have students enrol.");
                return;
            }
            for (String sid : addedIds) {
                Student s = DataManager.loadStudentById(sid);
                String sname = s != null ? s.name : "(Unknown)";
                rollModel.addRow(new Object[]{sid, sname, "Present", ""});
            }
            UIFactory.toast(panel, "Loaded " + addedIds.size() + " student(s) for " + subj);
        });

        // Mark all present
        markAllPresentBtn.addActionListener(e -> {
            for (int r = 0; r < rollModel.getRowCount(); r++)
                rollModel.setValueAt("Present", r, 2);
        });

        // Mark all absent
        markAllAbsentBtn.addActionListener(e -> {
            for (int r = 0; r < rollModel.getRowCount(); r++)
                rollModel.setValueAt("Absent", r, 2);
        });

        // Save session — write to teacher log AND each student's own record
        saveSessionBtn.addActionListener(e -> {
            if (rollModel.getRowCount() == 0) {
                UIFactory.toast(panel, "Load students first."); return;
            }
            String dateStr = dateField.getText().trim();
            String subj    = (String) subjectCombo.getSelectedItem();
            String section = (String) sectionCombo.getSelectedItem();
            LocalDate date;
            try { date = LocalDate.parse(dateStr); }
            catch (Exception ex) { UIFactory.toast(panel, "Invalid date."); return; }

            if (teacher.attendanceLog == null) teacher.attendanceLog = new ArrayList<>();

            for (int r = 0; r < rollModel.getRowCount(); r++) {
                String sid     = (String) rollModel.getValueAt(r, 0);
                String sname   = (String) rollModel.getValueAt(r, 1);
                String status  = (String) rollModel.getValueAt(r, 2);
                String remarks = (String) rollModel.getValueAt(r, 3);
                boolean present = "Present".equals(status);

                // Build teacher log entry
                TeacherAttendanceRecord rec =
                    new TeacherAttendanceRecord(sid, sname, subj, section, date, present);
                rec.remarks = remarks;

                // Remove any existing entry for same student/subject/date (idempotent)
                teacher.attendanceLog.removeIf(x ->
                    x.studentId.equals(sid)
                    && x.subject.equals(subj)
                    && date.equals(x.date));
                teacher.attendanceLog.add(rec);

                // Mirror into student document (only if same institution)
                Student s = DataManager.loadStudentById(sid);
                if (s != null) {
                    if (teacher.collegeName != null
                            && !teacher.collegeName.equalsIgnoreCase("Unknown College")
                            && !teacher.collegeName.equalsIgnoreCase(s.collegeName)) {
                        continue; // skip students from other institutions
                    }
                    s.attendanceMap.computeIfAbsent(subj, k -> new ArrayList<>())
                        .removeIf(x -> x.date.equals(date));
                    s.attendanceMap.get(subj)
                        .add(new AttendanceRecord(subj, date, present));
                    DataManager.saveStudent(s);
                }
            }
            TeacherDataManager.saveTeacher(teacher);
            refreshLogTable(teacher, logModel);
            UIFactory.toast(panel, "Attendance saved for "
                + rollModel.getRowCount() + " student(s).");
            onDataChange.run();
        });

        clearRollBtn.addActionListener(e -> rollModel.setRowCount(0));

        // Delete from log
        deleteLogBtn.addActionListener(e -> {
            int row = logTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select a log entry."); return; }
            if (JOptionPane.showConfirmDialog(panel,
                    "Delete this attendance entry?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                String dateStr  = (String) logModel.getValueAt(row, 0);
                String subj     = (String) logModel.getValueAt(row, 1);
                String sid      = (String) logModel.getValueAt(row, 3);
                LocalDate date  = LocalDate.parse(dateStr);
                if (teacher.attendanceLog != null)
                    teacher.attendanceLog.removeIf(x ->
                        x.studentId.equals(sid)
                        && x.subject.equals(subj)
                        && date.equals(x.date));
                TeacherDataManager.saveTeacher(teacher);
                refreshLogTable(teacher, logModel);
                onDataChange.run();
            }
        });

        panel.add(sessionCard, BorderLayout.NORTH);
        panel.add(split,       BorderLayout.CENTER);
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

    private static void refreshLogTable(Teacher teacher, DefaultTableModel model) {
        model.setRowCount(0);
        if (teacher.attendanceLog == null) return;
        List<TeacherAttendanceRecord> sorted = new ArrayList<>(teacher.attendanceLog);
        sorted.sort((a, b) -> {
            if (a.date == null && b.date == null) return 0;
            if (a.date == null) return 1;
            if (b.date == null) return -1;
            return b.date.compareTo(a.date);
        });
        for (TeacherAttendanceRecord r : sorted)
            model.addRow(new Object[]{
                r.date != null ? r.date.toString() : "",
                r.subject, r.classSection, r.studentId,
                r.studentName, r.getStatusLabel(),
                r.remarks != null ? r.remarks : ""
            });
    }

    private static JTable buildRollTable(DefaultTableModel model) {
        JTable t = UIFactory.styledTable(model);
        t.setRowHeight(30);
        t.getTableHeader().setBackground(Theme.darkMode
            ? new Color(20, 50, 40) : new Color(236, 253, 245));

        // Status column → JComboBox
        JComboBox<String> statusEditor = new JComboBox<>(
            new String[]{"Present", "Absent"});
        UIFactory.styleCombo(statusEditor);
        t.getColumnModel().getColumn(2)
            .setCellEditor(new DefaultCellEditor(statusEditor));
        t.setDefaultRenderer(Object.class, rollRowRenderer());
        return t;
    }

    private static DefaultTableCellRenderer rollRowRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    Object status = t.getModel().getValueAt(r, 2);
                    boolean present = "Present".equals(status);
                    comp.setBackground(Theme.statusRowBg(present));
                    comp.setForeground(Theme.text());
                }
                return comp;
            }
        };
    }

    private static DefaultTableCellRenderer logRowRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    Object status = t.getModel().getValueAt(r, 5);
                    boolean present = "Present".equals(status);
                    comp.setBackground(Theme.statusRowBg(present));
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
