package com.scis.ui;

import com.scis.model.AttendanceRecord;
import com.scis.model.Student;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * WhatIfDialog — modal "What-If Attendance Simulator" dialog.
 * Lets the user pick a subject, enter future class count, and see
 * the predicted new attendance percentage.
 */
public final class WhatIfDialog {

    private WhatIfDialog() {}

    public static void show(Component parent, Student student) {
        JDialog dlg = new JDialog(
            SwingUtilities.getWindowAncestor(parent),
            "What-If Attendance Simulator",
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(500, 440);
        dlg.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Theme.card());

        JLabel title = new JLabel("What-If Simulator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Theme.PURPLE);

        // Input grid
        JPanel ip = new JPanel(new GridLayout(3, 2, 10, 14));
        ip.setBackground(Theme.card());

        JComboBox<String> sc = new JComboBox<>();
        for (String s : student.getSubjects()) sc.addItem(s);

        JSpinner futureClasses = new JSpinner(
            new SpinnerNumberModel(10, 1, 50, 1));
        JSpinner missClasses = new JSpinner(
            new SpinnerNumberModel(0, 0, 50, 1));

        ip.add(new JLabel("Subject:"));        ip.add(sc);
        ip.add(new JLabel("Future Classes:")); ip.add(futureClasses);
        ip.add(new JLabel("Classes to Miss:")); ip.add(missClasses);

        JTextArea ra = new JTextArea();
        ra.setEditable(false);
        ra.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ra.setBackground(Theme.surface());

        JButton simBtn = UIFactory.bigButton("Simulate →", Theme.PURPLE);
        simBtn.addActionListener(e -> {
            String subj = (String) sc.getSelectedItem();
            if (subj == null) return;

            int fc = (Integer) futureClasses.getValue();
            int mc = (Integer) missClasses.getValue();
            double ca = student.getSubjectAttendance(subj);

            List<AttendanceRecord> recs = student.attendanceMap.get(subj);
            int ct  = (recs != null && recs.size() > 0) ? recs.size() : 10;
            int cp  = (int)((ca / 100.0) * ct);
            double newAtt = ((cp + fc - mc) * 100.0) / (ct + fc);

            StringBuilder result = new StringBuilder();
            result.append("=== SIMULATION RESULTS ===\n\n");
            result.append("Subject: ").append(subj).append("\n");
            result.append("Current: ").append(String.format("%.1f%%", ca))
                  .append(" (").append(ct).append(" classes)\n\n");
            result.append("Future: ").append(fc)
                  .append(" classes, miss ").append(mc).append("\n\n");
            result.append("New Attendance: ")
                  .append(String.format("%.1f%%", newAtt)).append("\n");

            if (newAtt < 75)
                result.append("\n[!!] WARNING: Below 75% threshold!");
            else if (newAtt < 80)
                result.append("\n[!] CAUTION: Close to danger zone.");
            else
                result.append("\n[OK] GOOD: You are on track!");

            ra.setText(result.toString());
        });

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setBackground(Theme.card());
        top.add(title,  BorderLayout.NORTH);
        top.add(ip,     BorderLayout.CENTER);
        top.add(simBtn, BorderLayout.SOUTH);

        panel.add(top,                 BorderLayout.NORTH);
        panel.add(new JScrollPane(ra), BorderLayout.CENTER);

        dlg.add(panel);
        dlg.setVisible(true);
    }
}
