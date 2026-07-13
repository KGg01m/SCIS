package com.scis.ui;

import com.scis.model.Student;
import com.scis.ml.MLPredictor;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * PerformanceTab — subject-wise performance overview with bar charts.
 */
public final class PerformanceTab {

    private PerformanceTab() {}

    public static JPanel build(Student student) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel hdr = new JLabel("Subject-Wise Performance");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hdr.setForeground(Theme.text());
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        // Subject cards
        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        cardsRow.setBackground(Theme.bg());
        String[] subjects = student.getSubjects();
        if (subjects.length == 0) {
            JLabel none = new JLabel(
                "No subjects yet. Add subjects via Attendance or Marks tabs.");
            none.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            none.setForeground(Theme.muted());
            cardsRow.add(none);
        } else {
            for (String subj : subjects) cardsRow.add(subjectCard(subj, student));
        }

        JScrollPane cardScroll = new JScrollPane(cardsRow,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cardScroll.setPreferredSize(new Dimension(0, 150));
        cardScroll.setBorder(null);
        cardScroll.setBackground(Theme.bg());

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(Theme.bg());
        top.add(hdr, BorderLayout.NORTH);
        top.add(cardScroll, BorderLayout.CENTER);

        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0));
        charts.setBackground(Theme.bg());
        charts.add(UIFactory.wrapChart("Attendance by Subject (%)",
            ChartPanel.attendanceChart(student)));
        charts.add(UIFactory.wrapChart("Performance by Subject (%)",
            ChartPanel.performanceChart(student)));

        panel.add(top, BorderLayout.NORTH);
        panel.add(charts, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel subjectCard(String subject, Student student) {
        double att   = student.getSubjectAttendance(subject);
        double perf  = student.getSubjectPerformance(subject);
        String status = student.getSubjectPassFailStatus(subject);

        JPanel crd = new JPanel();
        crd.setLayout(new BoxLayout(crd, BoxLayout.Y_AXIS));
        crd.setBackground(Theme.card());
        crd.setPreferredSize(new Dimension(170, 130));
        crd.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder("PASS".equals(status) ? Theme.GREEN : Theme.RED, 2, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel sl = new JLabel(subject.length() > 15
            ? subject.substring(0, 15) + ".." : subject);
        sl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sl.setForeground(Theme.text());

        JLabel al = new JLabel("[C] " + String.format("%.1f%%", att));
        al.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        al.setForeground(att >= 75 ? Theme.GREEN : Theme.RED);

        JLabel pl = new JLabel("[G] " + String.format("%.1f%%", perf));
        pl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pl.setForeground(perf >= 60 ? Theme.GREEN : Theme.RED);

        JLabel stl = new JLabel("PASS".equals(status) ? "[v] PASS" : "[x] FAIL");
        stl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        stl.setForeground("PASS".equals(status) ? Theme.GREEN : Theme.RED);

        crd.add(sl);
        crd.add(Box.createRigidArea(new Dimension(0, 6)));
        crd.add(al);
        crd.add(Box.createRigidArea(new Dimension(0, 3)));
        crd.add(pl);
        crd.add(Box.createRigidArea(new Dimension(0, 6)));
        crd.add(stl);
        return crd;
    }
}
