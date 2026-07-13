package com.scis.teacher.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * TeacherMarksRecord — a marks entry entered by a teacher for a student.
 *
 * Stored in the teacher's document and mirrored into the student's
 * marksMap so the student sees their grades in their own dashboard.
 */
public class TeacherMarksRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String[] EXAM_TYPES = {
        "IA1", "IA2", "Quiz", "Assignment", "Lab",
        "Midterm", "Final", "Project", "Viva", "Other"
    };

    public String    studentId;
    public String    studentName;
    public String    subject;
    public String    classSection;
    public String    examType;      // "IA1", "Assignment", etc.
    public double    marksObtained;
    public double    maxMarks;
    public LocalDate examDate;
    public String    remarks;       // teacher's comment on the result

    public TeacherMarksRecord() {}

    public TeacherMarksRecord(String studentId, String studentName,
                               String subject, String classSection,
                               String examType, double obtained, double max,
                               LocalDate examDate) {
        this.studentId    = studentId;
        this.studentName  = studentName;
        this.subject      = subject;
        this.classSection = classSection;
        this.examType     = examType;
        this.marksObtained = obtained;
        this.maxMarks     = max;
        this.examDate     = examDate;
    }

    public double getPercentage() {
        return maxMarks > 0 ? (marksObtained * 100.0 / maxMarks) : 0.0;
    }

    public String getGrade() {
        double pct = getPercentage();
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B";
        if (pct >= 60) return "C";
        if (pct >= 50) return "D";
        return "F";
    }

    public boolean isPassing() { return getPercentage() >= 40.0; }
}
