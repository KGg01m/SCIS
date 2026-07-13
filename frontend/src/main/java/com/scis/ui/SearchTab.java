package com.scis.ui;

import com.scis.model.AttendanceRecord;
import com.scis.model.MarksRecord;
import com.scis.model.Student;
import com.scis.ml.MLPredictor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SearchTab — live search and filter panel for attendance and marks records.
 */
public final class SearchTab {

    private SearchTab() {}

    public static JPanel build(Student student) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel hdr = new JLabel("Search & Filter Records");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hdr.setForeground(Theme.text());

        // ── Filter card ───────────────────────────────────────────────────────
        JPanel filterCard = UIFactory.buildCard("Filter & Sort");
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setBackground(Theme.card());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField searchField =
            UIFactory.styledTextField("Search subject, test type, status...");
        JComboBox<String> typeFilter =
            new JComboBox<>(new String[]{"All Records", "Attendance", "Marks"});
        JComboBox<String> subjectFilter = new JComboBox<>();
        subjectFilter.addItem("All Subjects");
        for (String s : student.getSubjects()) subjectFilter.addItem(s);
        JComboBox<String> statusFilter =
            new JComboBox<>(new String[]{"All Status", "Present", "Absent", "PASS", "FAIL"});
        JComboBox<String> sortBy =
            new JComboBox<>(new String[]{
                "Date (Newest)", "Date (Oldest)", "Subject A-Z", "Status"});
        JButton clearBtn = UIFactory.actionButton("Clear", new Color(100, 100, 100));

        UIFactory.styleCombo(typeFilter);
        UIFactory.styleCombo(subjectFilter);
        UIFactory.styleCombo(statusFilter);
        UIFactory.styleCombo(sortBy);

        gbc.gridx = 0; gbc.gridy = 0;
        filters.add(UIFactory.formLbl("Search:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; filters.add(searchField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        filters.add(UIFactory.formLbl("Record Type:"), gbc);
        gbc.gridx = 1; filters.add(typeFilter, gbc);
        gbc.gridx = 2; filters.add(UIFactory.formLbl("Subject:"), gbc);
        gbc.gridx = 3; filters.add(subjectFilter, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        filters.add(UIFactory.formLbl("Status:"), gbc);
        gbc.gridx = 1; filters.add(statusFilter, gbc);
        gbc.gridx = 2; filters.add(UIFactory.formLbl("Sort By:"), gbc);
        gbc.gridx = 3; filters.add(sortBy, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        JPanel cb = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cb.setBackground(Theme.card()); cb.add(clearBtn);
        filters.add(cb, gbc);
        filterCard.add(filters, BorderLayout.CENTER);

        // ── Results table ─────────────────────────────────────────────────────
        String[] cols = {"Type", "Subject", "Date / Test", "Value", "Status", "Notes"};
        DefaultTableModel resultsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable resultsTable = UIFactory.styledTable(resultsModel);
        resultsTable.setDefaultRenderer(Object.class, statusRowRenderer());

        JLabel summaryLbl = new JLabel("Showing all records");
        summaryLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        summaryLbl.setForeground(Theme.muted());

        // ── Filter logic ──────────────────────────────────────────────────────
        Runnable applyFilters = () -> {
            resultsModel.setRowCount(0);
            String query   = searchField.getText().trim().toLowerCase();
            String type    = (String) typeFilter.getSelectedItem();
            String subj    = (String) subjectFilter.getSelectedItem();
            String status  = (String) statusFilter.getSelectedItem();
            String sort    = (String) sortBy.getSelectedItem();
            List<Object[]> rows = new ArrayList<>();

            // Attendance rows
            if (!"Marks".equals(type)) {
                for (String s : student.getSubjects()) {
                    if (!"All Subjects".equals(subj) && !s.equals(subj)) continue;
                    List<AttendanceRecord> recs = student.attendanceMap.get(s);
                    if (recs == null) continue;
                    for (AttendanceRecord rec : recs) {
                        String st = rec.present ? "Present" : "Absent";
                        if (!"All Status".equals(status) && !st.equals(status)) continue;
                        String dateStr = rec.date.toString();
                        if (!query.isEmpty()
                                && !s.toLowerCase().contains(query)
                                && !st.toLowerCase().contains(query)
                                && !dateStr.contains(query)) continue;
                        rows.add(new Object[]{
                            "Attendance", s, dateStr, st, st,
                            rec.present ? "✓" : "✗"
                        });
                    }
                }
            }

            // Marks rows
            if (!"Attendance".equals(type)) {
                for (String s : student.getSubjects()) {
                    if (!"All Subjects".equals(subj) && !s.equals(subj)) continue;
                    List<MarksRecord> recs = student.marksMap.get(s);
                    if (recs == null) continue;
                    for (MarksRecord rec : recs) {
                        double pct = rec.marksObtained * 100.0 / rec.maxMarks;
                        String st  = pct >= 60 ? "PASS" : "FAIL";
                        if (!"All Status".equals(status) && !st.equals(status)) continue;
                        String val = String.format("%.1f/%.0f (%.0f%%)",
                            rec.marksObtained, rec.maxMarks, pct);
                        if (!query.isEmpty()
                                && !s.toLowerCase().contains(query)
                                && !rec.testType.toLowerCase().contains(query)
                                && !st.toLowerCase().contains(query)) continue;
                        rows.add(new Object[]{
                            "Marks", s, rec.testType, val, st,
                            MLPredictor.predictGradeCategory(
                                pct, student.getSubjectAttendance(s))
                        });
                    }
                }
            }

            // Sort
            switch (sort) {
                case "Date (Newest)":
                    rows.sort((a, b) -> ((String) b[2]).compareTo((String) a[2])); break;
                case "Date (Oldest)":
                    rows.sort((a, b) -> ((String) a[2]).compareTo((String) b[2])); break;
                case "Subject A-Z":
                    rows.sort((a, b) -> ((String) a[1]).compareTo((String) b[1])); break;
                default:
                    rows.sort((a, b) -> ((String) a[4]).compareTo((String) b[4]));
            }

            for (Object[] r : rows) resultsModel.addRow(r);
            summaryLbl.setText("Showing " + rows.size() + " record(s)");
        };

        // Live search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { applyFilters.run(); }
            public void removeUpdate(DocumentEvent e)  { applyFilters.run(); }
            public void changedUpdate(DocumentEvent e) { applyFilters.run(); }
        });
        typeFilter.addActionListener(e    -> applyFilters.run());
        subjectFilter.addActionListener(e -> applyFilters.run());
        statusFilter.addActionListener(e  -> applyFilters.run());
        sortBy.addActionListener(e        -> applyFilters.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            typeFilter.setSelectedIndex(0);
            subjectFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            sortBy.setSelectedIndex(0);
            applyFilters.run();
        });
        applyFilters.run();

        JPanel tableCard = UIFactory.buildTableCard(resultsTable, "Search Results");
        tableCard.add(summaryLbl, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(Theme.bg());
        top.add(hdr, BorderLayout.NORTH);
        top.add(filterCard, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    // ── Renderer ──────────────────────────────────────────────────────────────

    private static DefaultTableCellRenderer statusRowRenderer() {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String st = (String) t.getModel().getValueAt(r, 4);
                    if ("Present".equals(st) || "PASS".equals(st))
                        comp.setBackground(Theme.statusRowBg(true));
                    else if ("Absent".equals(st) || "FAIL".equals(st))
                        comp.setBackground(Theme.statusRowBg(false));
                    else
                        comp.setBackground(Theme.card());
                    comp.setForeground(Theme.text());
                }
                return comp;
            }
        };
    }
}
