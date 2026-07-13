package com.scis.ui;

import com.scis.db.DataManager;
import com.scis.model.AttendanceRecord;
import com.scis.model.MedicalLeave;
import com.scis.model.Student;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * MedicalLeaveTab — student portal tab for submitting and tracking
 * medical leave requests.
 *
 * <p>When a leave is approved (by a teacher), the attendance records
 * for the covered dates are automatically tagged with the leave ID and
 * excluded from the attendance percentage calculation.
 */
public final class MedicalLeaveTab {

    private MedicalLeaveTab() {}

    public static JPanel build(Student student, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel hdr = new JLabel("Medical Leave Requests");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hdr.setForeground(Theme.text());

        JLabel info = new JLabel(
            "<html><i>Approved leaves are excluded from your attendance percentage. "
            + "Pending/Rejected leaves count as absent.</i></html>");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(Theme.muted());

        // ── Form card ─────────────────────────────────────────────────────────
        JPanel formCard = buildCard("Submit New Leave Request");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.card());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6); gc.fill = GridBagConstraints.HORIZONTAL;

        // Subject combo
        List<String> subjectItems = new ArrayList<>(
            Arrays.asList(student.getSubjects()));
        subjectItems.add(0, "All Subjects");
        JComboBox<String> subjectCombo =
            new JComboBox<>(subjectItems.toArray(new String[0]));
        UIFactory.styleCombo(subjectCombo);

        JTextField startField = UIFactory.styledTextField(LocalDate.now().toString());
        JTextField endField   = UIFactory.styledTextField(LocalDate.now().toString());
        JTextArea  reasonArea = new JTextArea(2, 20);
        reasonArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reasonArea.setLineWrap(true); reasonArea.setWrapStyleWord(true);
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(203, 213, 225), 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JTextField docRefField = UIFactory.styledTextField(
            "e.g. Dr. Sharma, Apollo Hospital — 12/03/2025");

        // File upload components
        JButton uploadBtn = new JButton("📎 Upload Prescription");
        uploadBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        uploadBtn.setBackground(Theme.BLUE);
        uploadBtn.setForeground(Color.WHITE);
        uploadBtn.setFocusPainted(false);
        uploadBtn.setBorderPainted(false);
        uploadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel fileLabel = new JLabel("No file selected");
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fileLabel.setForeground(Theme.muted());
        
        // Store uploaded file data
        final byte[][] uploadedFileData = {null};
        final String[] uploadedFileName = {null};
        final String[] uploadedFileType = {null};

        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Prescription Document");
            // Filter for common document types
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Documents & Images", "pdf", "doc", "docx", "jpg", "jpeg", "png", "gif"));
            
            int result = fileChooser.showOpenDialog(panel);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File selectedFile = fileChooser.getSelectedFile();
                    byte[] fileData = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                    
                    uploadedFileData[0] = fileData;
                    uploadedFileName[0] = selectedFile.getName();
                    uploadedFileType[0] = java.nio.file.Files.probeContentType(selectedFile.toPath());
                    
                    fileLabel.setText("📎 " + selectedFile.getName());
                    fileLabel.setForeground(Theme.BLUE);
                    uploadBtn.setText("📎 Change File");
                    
                } catch (Exception ex) {
                    UIFactory.toast(panel, "Error reading file: " + ex.getMessage());
                }
            }
        });

        JButton submitBtn = UIFactory.actionButton("Submit Request", Theme.BLUE);
        JButton cancelBtn = UIFactory.actionButton("Cancel Selected", Theme.RED);

        // Layout
        gc.gridx=0; gc.gridy=0; form.add(UIFactory.formLbl("Subject:"), gc);
        gc.gridx=1; form.add(subjectCombo, gc);
        gc.gridx=2; form.add(UIFactory.formLbl("Start Date (YYYY-MM-DD):"), gc);
        gc.gridx=3; form.add(startField, gc);
        gc.gridx=0; gc.gridy=1; form.add(UIFactory.formLbl("End Date (YYYY-MM-DD):"), gc);
        gc.gridx=1; form.add(endField, gc);
        gc.gridx=2; form.add(UIFactory.formLbl("Document Reference:"), gc);
        gc.gridx=3; form.add(docRefField, gc);
        gc.gridx=0; gc.gridy=2; form.add(UIFactory.formLbl("Reason:"), gc);
        gc.gridx=1; gc.gridwidth=3;
        form.add(new JScrollPane(reasonArea), gc); gc.gridwidth=1;
        
        // File upload row
        gc.gridx=0; gc.gridy=3; form.add(UIFactory.formLbl("Prescription:"), gc);
        gc.gridx=1; gc.gridwidth=2; form.add(uploadBtn, gc); gc.gridwidth=1;
        gc.gridx=3; form.add(fileLabel, gc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Theme.card());
        btnRow.add(submitBtn); btnRow.add(cancelBtn);
        gc.gridx=0; gc.gridy=4; gc.gridwidth=4; form.add(btnRow, gc);
        formCard.add(form, BorderLayout.CENTER);

        // ── Leave history table ───────────────────────────────────────────────
        String[] cols = {"Subject","Start Date","End Date","Days","Reason","Document","Status","Reviewed By","Note"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIFactory.styledTable(model);
        table.setDefaultRenderer(Object.class, statusRenderer());
        refreshTable(student, model);
        
        // Add double-click listener to view/download attachments
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col == 5) { // Document column
                        List<MedicalLeave> leaves = student.getMedicalLeaves();
                        String subject = (String) model.getValueAt(row, 0);
                        String startDate = (String) model.getValueAt(row, 1);
                        
                        // Find the matching leave
                        for (MedicalLeave ml : leaves) {
                            if (ml.subject.equals(subject) && 
                                ml.startDate != null && 
                                ml.startDate.toString().equals(startDate)) {
                                
                                if (ml.hasAttachment()) {
                                    showAttachmentDialog(ml, panel);
                                } else if (ml.documentRef != null && !ml.documentRef.trim().isEmpty()) {
                                    JOptionPane.showMessageDialog(panel, 
                                        "Document Reference: " + ml.documentRef,
                                        "Document Information", 
                                        JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    UIFactory.toast(panel, "No document attached.");
                                }
                                break;
                            }
                        }
                    }
                }
            }
        });

        JPanel tableCard = buildCard("My Leave History");
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Attendance impact summary ─────────────────────────────────────────
        JPanel summaryCard = buildCard("Attendance Impact");
        JTextArea summaryArea = new JTextArea(buildSummary(student));
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        summaryArea.setEditable(false);
        summaryArea.setBackground(Theme.card()); summaryArea.setForeground(Theme.text());
        summaryArea.setMargin(new Insets(8, 8, 8, 8));
        summaryCard.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        summaryCard.setPreferredSize(new Dimension(0, 160));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            tableCard, summaryCard);
        split.setResizeWeight(0.65); split.setDividerSize(6);

        // ── Actions ───────────────────────────────────────────────────────────
        submitBtn.addActionListener(e -> {
            String subj = (String) subjectCombo.getSelectedItem();
            String reason = reasonArea.getText().trim();
            if (reason.isEmpty()) { UIFactory.toast(panel, "Enter a reason."); return; }
            LocalDate start, end;
            try {
                start = LocalDate.parse(startField.getText().trim());
                end   = LocalDate.parse(endField.getText().trim());
            } catch (Exception ex) {
                UIFactory.toast(panel, "Invalid date — use YYYY-MM-DD."); return;
            }
            if (end.isBefore(start)) {
                UIFactory.toast(panel, "End date cannot be before start date."); return;
            }
            MedicalLeave ml = new MedicalLeave(subj, start, end, reason);
            ml.documentRef = docRefField.getText().trim();
            
            // Handle file attachment if uploaded
            if (uploadedFileData[0] != null && uploadedFileName[0] != null) {
                ml.setAttachment(uploadedFileName[0], uploadedFileType[0], uploadedFileData[0]);
            }
            
            student.addMedicalLeave(ml);
            DataManager.saveStudent(student);
            refreshTable(student, model);
            summaryArea.setText(buildSummary(student));
            reasonArea.setText(""); docRefField.setText("");
            // Reset file upload
            uploadedFileData[0] = null; uploadedFileName[0] = null; uploadedFileType[0] = null;
            fileLabel.setText("No file selected"); fileLabel.setForeground(Theme.muted());
            uploadBtn.setText("📎 Upload Prescription");
            UIFactory.toast(panel, "Leave request submitted. Awaiting teacher approval.");
            onDataChange.run();
        });

        cancelBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select a leave to cancel."); return; }
            String status = (String) model.getValueAt(row, 6);
            if ("Approved".equals(status)) {
                UIFactory.toast(panel, "Cannot cancel an already approved leave."); return;
            }
            if (JOptionPane.showConfirmDialog(panel,
                    "Cancel this leave request?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                String subjFilter = (String) model.getValueAt(row, 0);
                String startStr   = (String) model.getValueAt(row, 1);
                student.getMedicalLeaves().removeIf(ml ->
                    ml.subject.equals(subjFilter)
                    && ml.startDate != null
                    && ml.startDate.toString().equals(startStr)
                    && !ml.isApproved());
                DataManager.saveStudent(student);
                refreshTable(student, model);
                summaryArea.setText(buildSummary(student));
                onDataChange.run();
            }
        });

        JPanel topBar = new JPanel(new BorderLayout(0, 4));
        topBar.setBackground(Theme.bg());
        topBar.add(hdr,      BorderLayout.NORTH);
        topBar.add(info,     BorderLayout.CENTER);

        JPanel topSection = new JPanel(new BorderLayout(0, 10));
        topSection.setBackground(Theme.bg());
        topSection.add(topBar,   BorderLayout.NORTH);
        topSection.add(formCard, BorderLayout.CENTER);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(split,      BorderLayout.CENTER);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void refreshTable(Student student, DefaultTableModel model) {
        model.setRowCount(0);
        List<MedicalLeave> leaves = student.getMedicalLeaves();
        // newest first
        List<MedicalLeave> sorted = new ArrayList<>(leaves);
        sorted.sort((a, b) -> {
            if (a.submittedDate == null) return 1;
            if (b.submittedDate == null) return -1;
            return b.submittedDate.compareTo(a.submittedDate);
        });
        for (MedicalLeave ml : sorted)
            model.addRow(new Object[]{
                ml.subject,
                ml.startDate  != null ? ml.startDate.toString()  : "",
                ml.endDate    != null ? ml.endDate.toString()    : "",
                ml.getDurationDays(),
                ml.reason    != null ? ml.reason    : "",
                ml.getAttachmentDisplay(),
                ml.getStatusLabel(),
                ml.reviewedBy  != null ? ml.reviewedBy  : "",
                ml.rejectionNote != null ? ml.rejectionNote : ""
            });
    }

    private static String buildSummary(Student student) {
        StringBuilder sb = new StringBuilder("ATTENDANCE IMPACT OF MEDICAL LEAVES\n");
        sb.append("=".repeat(50)).append("\n");
        for (String subj : student.getSubjects()) {
            List<AttendanceRecord> recs = student.attendanceMap.get(subj);
            if (recs == null) continue;
            int total = 0, present = 0, mlExcluded = 0;
            for (AttendanceRecord r : recs) {
                if (r.isMedicalLeave()) { mlExcluded++; continue; }
                total++; if (r.present) present++;
            }
            double pct = total > 0 ? present * 100.0 / total : 0;
            int totalRaw = recs.size();
            double rawPct = totalRaw > 0
                ? (int)(recs.stream().filter(r -> r.present).count()) * 100.0 / totalRaw : 0;
            sb.append(String.format("%-25s | Raw: %5.1f%% | After ML exclusion: %5.1f%% | ML days excl: %d\n",
                subj, rawPct, pct, mlExcluded));
        }
        int totalApproved = student.getApprovedMedicalLeaveCount(null);
        int totalPending  = (int) student.getMedicalLeaves().stream()
            .filter(MedicalLeave::isPending).count();
        sb.append(String.format("\nTotal approved leaves: %d  |  Pending: %d\n",
            totalApproved, totalPending));
        return sb.toString();
    }

    private static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) {
                    String st = (String) t.getModel().getValueAt(r, 6);
                    if ("Approved".equals(st))
                        comp.setBackground(Theme.statusRowBg(true));
                    else if ("Rejected".equals(st))
                        comp.setBackground(Theme.statusRowBg(false));
                    else
                        comp.setBackground(Theme.darkMode
                            ? new Color(50, 45, 10) : new Color(255, 251, 235));
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

    /** Show dialog to view/download attachment. */
    private static void showAttachmentDialog(MedicalLeave ml, JPanel parentPanel) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parentPanel), 
            "Prescription Document", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(parentPanel);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Theme.card());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header info
        JLabel infoLabel = new JLabel("<html><b>File:</b> " + ml.attachmentName + 
            "<br><b>Type:</b> " + ml.attachmentType + "</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setForeground(Theme.text());
        
        // Action buttons
        JButton downloadBtn = new JButton("💾 Download");
        downloadBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        downloadBtn.setBackground(Theme.BLUE);
        downloadBtn.setForeground(Color.WHITE);
        downloadBtn.setFocusPainted(false);
        downloadBtn.setBorderPainted(false);
        downloadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        downloadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Prescription");
            fileChooser.setSelectedFile(new java.io.File(ml.attachmentName));
            
            int result = fileChooser.showSaveDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File saveFile = fileChooser.getSelectedFile();
                    java.nio.file.Files.write(saveFile.toPath(), ml.getAttachmentData());
                    UIFactory.toast(parentPanel, "File saved successfully!");
                } catch (Exception ex) {
                    UIFactory.toast(parentPanel, "Error saving file: " + ex.getMessage());
                }
            }
        });
        
        closeBtn.addActionListener(e -> dialog.dispose());
        
        // Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Theme.card());
        buttonPanel.add(downloadBtn);
        buttonPanel.add(closeBtn);
        
        mainPanel.add(infoLabel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
}
