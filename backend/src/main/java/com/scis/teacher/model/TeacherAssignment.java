package com.scis.teacher.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * TeacherAssignment — an assignment or task created by a teacher and
 * assigned to students in a particular subject/section.
 *
 * Tracks which students have submitted and stores individual marks.
 */
public class TeacherAssignment implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String[] ASSIGNMENT_TYPES = {
        "Assignment", "Quiz", "Project", "Lab Report",
        "Presentation", "Case Study", "Research Paper", "Other"
    };

    public String    id;
    public String    title;
    public String    description;
    public String    subject;
    public String    classSection;
    public String    assignmentType;
    public double    maxMarks;
    public LocalDate assignedDate;
    public LocalDate dueDate;
    public LocalDate createdDate;

    /** studentId -> submitted flag */
    public Map<String, Boolean> submissionMap = new HashMap<>();

    /** studentId -> marks awarded */
    public Map<String, Double> marksMap = new HashMap<>();

    /** studentId -> teacher feedback/comment */
    public Map<String, String> feedbackMap = new HashMap<>();

    public TeacherAssignment() {
        this.id          = UUID.randomUUID().toString().substring(0, 8);
        this.createdDate = LocalDate.now();
        this.assignedDate = LocalDate.now();
    }

    public TeacherAssignment(String title, String description, String subject,
                              String classSection, String type,
                              double maxMarks, LocalDate dueDate) {
        this();
        this.title          = title;
        this.description    = description;
        this.subject        = subject;
        this.classSection   = classSection;
        this.assignmentType = type;
        this.maxMarks       = maxMarks;
        this.dueDate        = dueDate;
    }

    public boolean isExpired() {
        return dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;
        return LocalDate.now().until(dueDate, ChronoUnit.DAYS);
    }

    public String getStatusLabel() {
        if (dueDate == null) return "Open";
        if (isExpired()) return "Closed";
        long days = getDaysUntilDue();
        if (days <= 1) return "Due Today";
        if (days <= 3) return "Due Soon";
        return "Open";
    }

    public int getSubmittedCount() {
        int count = 0;
        for (Boolean b : submissionMap.values()) if (Boolean.TRUE.equals(b)) count++;
        return count;
    }

    public int getGradedCount() { return marksMap.size(); }

    public void markSubmitted(String studentId) {
        submissionMap.put(studentId, true);
    }

    public void awardMarks(String studentId, double marks, String feedback) {
        submissionMap.put(studentId, true);
        marksMap.put(studentId, marks);
        if (feedback != null && !feedback.isEmpty())
            feedbackMap.put(studentId, feedback);
    }

    public double getStudentMarks(String studentId) {
        Double m = marksMap.get(studentId);
        return m != null ? m : -1.0;
    }

    public boolean isSubmitted(String studentId) {
        return Boolean.TRUE.equals(submissionMap.get(studentId));
    }

    /** Average marks across all graded students (0–maxMarks). */
    public double getClassAverage() {
        if (marksMap.isEmpty()) return 0.0;
        double sum = 0;
        for (double v : marksMap.values()) sum += v;
        return sum / marksMap.size();
    }
}
