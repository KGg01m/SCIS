package com.scis.teacher.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * TeacherAttendanceRecord — a single attendance entry recorded by a teacher
 * for a specific student in a specific subject on a specific date.
 *
 * These records are stored inside the teacher document (teacher owns them)
 * and are also written back to the student's own attendance map so the
 * student dashboard reflects teacher-entered data.
 */
public class TeacherAttendanceRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public String    studentId;
    public String    studentName;
    public String    subject;
    public String    classSection;   // e.g. "CS-A", "3rd Year B"
    public LocalDate date;
    public boolean   present;
    public String    remarks;        // optional note, e.g. "Medical leave"

    public TeacherAttendanceRecord() {}

    public TeacherAttendanceRecord(String studentId, String studentName,
                                    String subject, String classSection,
                                    LocalDate date, boolean present) {
        this.studentId    = studentId;
        this.studentName  = studentName;
        this.subject      = subject;
        this.classSection = classSection;
        this.date         = date;
        this.present      = present;
    }

    public String getStatusLabel() { return present ? "Present" : "Absent"; }
}
