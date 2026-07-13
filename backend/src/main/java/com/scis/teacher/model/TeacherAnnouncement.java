package com.scis.teacher.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * TeacherAnnouncement — a notice or announcement posted by a teacher.
 * Can target all students or a specific subject/section.
 */
public class TeacherAnnouncement implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String[] PRIORITIES = {"High", "Medium", "Low"};

    public String    id;
    public String    title;
    public String    message;
    public String    subject;       // null = all subjects
    public String    classSection;  // null = all sections
    public String    priority;      // "High", "Medium", "Low"
    public LocalDate postedDate;
    public LocalDate expiryDate;    // null = no expiry

    public TeacherAnnouncement() {
        this.id         = UUID.randomUUID().toString().substring(0, 8);
        this.postedDate = LocalDate.now();
        this.priority   = "Medium";
    }

    public TeacherAnnouncement(String title, String message, String subject,
                                String classSection, String priority,
                                LocalDate expiryDate) {
        this();
        this.title        = title;
        this.message      = message;
        this.subject      = subject;
        this.classSection = classSection;
        this.priority     = priority;
        this.expiryDate   = expiryDate;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isHighPriority() { return "High".equals(priority); }
}
