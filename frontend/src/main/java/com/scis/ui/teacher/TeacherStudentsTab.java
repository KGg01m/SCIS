package com.scis.ui.teacher;

import com.scis.ui.Theme;

import com.scis.db.DataManager;
import com.scis.model.MedicalLeave;
import com.scis.model.Student;
import com.scis.teacher.model.Teacher;
import com.scis.teacher.model.TeacherAttendanceRecord;
import com.scis.teacher.model.TeacherMarksRecord;
import com.scis.ui.UIFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * TeacherStudentsTab — teacher views all students they have interacted with,
 * with their subject-wise attendance and marks stats.
 * Clicking a student shows a detailed profile panel.
 */
public final class TeacherStudentsTab {

    private TeacherStudentsTab() {}

    public static JPanel build(Teacher teacher) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel hdr = new JLabel("Students in My Classes");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hdr.setForeground(Theme.text());

        // College scope badge
        String collegeBadgeText = (teacher.collegeName != null
                && !teacher.collegeName.equalsIgnoreCase("Unknown College"))
            ? "🏫  Showing students from: " + teacher.collegeName
            : "🏫  Showing all students (no institution filter)";
        JLabel collegeBadge = new JLabel(collegeBadgeText);
        collegeBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        collegeBadge.setForeground(Theme.muted());

        JPanel hdrPanel = new JPanel(new BorderLayout(0, 2));
        hdrPanel.setBackground(Theme.bg());
        hdrPanel.add(hdr, BorderLayout.NORTH);
        hdrPanel.add(collegeBadge, BorderLayout.SOUTH);

        // Filter controls
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setBackground(Theme.bg());
        JComboBox<String> subjectFilter = new JComboBox<>();
        subjectFilter.addItem("All Subjects");
        for (String s : teacher.getSubjects()) subjectFilter.addItem(s);
        styleCombo(subjectFilter);
        JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"All", "At Risk (Att < 75%)", "Low Marks (< 60%)", "Good Standing"});
        styleCombo(statusFilter);
        JTextField searchField = UIFactory.styledTextField("Search by name or ID...");
        searchField.setPreferredSize(new Dimension(220, 34));

        filterRow.add(new JLabel("Subject:"));   filterRow.add(subjectFilter);
        filterRow.add(new JLabel("Status:"));    filterRow.add(statusFilter);
        filterRow.add(new JLabel("Search:"));    filterRow.add(searchField);

        // Student list table
        String[] cols = {"Student ID","Name","Subject","Attendance %",
                          "Avg Marks %","Status","Risk Level"};
        DefaultTableModel studentModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable studentTable = UIFactory.styledTable(studentModel);
        studentTable.getTableHeader().setBackground(Theme.darkMode
            ? new Color(20, 50, 40) : new Color(236, 253, 245));
        studentTable.setDefaultRenderer(Object.class, riskRenderer());
        refreshStudentTable(teacher, studentModel, null, null, "");

        // Detail panel on right
        JPanel detailCard = buildCard("Student Detail");
        JTextArea detailArea = new JTextArea("Click a student row to see details.");
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        detailArea.setEditable(false);
        detailArea.setBackground(Theme.card());
        detailArea.setForeground(Theme.text());
        detailArea.setMargin(new Insets(8, 8, 8, 8));
        detailCard.add(new JScrollPane(detailArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildCard("Student List", new JScrollPane(studentTable)),
            detailCard);
        split.setResizeWeight(0.6);
        split.setDividerSize(6);

        // Wire filters
        Runnable applyFilters = () -> {
            String subj   = (String) subjectFilter.getSelectedItem();
            String status = (String) statusFilter.getSelectedItem();
            String search = searchField.getText().trim().toLowerCase();
            refreshStudentTable(teacher, studentModel,
                "All Subjects".equals(subj) ? null : subj,
                "All".equals(status) ? null : status, search);
        };
        subjectFilter.addActionListener(e  -> applyFilters.run());
        statusFilter.addActionListener(e   -> applyFilters.run());
        searchField.getDocument().addDocumentListener(
            new com.scis.ui.DocumentAdapter() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters.run(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters.run(); }
            });

        // Wire row click → detail panel
        studentTable.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = studentTable.getSelectedRow();
            if (row < 0) return;
            String sid  = (String) studentModel.getValueAt(row, 0);
            String subj = (String) studentModel.getValueAt(row, 2);
            detailArea.setText(buildStudentDetail(teacher, sid, subj));
            detailArea.setCaretPosition(0);
        });

        JPanel topBar = new JPanel(new BorderLayout(0, 6));
        topBar.setBackground(Theme.bg());
        topBar.add(hdrPanel,   BorderLayout.NORTH);
        topBar.add(filterRow, BorderLayout.CENTER);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(split,  BorderLayout.CENTER);
        return panel;
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    private static void refreshStudentTable(Teacher teacher,
                                             DefaultTableModel model,
                                             String subjectFilter,
                                             String statusFilter,
                                             String search) {
        model.setRowCount(0);

        // --- Source 1: students from teacher's own attendance/marks log ---
        Map<String, Set<String>> studentSubjects = new LinkedHashMap<>();
        if (teacher.attendanceLog != null)
            for (TeacherAttendanceRecord r : teacher.attendanceLog) {
                if (subjectFilter != null && !subjectFilter.equals(r.subject)) continue;
                studentSubjects.computeIfAbsent(r.studentId, k -> new LinkedHashSet<>())
                    .add(r.subject);
            }
        if (teacher.marksLog != null)
            for (TeacherMarksRecord r : teacher.marksLog) {
                if (subjectFilter != null && !subjectFilter.equals(r.subject)) continue;
                studentSubjects.computeIfAbsent(r.studentId, k -> new LinkedHashSet<>())
                    .add(r.subject);
            }

        // --- Source 2: students enrolled in teacher's subjects via DB (scoped to same college) ---
        for (String subj : teacher.getSubjects()) {
            if (subjectFilter != null && !subjectFilter.equals(subj)) continue;
            List<Student> enrolled = (teacher.collegeName != null
                    && !teacher.collegeName.equalsIgnoreCase("Unknown College"))
                ? DataManager.findStudentsBySubjectAndCollege(subj, teacher.collegeName)
                : DataManager.findAllStudentsBySubject(subj);
            for (Student s : enrolled)
                studentSubjects.computeIfAbsent(s.studentId, k -> new LinkedHashSet<>())
                    .add(subj);
        }

        for (Map.Entry<String, Set<String>> entry : studentSubjects.entrySet()) {
            String sid = entry.getKey();
            Student s  = DataManager.loadStudentById(sid);

            // Only show students from the same college as the teacher
            if (s != null && teacher.collegeName != null
                    && !teacher.collegeName.equalsIgnoreCase("Unknown College")
                    && !teacher.collegeName.equalsIgnoreCase(s.collegeName)) continue;

            String sname = s != null ? s.name : "(Unknown)";

            if (!search.isEmpty()
                    && !sid.toLowerCase().contains(search)
                    && !sname.toLowerCase().contains(search)) continue;

            for (String subj : entry.getValue()) {
                // Use teacher-recorded data if available, else student's own data
                double attPct  = computeTeacherAtt(teacher, sid, subj);
                if (attPct == 0 && s != null) attPct = s.getSubjectAttendance(subj);
                double marksPct= computeTeacherMarks(teacher, sid, subj);
                if (marksPct < 0 && s != null) marksPct = s.getSubjectPerformance(subj);

                String status  = deriveStatus(attPct, marksPct);
                String risk    = attPct < 65 || (marksPct >= 0 && marksPct < 40) ? "HIGH"
                    : attPct < 75 || (marksPct >= 0 && marksPct < 60) ? "MEDIUM" : "LOW";

                if (statusFilter != null) {
                    if (statusFilter.contains("At Risk") && attPct >= 75) continue;
                    if (statusFilter.contains("Low Marks") && (marksPct < 0 || marksPct >= 60)) continue;
                    if (statusFilter.contains("Good Standing")
                            && (attPct < 75 || (marksPct >= 0 && marksPct < 60))) continue;
                }

                // Medical leave count for this student
                int mlCount = s != null ? s.getApprovedMedicalLeaveCount(subj) : 0;
                String mlNote = mlCount > 0 ? " [ML:" + mlCount + "]" : "";

                model.addRow(new Object[]{
                    sid, sname, subj,
                    String.format("%.1f%%%s", attPct, mlNote),
                    marksPct >= 0 ? String.format("%.1f%%", marksPct) : "N/A",
                    status, risk
                });
            }
        }
    }

    private static double computeTeacherAtt(Teacher teacher,
                                              String sid, String subj) {
        if (teacher.attendanceLog == null) return 0;
        int total = 0, present = 0;
        for (TeacherAttendanceRecord r : teacher.attendanceLog)
            if (r.studentId.equals(sid) && r.subject.equals(subj)) {
                total++;
                if (r.present) present++;
            }
        return total > 0 ? present * 100.0 / total : 0;
    }

    private static double computeTeacherMarks(Teacher teacher,
                                               String sid, String subj) {
        if (teacher.marksLog == null) return -1;
        double sum = 0; int count = 0;
        for (TeacherMarksRecord r : teacher.marksLog)
            if (r.studentId.equals(sid) && r.subject.equals(subj)) {
                sum += r.getPercentage(); count++;
            }
        return count > 0 ? sum / count : -1;
    }

    private static String deriveStatus(double att, double marks) {
        if (att < 65 || (marks >= 0 && marks < 40)) return "Critical";
        if (att < 75 || (marks >= 0 && marks < 60)) return "At Risk";
        return "Good";
    }

    private static String buildStudentDetail(Teacher teacher,
                                              String sid, String subj) {
        StringBuilder sb = new StringBuilder();
        Student s = DataManager.loadStudentById(sid);
        sb.append("=== STUDENT PROFILE ===\n");
        if (s != null) {
            sb.append("Name:       ").append(s.name).append("\n");
            sb.append("ID:         ").append(s.studentId).append("\n");
            sb.append("Email:      ").append(s.email).append("\n");
            sb.append("Department: ").append(s.department).append("\n");
            sb.append("Semester:   ").append(s.semester).append("\n");
        } else {
            sb.append("Student ID: ").append(sid).append(" (not found in DB)\n");
        }

        sb.append("\n=== ATTENDANCE (").append(subj).append(") ===\n");
        if (teacher.attendanceLog != null) {
            int total = 0, present = 0;
            for (TeacherAttendanceRecord r : teacher.attendanceLog)
                if (r.studentId.equals(sid) && r.subject.equals(subj)) {
                    total++;
                    if (r.present) present++;
                    sb.append(r.date).append(" — ")
                        .append(r.getStatusLabel())
                        .append(r.remarks != null && !r.remarks.isEmpty()
                            ? " [" + r.remarks + "]" : "")
                        .append("\n");
                }
            if (total > 0)
                sb.append(String.format("Total: %d/%d (%.1f%%)\n",
                    present, total, present * 100.0 / total));
        }

        sb.append("\n=== MARKS (").append(subj).append(") ===\n");
        if (teacher.marksLog != null)
            for (TeacherMarksRecord r : teacher.marksLog)
                if (r.studentId.equals(sid) && r.subject.equals(subj))
                    sb.append(String.format("%-12s : %.1f / %.0f  (%s) %s\n",
                        r.examType, r.marksObtained, r.maxMarks,
                        r.getGrade(),
                        r.remarks != null ? "[" + r.remarks + "]" : ""));
        return sb.toString();
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private static DefaultTableCellRenderer riskRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String risk = (String) t.getModel().getValueAt(r, 6);
                    if ("HIGH".equals(risk))
                        comp.setBackground(Theme.darkMode
                            ? new Color(60, 10, 10) : new Color(255, 241, 242));
                    else if ("MEDIUM".equals(risk))
                        comp.setBackground(Theme.darkMode
                            ? new Color(55, 40, 5) : new Color(255, 251, 235));
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

    private static void styleCombo(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setBackground(Theme.card());
        cb.setForeground(Theme.text());
        cb.setPreferredSize(new Dimension(180, 34));
    }
}
