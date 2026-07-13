package com.scis.ui;

import com.scis.db.DataManager;
import com.scis.model.AttendanceRecord;
import com.scis.model.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * AttendanceTab — builds the Attendance management panel.
 */
public final class AttendanceTab {

    private AttendanceTab() {}

    public static JPanel build(Student student, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Form card ─────────────────────────────────────────────────────────
        JPanel formCard = UIFactory.buildCard("Mark Attendance");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.card());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> subjectCombo =
            SubjectComboHelper.create(panel, student, () -> onDataChange.run());
        JComboBox<String> statusCombo =
            new JComboBox<>(new String[]{"Present", "Absent"});
        UIFactory.styleCombo(statusCombo);

        JButton saveBtn     = UIFactory.actionButton("Save",      Theme.GREEN);
        JButton editBtn     = UIFactory.actionButton("Edit",      Theme.ORANGE);
        JButton delBtn      = UIFactory.actionButton("Delete",    Theme.RED);
        JButton delMultiBtn = UIFactory.actionButton("Multi-Del", new Color(185, 28, 28));

        gbc.gridx = 0; gbc.gridy = 0; form.add(UIFactory.formLbl("Subject:"), gbc);
        gbc.gridx = 1; form.add(subjectCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 1; form.add(UIFactory.formLbl("Status:"), gbc);
        gbc.gridx = 1; form.add(statusCombo, gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Theme.card());
        btnRow.add(saveBtn); btnRow.add(editBtn);
        btnRow.add(delBtn);  btnRow.add(delMultiBtn);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        form.add(btnRow, gbc);
        formCard.add(form, BorderLayout.CENTER);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"Subject", "Date", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIFactory.styledTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultRenderer(Object.class, statusRowRenderer(tableModel));
        refreshTable(student, tableModel);

        // ── Button actions ────────────────────────────────────────────────────
        saveBtn.addActionListener(e -> {
            String subj = (String) subjectCombo.getSelectedItem();
            if (!SubjectComboHelper.isValidSelection(subj)) {
                UIFactory.toast(panel, "Select a valid subject!"); return;
            }
            boolean present = "Present".equals(statusCombo.getSelectedItem());
            LocalDate today = LocalDate.now();
            List<AttendanceRecord> ex = student.attendanceMap.get(subj);
            if (ex != null)
                for (AttendanceRecord r : ex)
                    if (r.date.equals(today)) {
                        UIFactory.toast(panel, "Already recorded today for " + subj + "!");
                        return;
                    }
            student.attendanceMap.get(subj)
                .add(new AttendanceRecord(subj, today, present));
            DataManager.saveStudent(student);
            refreshTable(student, tableModel);
            UIFactory.toast(panel, "Attendance saved!");
            onDataChange.run();
        });

        delBtn.addActionListener(e -> deleteSingle(
            panel, student, table, tableModel, onDataChange));
        delMultiBtn.addActionListener(e -> deleteMulti(
            panel, student, table, tableModel, onDataChange));
        editBtn.addActionListener(e -> editRecord(
            panel, student, table, tableModel, onDataChange));

        panel.add(formCard, BorderLayout.NORTH);
        panel.add(UIFactory.buildTableCard(table, "Attendance Records"),
            BorderLayout.CENTER);
        return panel;
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    static void refreshTable(Student student, DefaultTableModel model) {
        model.setRowCount(0);
        List<Object[]> rows = new ArrayList<>();
        for (String subj : student.getSubjects())
            for (AttendanceRecord rec : student.attendanceMap.get(subj)) {
                String statusLabel = rec.present ? "Present"
                    : rec.isMedicalLeave() ? "Medical Leave" : "Absent";
                rows.add(new Object[]{subj, rec.date.toString(), statusLabel});
            }
        rows.sort((a, b) -> ((String) b[1]).compareTo((String) a[1]));
        for (Object[] r : rows) model.addRow(r);
    }

    private static DefaultTableCellRenderer statusRowRenderer(
            DefaultTableModel model) {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String st = (String) t.getModel().getValueAt(r, 2);
                    comp.setBackground(Theme.statusRowBg("Present".equals(st)));
                    comp.setForeground(Theme.text());
                }
                return comp;
            }
        };
    }

    // ── Edit / delete ─────────────────────────────────────────────────────────

    private static void deleteSingle(Component parent, Student student,
                                      JTable table, DefaultTableModel model,
                                      Runnable onChange) {
        int row = table.getSelectedRow();
        if (row == -1) { UIFactory.toast(parent, "Select a record!"); return; }
        String subj    = (String) model.getValueAt(row, 0);
        String dateStr = (String) model.getValueAt(row, 1);
        String status  = (String) model.getValueAt(row, 2);
        if (JOptionPane.showConfirmDialog(parent,
                "Delete: " + subj + " | " + dateStr + "?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            boolean present = "Present".equals(status);
            LocalDate date  = LocalDate.parse(dateStr);
            student.attendanceMap.get(subj)
                .removeIf(r -> r.date.equals(date) && r.present == present);
            DataManager.saveStudent(student);
            refreshTable(student, model);
            onChange.run();
        }
    }

    private static void deleteMulti(Component parent, Student student,
                                     JTable table, DefaultTableModel model,
                                     Runnable onChange) {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            UIFactory.toast(parent, "Select records (Ctrl+Click)!"); return;
        }
        if (JOptionPane.showConfirmDialog(parent,
                "Delete " + rows.length + " record(s)?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Integer[] sorted = new Integer[rows.length];
            for (int i = 0; i < rows.length; i++) sorted[i] = rows[i];
            Arrays.sort(sorted, Comparator.reverseOrder());
            for (int r : sorted) {
                String subj    = (String) model.getValueAt(r, 0);
                String dateStr = (String) model.getValueAt(r, 1);
                String status  = (String) model.getValueAt(r, 2);
                boolean present = "Present".equals(status);
                LocalDate date  = LocalDate.parse(dateStr);
                List<AttendanceRecord> recs = student.attendanceMap.get(subj);
                if (recs != null)
                    for (int i = 0; i < recs.size(); i++)
                        if (recs.get(i).date.equals(date)
                                && recs.get(i).present == present) {
                            recs.remove(i); break;
                        }
            }
            DataManager.saveStudent(student);
            refreshTable(student, model);
            onChange.run();
        }
    }

    private static void editRecord(Component parent, Student student,
                                    JTable table, DefaultTableModel model,
                                    Runnable onChange) {
        int row = table.getSelectedRow();
        if (row == -1) { UIFactory.toast(parent, "Select a record!"); return; }
        String subj    = (String) model.getValueAt(row, 0);
        String dateStr = (String) model.getValueAt(row, 1);
        String status  = (String) model.getValueAt(row, 2);

        JDialog dlg = new JDialog(
            SwingUtilities.getWindowAncestor(parent),
            "Edit Attendance",
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(360, 200);
        dlg.setLocationRelativeTo(parent);

        JPanel p = new JPanel(new GridLayout(2, 2, 10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        JTextField dtf = new JTextField(dateStr);
        JComboBox<String> sc =
            new JComboBox<>(new String[]{"Present", "Absent"});
        sc.setSelectedItem(status);
        p.add(new JLabel("Date (YYYY-MM-DD):")); p.add(dtf);
        p.add(new JLabel("Status:")); p.add(sc);

        JButton upd = UIFactory.actionButton("Update",  Theme.GREEN);
        JButton can = UIFactory.actionButton("Cancel",  new Color(100, 100, 100));
        JPanel  bp  = new JPanel(new FlowLayout());
        bp.add(upd); bp.add(can);
        JPanel mp = new JPanel(new BorderLayout());
        mp.add(p, BorderLayout.CENTER); mp.add(bp, BorderLayout.SOUTH);

        upd.addActionListener(e2 -> {
            try {
                LocalDate nd = LocalDate.parse(dtf.getText());
                boolean np   = "Present".equals(sc.getSelectedItem());
                boolean old  = "Present".equals(status);
                LocalDate od = LocalDate.parse(dateStr);
                List<AttendanceRecord> recs = student.attendanceMap.get(subj);
                for (int i = 0; i < recs.size(); i++)
                    if (recs.get(i).date.equals(od) && recs.get(i).present == old) {
                        recs.set(i, new AttendanceRecord(subj, nd, np));
                        break;
                    }
                DataManager.saveStudent(student);
                refreshTable(student, model);
                dlg.dispose();
                onChange.run();
            } catch (Exception ex) {
                UIFactory.toast(dlg, "Invalid date! Use YYYY-MM-DD");
            }
        });
        can.addActionListener(e2 -> dlg.dispose());
        dlg.add(mp);
        dlg.setVisible(true);
    }
}
