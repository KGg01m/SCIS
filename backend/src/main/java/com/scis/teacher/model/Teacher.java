package com.scis.teacher.model;

import java.io.Serializable;
import java.util.*;

/**
 * Teacher — domain object representing a faculty member.
 *
 * A teacher:
 *  - belongs to a department
 *  - teaches one or more subjects (each tied to a class/section)
 *  - can mark attendance and assign marks for students in their classes
 *  - can post assignments and announcements
 */
public class Teacher implements Serializable {
    private static final long serialVersionUID = 1L;

    public String teacherId;      // e.g. TCH001
    public String name;
    public String email;
    public String department;
    public String designation;    // e.g. "Assistant Professor"
    public String collegeName;
    public String password;

    /** Subjects this teacher is responsible for: subjectName -> list of class sections */
    public Map<String, List<String>> subjectClassMap = new LinkedHashMap<>();

    /** Assignments created by this teacher */
    public List<TeacherAssignment> assignments = new ArrayList<>();

    /** Announcements posted to students */
    public List<TeacherAnnouncement> announcements = new ArrayList<>();

    /** All attendance records entered by this teacher (loaded at runtime). */
    public List<TeacherAttendanceRecord> attendanceLog = new ArrayList<>();

    /** All marks records entered by this teacher (loaded at runtime). */
    public List<TeacherMarksRecord> marksLog = new ArrayList<>();

    /** Class codes assigned to this teacher (e.g. "MATH-101", "PHY-101"). */
    public List<String> assignedClassCodes = new ArrayList<>();

    public Teacher() {}

    // ── Field validation (public static so tests can call directly) ───────────

    public static String validateTeacherId(String id) {
        if (id == null || id.trim().isEmpty())
            throw new IllegalArgumentException("teacherId must not be null or blank");
        String t = id.trim();
        if (t.length() > 30)
            throw new IllegalArgumentException("teacherId must be <= 30 characters");
        if (!t.matches("[A-Za-z0-9_\\-]+"))
            throw new IllegalArgumentException(
                "teacherId must contain only letters, digits, hyphens or underscores: \"" + t + "\"");
        return t;
    }

    public static String validateTeacherName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("name must not be null or blank");
        String t = name.trim();
        if (t.length() > 100)
            throw new IllegalArgumentException("name must be <= 100 characters");
        if (!t.matches("[A-Za-z][A-Za-z .'\\-]*"))
            throw new IllegalArgumentException(
                "name must start with a letter and contain only letters, spaces, dots, apostrophes or hyphens");
        return t;
    }

    public static String validateTeacherEmail(String email) {
        if (email == null || email.trim().isEmpty())
            throw new IllegalArgumentException("email must not be null or blank");
        String t = email.trim();
        long atCount = t.chars().filter(c -> c == '@').count();
        if (atCount != 1)
            throw new IllegalArgumentException("email must contain exactly one '@': \"" + t + "\"");
        int at = t.indexOf('@');
        if (at == 0 || at == t.length() - 1 || !t.substring(at + 1).contains("."))
            throw new IllegalArgumentException("email is not valid: \"" + t + "\"");
        return t.toLowerCase();
    }

    /** email: checks if email already exists in the database */
    public static String validateUniqueTeacherEmail(String email, String excludeTeacherId) {
        String normalizedEmail = validateTeacherEmail(email);
        try {
            // Check if email already exists for a different teacher
            com.scis.teacher.db.TeacherDataManager.EmailCheckResult result = 
                com.scis.teacher.db.TeacherDataManager.checkEmailExists(normalizedEmail, excludeTeacherId);
            if (result.exists) {
                throw new IllegalArgumentException(
                    "email address already exists: \"" + normalizedEmail + "\"");
            }
            return normalizedEmail;
        } catch (Exception e) {
            // If database check fails, still return normalized email for basic validation
            return normalizedEmail;
        }
    }

    public static String validateTeacherPassword(String password) {
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("password must be at least 6 characters");
        return password;
    }

    public Teacher(String id, String name, String email, String department,
                   String designation, String college, String password) {
        this.teacherId   = id;
        this.name        = name;
        this.email       = email;
        this.department  = department;
        this.designation = designation;
        this.collegeName = college != null ? college : "Unknown College";
        this.password    = password;
    }

    /** Returns all subjects this teacher teaches. */
    public String[] getSubjects() {
        return subjectClassMap.keySet().toArray(new String[0]);
    }

    /** Adds a subject (and optionally a class/section) to this teacher's profile. */
    public void addSubject(String subject, String classSection) {
        if (subject == null || subject.trim().isEmpty()) return;
        List<String> sections = subjectClassMap
            .computeIfAbsent(subject.trim(), k -> new ArrayList<>());
        if (classSection != null && !classSection.trim().isEmpty()
                && !sections.contains(classSection.trim())) {
            sections.add(classSection.trim());
        }
    }

    public void removeSubject(String subject) {
        subjectClassMap.remove(subject);
    }

    /** Returns class sections for a given subject. */
    public List<String> getSectionsForSubject(String subject) {
        return subjectClassMap.getOrDefault(subject, Collections.emptyList());
    }

    public List<TeacherAssignment> getAssignments() {
        if (assignments == null) assignments = new ArrayList<>();
        return assignments;
    }

    public List<TeacherAnnouncement> getAnnouncements() {
        if (announcements == null) announcements = new ArrayList<>();
        return announcements;
    }

    /** Returns active (non-expired) assignments. */
    public List<TeacherAssignment> getActiveAssignments() {
        List<TeacherAssignment> active = new ArrayList<>();
        for (TeacherAssignment a : getAssignments())
            if (!a.isExpired()) active.add(a);
        return active;
    }
}
