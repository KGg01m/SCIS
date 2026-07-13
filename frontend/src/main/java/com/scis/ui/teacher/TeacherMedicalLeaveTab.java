package com.scis.ui.teacher;

import com.scis.ui.Theme;

import com.scis.db.DataManager;
import com.scis.model.AttendanceRecord;
import com.scis.model.MedicalLeave;
import com.scis.model.Student;
import com.scis.teacher.model.Teacher;
import com.scis.ui.UIFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * TeacherMedicalLeaveTab — teacher reviews all medical leave requests
 * for students in their subjects, then approves or rejects them.
 *
 * When approved:
 *  - All AttendanceRecord entries within the leave date range for that
 *    subject are tagged with the leave ID → excluded from % calculation.
 *  - The student document is saved with the updated records.
 */
public final class TeacherMedicalLeaveTab {

    private TeacherMedicalLeaveTab() {}

    public static JPanel build(Teacher teacher, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel hdr = new JLabel("Medical Leave Requests — Review & Approve");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hdr.setForeground(Theme.text());

        JLabel info = new JLabel(
            "<html><i>Approving a request tags all absence records within that date "
            + "range as medical leave, excluding them from attendance %. "
            + "Rejecting marks the request as rejected with an optional note.</i></html>");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(Theme.muted());

        // ── Filter controls ───────────────────────────────────────────────────
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setBackground(Theme.bg());
        JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"Pending", "All", "Approved", "Rejected"});
        JComboBox<String> subjectFilter = new JComboBox<>();
        subjectFilter.addItem("All Subjects");
        for (String s : teacher.getSubjects()) subjectFilter.addItem(s);
        styleCombo(statusFilter); styleCombo(subjectFilter);
        filterRow.add(new JLabel("Status:")); filterRow.add(statusFilter);
        filterRow.add(new JLabel("Subject:")); filterRow.add(subjectFilter);
        JButton refreshBtn = buildBtn("Refresh", Theme.TEACHER_TEAL);
        filterRow.add(refreshBtn);

        // ── Requests table ────────────────────────────────────────────────────
        String[] cols = {"Student ID","Student Name","Subject","Start","End",
                          "Days","Reason","Document","Status","Submitted"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIFactory.styledTable(model);
        table.getTableHeader().setBackground(Theme.surface());
        table.setDefaultRenderer(Object.class, statusRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Detail panel
        JTextArea detailArea = new JTextArea("Select a request to see full details.");
        detailArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailArea.setEditable(false); detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setBackground(Theme.card());
        detailArea.setForeground(Theme.text());
        detailArea.setMargin(new Insets(8, 8, 8, 8));

        JPanel detailCard = buildCard("Request Detail");
        detailCard.add(new JScrollPane(detailArea), BorderLayout.CENTER);

        // Action controls
        JTextField rejectNoteField = UIFactory.styledTextField("Rejection reason (optional)...");
        JButton approveBtn = buildBtn("Approve", Theme.EMERALD);
        JButton rejectBtn  = buildBtn("Reject",  Theme.RED);
        JButton viewDocBtn = buildBtn("See Document", Theme.TEACHER_TEAL);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setBackground(Theme.card());
        actionRow.add(approveBtn); actionRow.add(rejectBtn); actionRow.add(viewDocBtn);
        actionRow.add(new JLabel("Reject note:"));
        rejectNoteField.setPreferredSize(new Dimension(260, 32));
        actionRow.add(rejectNoteField);
        detailCard.add(actionRow, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildCard("Leave Requests", new JScrollPane(table)),
            detailCard);
        split.setResizeWeight(0.6); split.setDividerSize(6);

        // ── Load data ─────────────────────────────────────────────────────────
        // We need to scan students that belong to teacher's subjects
        final List<StudentLeaveEntry> entries = new ArrayList<>();

        Runnable loadEntries = () -> {
            entries.clear();
            model.setRowCount(0);
            String subjFilter  = (String) subjectFilter.getSelectedItem();
            String stFilter    = (String) statusFilter.getSelectedItem();
            Set<String> mySubjects = new LinkedHashSet<>(
                Arrays.asList(teacher.getSubjects()));

            // Fetch students for each subject teacher teaches, scoped to same institution
            Set<String> processedIds = new HashSet<>();
            for (String subj : mySubjects) {
                if (!"All Subjects".equals(subjFilter) && !subj.equals(subjFilter)) continue;
                List<Student> students = (teacher.collegeName != null
                        && !teacher.collegeName.equalsIgnoreCase("Unknown College"))
                    ? DataManager.findStudentsBySubjectAndCollege(subj, teacher.collegeName)
                    : DataManager.findAllStudentsBySubject(subj);
                for (Student s : students) {
                    if (processedIds.contains(s.studentId)) continue;
                    processedIds.add(s.studentId);
                    for (MedicalLeave ml : s.getMedicalLeaves()) {
                        if (!"All Subjects".equals(subjFilter)
                                && !subj.equals(ml.subject)
                                && !"All Subjects".equals(ml.subject)) continue;
                        if (!"All".equals(stFilter)
                                && !ml.getStatusLabel().equals(stFilter)) continue;
                        entries.add(new StudentLeaveEntry(s, ml));
                        model.addRow(new Object[]{
                            s.studentId, s.name, ml.subject,
                            ml.startDate != null ? ml.startDate.toString() : "",
                            ml.endDate   != null ? ml.endDate.toString()   : "",
                            ml.getDurationDays(),
                            ml.reason    != null ? ml.reason    : "",
                            ml.getAttachmentDisplay(),
                            ml.getStatusLabel(),
                            ml.submittedDate != null ? ml.submittedDate.toString() : ""
                        });
                    }
                }
            }
        };
        loadEntries.run();

        // Wire selection → detail panel
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0 || row >= entries.size()) {
                detailArea.setText("Select a request to see full details.");
                viewDocBtn.setText("See Document");
                return;
            }
            StudentLeaveEntry entry = entries.get(row);
            detailArea.setText(buildDetail(entry.student, entry.leave));
            
            // Always show "See Document" - the action will handle different types
            viewDocBtn.setText("See Document");
        });

        refreshBtn.addActionListener(e -> loadEntries.run());
        subjectFilter.addActionListener(e -> loadEntries.run());
        statusFilter.addActionListener(e -> loadEntries.run());

        // Approve
        approveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0 || row >= entries.size()) {
                UIFactory.toast(panel, "Select a leave request."); return;
            }
            StudentLeaveEntry entry = entries.get(row);
            if (entry.leave.isApproved()) { UIFactory.toast(panel, "Already approved."); return; }

            if (JOptionPane.showConfirmDialog(panel,
                    "Approve leave for " + entry.student.name
                    + "\n" + entry.leave.startDate + " → " + entry.leave.endDate
                    + "\nSubject: " + entry.leave.subject,
                    "Confirm Approval", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;

            entry.leave.approve(teacher.teacherId);
            // Tag attendance records in the date range
            tagAttendanceRecords(entry.student, entry.leave);
            DataManager.saveStudent(entry.student);
            loadEntries.run();
            UIFactory.toast(panel, "Leave approved. Attendance updated.");
            onDataChange.run();
        });

        // Reject
        rejectBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0 || row >= entries.size()) {
                UIFactory.toast(panel, "Select a leave request."); return;
            }
            StudentLeaveEntry entry = entries.get(row);
            if (entry.leave.isApproved()) { UIFactory.toast(panel, "Cannot reject an approved leave."); return; }

            String note = rejectNoteField.getText().trim();
            entry.leave.reject(teacher.teacherId, note);
            // Clear any previously tagged attendance records
            clearAttendanceTags(entry.student, entry.leave);
            DataManager.saveStudent(entry.student);
            loadEntries.run();
            UIFactory.toast(panel, "Leave rejected.");
            onDataChange.run();
        });

        // View Document
        viewDocBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0 || row >= entries.size()) {
                UIFactory.toast(panel, "Select a leave request."); return;
            }
            StudentLeaveEntry entry = entries.get(row);
            MedicalLeave leave = entry.leave;
            
            // Check if there's an actual file attachment
            if (leave.hasAttachment()) {
                try {
                    // Create a temporary file
                    String fileName = leave.attachmentName != null ? leave.attachmentName : "medical_document";
                    String extension = getFileExtension(leave.attachmentType, fileName);
                    File tempFile = File.createTempFile("medical_leave_", "." + extension);
                    tempFile.deleteOnExit();
                    
                    // Write the decoded file data
                    byte[] fileData = leave.getAttachmentData();
                    if (fileData != null) {
                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                            fos.write(fileData);
                        }
                        
                        // Open the file with the default system application
                        if (Desktop.isDesktopSupported()) {
                            Desktop desktop = Desktop.getDesktop();
                            if (desktop.isSupported(Desktop.Action.OPEN)) {
                                desktop.open(tempFile);
                                UIFactory.toast(panel, "Opening document: " + fileName);
                            } else {
                                UIFactory.toast(panel, "System doesn't support file opening. File saved to: " + tempFile.getAbsolutePath());
                            }
                        } else {
                            UIFactory.toast(panel, "Desktop not supported. File saved to: " + tempFile.getAbsolutePath());
                        }
                    } else {
                        UIFactory.toast(panel, "Failed to read document data.");
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(panel, 
                        "Error opening document: " + ex.getMessage(), 
                        "Document Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } 
            // Check if there's a document reference (text-only)
            else if (leave.documentRef != null && !leave.documentRef.trim().isEmpty()) {
                JOptionPane.showMessageDialog(panel, 
                    "Document Reference Information:\n\n" + leave.documentRef,
                    "Document Reference", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
            // No document at all
            else {
                UIFactory.toast(panel, "No document attached to this leave request.");
            }
        });

        JPanel topBar = new JPanel(new BorderLayout(0, 4));
        topBar.setBackground(Theme.bg());
        topBar.add(hdr, BorderLayout.NORTH);
        topBar.add(info, BorderLayout.CENTER);
        topBar.add(filterRow, BorderLayout.SOUTH);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(split,  BorderLayout.CENTER);
        return panel;
    }

    // ── Business logic ────────────────────────────────────────────────────────

    /**
     * Tags all absence records within the leave date range with the leave ID,
     * so they are excluded from attendance percentage calculation.
     */
    private static void tagAttendanceRecords(Student student, MedicalLeave leave) {
        if (leave.startDate == null || leave.endDate == null) return;
        Set<String> subjects = new LinkedHashSet<>();
        if ("All Subjects".equals(leave.subject)) {
            subjects.addAll(student.attendanceMap.keySet());
        } else {
            subjects.add(leave.subject);
        }
        for (String subj : subjects) {
            List<AttendanceRecord> recs = student.attendanceMap.get(subj);
            if (recs == null) continue;
            for (AttendanceRecord rec : recs) {
                if (!rec.present && leave.covers(rec.date)) {
                    rec.medicalLeaveId = leave.id;
                }
            }
        }
    }

    /** Removes leave tags from attendance records when a leave is rejected. */
    private static void clearAttendanceTags(Student student, MedicalLeave leave) {
        for (List<AttendanceRecord> recs : student.attendanceMap.values())
            for (AttendanceRecord rec : recs)
                if (leave.id.equals(rec.medicalLeaveId))
                    rec.medicalLeaveId = null;
    }

    // ── Detail text ───────────────────────────────────────────────────────────

    private static String buildDetail(Student student, MedicalLeave ml) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MEDICAL LEAVE REQUEST ===\n\n");
        sb.append("Student:      ").append(student.name)
          .append(" (").append(student.studentId).append(")\n");
        sb.append("Email:        ").append(student.email).append("\n");
        sb.append("Subject:      ").append(ml.subject).append("\n");
        sb.append("Period:       ").append(ml.startDate)
          .append(" → ").append(ml.endDate)
          .append(" (").append(ml.getDurationDays()).append(" day(s))\n");
        sb.append("Reason:\n  ").append(ml.reason).append("\n\n");
        
        // Enhanced attachment information
        if (ml.hasAttachment()) {
            sb.append("📎 ATTACHED DOCUMENT:\n");
            sb.append("  File Name: ").append(ml.attachmentName != null ? ml.attachmentName : "Unknown").append("\n");
            sb.append("  File Type: ").append(ml.attachmentType != null ? ml.attachmentType : "Unknown").append("\n");
            sb.append("  Size: ").append(formatFileSize(ml.getAttachmentData() != null ? ml.getAttachmentData().length : 0)).append("\n");
            sb.append("  Click 'View Document' to open this file.\n");
        } else if (ml.documentRef != null && !ml.documentRef.trim().isEmpty()) {
            sb.append("📝 DOCUMENT REFERENCE:\n");
            sb.append("  ").append(ml.documentRef).append("\n");
        } else {
            sb.append("📄 NO DOCUMENT ATTACHED\n");
        }
        sb.append("\n");
        
        sb.append("Submitted:    ").append(ml.submittedDate).append("\n");
        sb.append("Status:       ").append(ml.getStatusLabel()).append("\n");
        if (ml.reviewedBy != null)
            sb.append("Reviewed By:  ").append(ml.reviewedBy)
              .append(" on ").append(ml.reviewedDate).append("\n");
        if (ml.rejectionNote != null)
            sb.append("Reject Note:  ").append(ml.rejectionNote).append("\n");

        // Show current attendance for reference
        sb.append("\nCurrent attendance in ").append(ml.subject).append(":\n");
        double att = student.getSubjectAttendance(ml.subject);
        sb.append(String.format("  %.1f%% (after any approved leaves)\n", att));
        return sb.toString();
    }

    /**
     * Formats file size in human-readable format.
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ── Inner type ────────────────────────────────────────────────────────────

    private static class StudentLeaveEntry {
        final Student     student;
        final MedicalLeave leave;
        StudentLeaveEntry(Student s, MedicalLeave ml) { student = s; leave = ml; }
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) {
                    String st = (String) t.getModel().getValueAt(r, 8);
                    if ("Approved".equals(st))
                        comp.setBackground(Theme.statusRowBg(true));
                    else if ("Rejected".equals(st))
                        comp.setBackground(Theme.statusRowBg(false));
                    else
                        comp.setBackground(Theme.card());
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
        b.setForeground(Theme.text()); b.setBackground(bg);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void styleCombo(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setBackground(Theme.card());
        cb.setForeground(Theme.text());
        cb.setPreferredSize(new Dimension(160, 34));
    }

    /**
     * Determines file extension from MIME type or filename.
     * Returns a default extension if unable to determine.
     */
    private static String getFileExtension(String mimeType, String fileName) {
        // First try to get from filename
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        
        // Then try to determine from MIME type
        if (mimeType != null) {
            switch (mimeType.toLowerCase()) {
                case "application/pdf":
                    return "pdf";
                case "image/jpeg":
                case "image/jpg":
                    return "jpg";
                case "image/png":
                    return "png";
                case "image/gif":
                    return "gif";
                case "text/plain":
                    return "txt";
                case "application/msword":
                    return "doc";
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    return "docx";
                case "application/vnd.ms-excel":
                    return "xls";
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                    return "xlsx";
                default:
                    // Extract from MIME type (e.g., "image/png" -> "png")
                    int slashIndex = mimeType.lastIndexOf('/');
                    if (slashIndex >= 0 && slashIndex < mimeType.length() - 1) {
                        return mimeType.substring(slashIndex + 1);
                    }
            }
        }
        
        // Default fallback
        return "pdf";
    }
}
