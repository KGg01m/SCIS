package com.scis.ui;

import com.scis.db.DataManager;
import com.scis.model.MarksRecord;
import com.scis.model.Student;
import com.scis.ml.MLPredictor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * MarksTab — builds the Marks management panel.
 */
public final class MarksTab {

    private static final String[] TEST_TYPES = {
        "IA1", "IA2", "Quiz", "Test", "Assignment",
        "Project", "Lab", "Final", "Midterm"
    };

    private MarksTab() {}

    public static JPanel build(Student student, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel formCard = UIFactory.buildCard("Add Marks");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.card());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> subjectCombo =
            SubjectComboHelper.create(panel, student, () -> onDataChange.run());
        JComboBox<String> testCombo = new JComboBox<>(TEST_TYPES);
        UIFactory.styleCombo(testCombo);

        JTextField marksField = UIFactory.styledTextField("e.g. 75");
        JTextField maxField   = UIFactory.styledTextField("e.g. 100");

        JButton saveBtn     = UIFactory.actionButton("Save",      Theme.GREEN);
        JButton editBtn     = UIFactory.actionButton("Edit",      Theme.ORANGE);
        JButton delBtn      = UIFactory.actionButton("Delete",    Theme.RED);
        JButton delMultiBtn = UIFactory.actionButton("Multi-Del", new Color(185, 28, 28));

        gbc.gridx = 0; gbc.gridy = 0; form.add(UIFactory.formLbl("Subject:"),   gbc);
        gbc.gridx = 1; form.add(subjectCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 1; form.add(UIFactory.formLbl("Test Type:"), gbc);
        gbc.gridx = 1; form.add(testCombo,    gbc);
        gbc.gridx = 0; gbc.gridy = 2; form.add(UIFactory.formLbl("Marks:"),     gbc);
        gbc.gridx = 1; form.add(marksField,   gbc);
        gbc.gridx = 0; gbc.gridy = 3; form.add(UIFactory.formLbl("Max:"),       gbc);
        gbc.gridx = 1; form.add(maxField,     gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Theme.card());
        btnRow.add(saveBtn); btnRow.add(editBtn);
        btnRow.add(delBtn);  btnRow.add(delMultiBtn);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        form.add(btnRow, gbc);
        formCard.add(form, BorderLayout.CENTER);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"Subject", "Test Type", "Marks", "Max", "Percentage", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIFactory.styledTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultRenderer(Object.class, statusRowRenderer());
        refreshTable(student, tableModel);

        // ── Button actions ────────────────────────────────────────────────────
        saveBtn.addActionListener(e -> {
            String subj = (String) subjectCombo.getSelectedItem();
            if (!SubjectComboHelper.isValidSelection(subj)) {
                UIFactory.toast(panel, "Select a valid subject!"); return;
            }
            try {
                double marks = Double.parseDouble(marksField.getText().trim());
                double maxM  = Double.parseDouble(maxField.getText().trim());
                if (maxM > 100) { UIFactory.toast(panel, "Max marks cannot exceed 100!"); return; }
                if (marks < 0 || marks > maxM) {
                    UIFactory.toast(panel, "Marks must be 0–" + maxM); return;
                }
                student.addMarks(subj, (String) testCombo.getSelectedItem(),
                    marks, maxM);
                DataManager.saveStudent(student);
                marksField.setText("");
                refreshTable(student, tableModel);
                UIFactory.toast(panel, "Marks saved!");
                onDataChange.run();
            } catch (NumberFormatException ex) {
                UIFactory.toast(panel, "Enter valid numbers!");
            }
        });

        delBtn.addActionListener(e -> deleteSingle(
            panel, student, table, tableModel, onDataChange));
        delMultiBtn.addActionListener(e -> deleteMulti(
            panel, student, table, tableModel, onDataChange));
        editBtn.addActionListener(e -> editRecord(
            panel, student, table, tableModel, onDataChange));

        panel.add(formCard, BorderLayout.NORTH);
        panel.add(UIFactory.buildTableCard(table, "Marks Records"),
            BorderLayout.CENTER);
        return panel;
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    static void refreshTable(Student student, DefaultTableModel model) {
        model.setRowCount(0);
        for (String subj : student.getSubjects()) {
            List<MarksRecord> recs = student.marksMap.get(subj);
            for (MarksRecord rec : recs) {
                double pct = (rec.marksObtained / rec.maxMarks) * 100;
                model.addRow(new Object[]{
                    subj, rec.testType,
                    String.format("%.1f", rec.marksObtained),
                    String.format("%.0f", rec.maxMarks),
                    String.format("%.1f%%", pct),
                    pct >= 60 ? "PASS" : "FAIL"
                });
            }
        }
    }

    private static DefaultTableCellRenderer statusRowRenderer() {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String st = (String) t.getModel().getValueAt(r, 5);
                    comp.setBackground(Theme.statusRowBg("PASS".equals(st)));
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
        String subj = (String) model.getValueAt(row, 0);
        String tt   = (String) model.getValueAt(row, 1);
        String ms   = (String) model.getValueAt(row, 2);
        if (JOptionPane.showConfirmDialog(parent,
                "Delete " + subj + " - " + tt + "?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            removeRecord(student, subj, tt, ms);
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
                removeRecord(student,
                    (String) model.getValueAt(r, 0),
                    (String) model.getValueAt(r, 1),
                    (String) model.getValueAt(r, 2));
            }
            DataManager.saveStudent(student);
            refreshTable(student, model);
            onChange.run();
        }
    }

    private static void removeRecord(Student student,
                                      String subj, String tt, String ms) {
        List<MarksRecord> recs = student.marksMap.get(subj);
        if (recs == null) return;
        for (int i = 0; i < recs.size(); i++)
            if (recs.get(i).testType.equals(tt)
                    && String.format("%.1f", recs.get(i).marksObtained).equals(ms)) {
                recs.remove(i); break;
            }
    }

    private static void editRecord(Component parent, Student student,
                                    JTable table, DefaultTableModel model,
                                    Runnable onChange) {
        int row = table.getSelectedRow();
        if (row == -1) { UIFactory.toast(parent, "Select a record!"); return; }
        String subj = (String) model.getValueAt(row, 0);
        String tt   = (String) model.getValueAt(row, 1);
        String ms   = (String) model.getValueAt(row, 2);
        String maxS = (String) model.getValueAt(row, 3);

        JDialog dlg = new JDialog(
            SwingUtilities.getWindowAncestor(parent),
            "Edit Marks",
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(380, 230);
        dlg.setLocationRelativeTo(parent);

        JPanel p = new JPanel(new GridLayout(3, 2, 10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JComboBox<String> ttc = new JComboBox<>(TEST_TYPES);
        ttc.setSelectedItem(tt);
        JTextField mf  = new JTextField(ms);
        JTextField mxf = new JTextField(maxS);
        p.add(new JLabel("Test Type:")); p.add(ttc);
        p.add(new JLabel("Marks:"));     p.add(mf);
        p.add(new JLabel("Max:"));       p.add(mxf);

        JButton sv = UIFactory.actionButton("Save",   Theme.GREEN);
        JButton cn = UIFactory.actionButton("Cancel", new Color(100, 100, 100));
        JPanel bp  = new JPanel(new FlowLayout()); bp.add(sv); bp.add(cn);
        JPanel mp  = new JPanel(new BorderLayout());
        mp.add(p, BorderLayout.CENTER); mp.add(bp, BorderLayout.SOUTH);

        sv.addActionListener(e2 -> {
            try {
                double nm  = Double.parseDouble(mf.getText());
                double nmx = Double.parseDouble(mxf.getText());
                List<MarksRecord> recs = student.marksMap.get(subj);
                if (recs != null)
                    for (int i = 0; i < recs.size(); i++)
                        if (recs.get(i).testType.equals(tt)
                                && String.format("%.1f", recs.get(i).marksObtained).equals(ms)) {
                            recs.set(i, new MarksRecord(subj,
                                (String) ttc.getSelectedItem(), nm, nmx));
                            break;
                        }
                DataManager.saveStudent(student);
                refreshTable(student, model);
                dlg.dispose();
                onChange.run();
            } catch (Exception ex) {
                UIFactory.toast(dlg, "Invalid values!");
            }
        });
        cn.addActionListener(e2 -> dlg.dispose());
        dlg.add(mp);
        dlg.setVisible(true);
    }
}
