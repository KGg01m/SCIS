package com.scis.ui;

import com.scis.db.DataManager;
import com.scis.model.Student;

import javax.swing.*;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SubjectComboHelper — manages the subject selector combo box that
 * includes "Add Subject" and "Remove Subject" special entries.
 */
public final class SubjectComboHelper {

    public static final String ADD_SUBJECT    = "< Add Subject... >";
    public static final String REMOVE_SUBJECT = "< Remove Subject... >";

    /** Callback invoked whenever subjects are added or removed. */
    public interface SubjectChangeCallback {
        void onChanged();
    }

    private SubjectComboHelper() {}

    /**
     * Creates a subject combo populated from {@code student}'s subjects,
     * plus the two special entries at the bottom.
     *
     * @param parent   parent component for dialogs
     * @param student  the logged-in student
     * @param onChange called after any add/remove action
     */
    public static JComboBox<String> create(Component parent,
                                            Student student,
                                            SubjectChangeCallback onChange) {
        List<String> items = new ArrayList<>(
            Arrays.asList(student.getSubjects()));
        items.add(ADD_SUBJECT);
        items.add(REMOVE_SUBJECT);

        JComboBox<String> combo =
            new JComboBox<>(items.toArray(new String[0]));

        combo.addActionListener(e -> {
            String sel = (String) combo.getSelectedItem();

            if (ADD_SUBJECT.equals(sel)) {
                String nm = JOptionPane.showInputDialog(
                    parent, "Enter new subject name:");
                if (nm != null && !nm.trim().isEmpty()) {
                    student.addSubject(nm.trim());
                    student.enrollInSubject(nm.trim()); // sync for teacher cross-reference
                    DataManager.saveStudent(student);
                    refresh(combo, student);
                    combo.setSelectedItem(nm.trim());
                    onChange.onChanged();
                } else {
                    combo.setSelectedIndex(0);
                }

            } else if (REMOVE_SUBJECT.equals(sel)) {
                String[] subs = student.getSubjects();
                if (subs.length == 0) {
                    JOptionPane.showMessageDialog(parent,
                        "No subjects to remove!");
                    combo.setSelectedIndex(0);
                    return;
                }
                String rm = (String) JOptionPane.showInputDialog(
                    parent, "Select subject to remove:",
                    "Remove Subject", JOptionPane.QUESTION_MESSAGE,
                    null, subs, subs[0]);
                if (rm != null && JOptionPane.showConfirmDialog(
                        parent,
                        "Remove '" + rm + "' and all its data?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    student.removeSubject(rm);
                    DataManager.saveStudent(student);
                    refresh(combo, student);
                    onChange.onChanged();
                } else {
                    combo.setSelectedIndex(0);
                }
            }
        });

        if (combo.getItemCount() > 2) combo.setSelectedIndex(0);
        UIFactory.styleCombo(combo);
        return combo;
    }

    /** Refreshes the combo items from the student's current subject list. */
    public static void refresh(JComboBox<String> combo, Student student) {
        combo.removeAllItems();
        for (String s : student.getSubjects()) combo.addItem(s);
        combo.addItem(ADD_SUBJECT);
        combo.addItem(REMOVE_SUBJECT);
        if (combo.getItemCount() > 2) combo.setSelectedIndex(0);
    }

    /** Returns {@code true} when the selection is a valid (non-special) subject. */
    public static boolean isValidSelection(String sel) {
        return sel != null
            && !sel.equals(ADD_SUBJECT)
            && !sel.equals(REMOVE_SUBJECT);
    }
}
