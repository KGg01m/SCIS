package com.scis.ui.teacher;

import com.scis.ui.Theme;

import com.scis.teacher.model.*;
import com.scis.ui.UIFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * TeacherDashboardTab — analytics overview for the teacher portal.
 * Shows: total students marked, assignments pending grading,
 * upcoming due dates, and quick-action buttons.
 */
public final class TeacherDashboardTab {

    private TeacherDashboardTab() {}

    public static JPanel build(Teacher teacher) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Welcome header ────────────────────────────────────────────────────
        JPanel welcome = new JPanel(new BorderLayout());
        welcome.setBackground(Theme.bg());
        JLabel greet = new JLabel("Welcome, " + teacher.name);
        greet.setFont(new Font("Segoe UI", Font.BOLD, 22));
        greet.setForeground(Theme.text());
        JLabel sub = new JLabel(teacher.designation + " · " + teacher.department
            + " · " + teacher.collegeName);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(Theme.muted());
        JPanel wLeft = new JPanel(new GridLayout(2, 1, 0, 2));
        wLeft.setBackground(Theme.bg());
        wLeft.add(greet); wLeft.add(sub);
        welcome.add(wLeft, BorderLayout.WEST);

        // ── Stat cards ────────────────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setBackground(Theme.bg());

        int subjects     = teacher.getSubjects().length;
        int attRecords   = teacher.attendanceLog != null ? teacher.attendanceLog.size() : 0;
        int marksRecords = teacher.marksLog      != null ? teacher.marksLog.size()      : 0;
        int pendingGrade = 0;
        int activeAsgn   = 0;
        for (TeacherAssignment a : teacher.getActiveAssignments()) {
            activeAsgn++;
            pendingGrade += (a.submissionMap.size() - a.marksMap.size());
        }

        statsRow.add(statCard("Subjects Taught",    String.valueOf(subjects),
            Theme.TEACHER_TEAL,    "subjects"));
        statsRow.add(statCard("Attendance Entries", String.valueOf(attRecords),
            Theme.EMERALD, "att"));
        statsRow.add(statCard("Active Assignments", String.valueOf(activeAsgn),
            Theme.TEACHER_PURPLE,  "asgn"));
        statsRow.add(statCard("Pending Grading",    String.valueOf(Math.max(0, pendingGrade)),
            pendingGrade > 0 ? Theme.AMBER : Theme.EMERALD, "grade"));

        // ── Bottom row: recent assignments + announcements ────────────────────
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomRow.setBackground(Theme.bg());
        bottomRow.add(buildRecentAssignments(teacher));
        bottomRow.add(buildRecentAnnouncements(teacher));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setBackground(Theme.bg());
        top.add(welcome,  BorderLayout.NORTH);
        top.add(statsRow, BorderLayout.CENTER);

        panel.add(top,       BorderLayout.NORTH);
        panel.add(bottomRow, BorderLayout.CENTER);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JPanel statCard(String label, String value,
                                    Color accent, String type) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        JPanel bar = new JPanel();
        bar.setBackground(accent); bar.setPreferredSize(new Dimension(0, 3));

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valLbl.setForeground(accent);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(Theme.muted());

        card.add(bar,    BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        card.add(lbl,    BorderLayout.SOUTH);
        return card;
    }

    private static JPanel buildRecentAssignments(Teacher teacher) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel title = new JLabel("Active Assignments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(Theme.text());

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.card());

        List<TeacherAssignment> active = teacher.getActiveAssignments();
        if (active.isEmpty()) {
            JLabel none = new JLabel("No active assignments.");
            none.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            none.setForeground(Theme.muted());
            list.add(none);
        } else {
            for (TeacherAssignment a : active) {
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(Theme.darkMode
                    ? new Color(13, 35, 30) : new Color(236, 253, 245));
                row.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.border(), 1, true),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                row.setMaximumSize(new Dimension(10000, 50));

                JLabel tl = new JLabel("[" + a.assignmentType + "] " + a.title);
                tl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                tl.setForeground(Theme.text());

                JLabel dl = new JLabel(a.subject + " · Due: "
                    + (a.dueDate != null ? a.dueDate : "N/A")
                    + " · " + a.getSubmittedCount() + " submitted");
                dl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                dl.setForeground(Theme.muted());

                JPanel inner = new JPanel(new GridLayout(2, 1));
                inner.setBackground(row.getBackground());
                inner.add(tl); inner.add(dl);
                row.add(inner, BorderLayout.CENTER);
                list.add(row);
                list.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }

        card.add(title, BorderLayout.NORTH);
        card.add(new JScrollPane(list), BorderLayout.CENTER);
        return card;
    }

    private static JPanel buildRecentAnnouncements(Teacher teacher) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel title = new JLabel("Recent Announcements");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(Theme.text());

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.card());

        List<TeacherAnnouncement> anns = teacher.getAnnouncements();
        if (anns.isEmpty()) {
            JLabel none = new JLabel("No announcements posted.");
            none.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            none.setForeground(Theme.muted());
            list.add(none);
        } else {
            int shown = 0;
            for (int i = anns.size() - 1; i >= 0 && shown < 5; i--, shown++) {
                TeacherAnnouncement an = anns.get(i);
                Color bg = an.isHighPriority()
                    ? (Theme.darkMode ? new Color(60, 20, 5) : new Color(255, 251, 235))
                    : Theme.card();
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(bg);
                row.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.border(), 1, true),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                row.setMaximumSize(new Dimension(10000, 50));
                JLabel tl = new JLabel(an.title);
                tl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                tl.setForeground(Theme.text());
                JLabel ml = new JLabel(an.subject != null
                    ? an.subject + " · " + an.postedDate
                    : "All · " + an.postedDate);
                ml.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                ml.setForeground(Theme.muted());
                JPanel inner = new JPanel(new GridLayout(2, 1));
                inner.setBackground(bg); inner.add(tl); inner.add(ml);
                row.add(inner, BorderLayout.CENTER);
                list.add(row);
                list.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }

        card.add(title, BorderLayout.NORTH);
        card.add(new JScrollPane(list), BorderLayout.CENTER);
        return card;
    }
}
