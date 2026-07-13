// Student model - stores student data, attendance, marks, and medical leaves
package com.scis.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

public class Student implements Serializable {
    private static final long serialVersionUID = 2L;

    public String studentId;
    public String name;
    public String email;
    public String department;
    public int semester;
    public String password;
    public String collegeName;

    public Map<String, List<AttendanceRecord>> attendanceMap = new HashMap<>();
    public Map<String, List<MarksRecord>> marksMap = new HashMap<>();
    public Map<String, Double> classAverageMarks = new HashMap<>();
    public Map<String, Double> passingMarks = new HashMap<>();
    public double overallPassingMarks = 40.0;
    public List<StudentTask> tasks = new ArrayList<>();

    public List<MedicalLeave> medicalLeaves = new ArrayList<>();

    public List<String> enrolledSubjects = new ArrayList<>();

    public Student() {}

    public Student(String id, String name, String email, String dept, int sem, String pass, String college) {
        this.studentId = validateStudentId(id);
        this.name = validateName(name);
        this.email = validateEmail(email);
        this.department = validateDepartment(dept);
        this.semester = validateSemester(sem);
        this.password = validatePassword(pass);
        this.collegeName = college != null && !college.trim().isEmpty() ? college.trim() : "Unknown College";
    }

    public static String validateStudentId(String id) {
        if (id == null || id.trim().isEmpty())
            throw new IllegalArgumentException("studentId must not be null or blank");
        String t = id.trim();
        if (t.length() > 30)
            throw new IllegalArgumentException("studentId must be <= 30 characters, got " + t.length());
        if (!t.matches("[A-Za-z0-9_\\-]+"))
            throw new IllegalArgumentException(
                "studentId must contain only letters, digits, hyphens or underscores: \"" + t + "\"");
        return t;
    }

    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("name must not be null or blank");
        String t = name.trim();
        if (t.length() > 100)
            throw new IllegalArgumentException("name must be <= 100 characters, got " + t.length());
        if (!t.matches("[A-Za-z][A-Za-z .'\\-]*"))
            throw new IllegalArgumentException(
                "name must start with a letter and contain only letters, spaces, dots, apostrophes or hyphens: \"" + t + "\"");
        return t;
    }

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty())
            throw new IllegalArgumentException("email must not be null or blank");
        String t = email.trim();
        int at = t.indexOf('@');
        if (at <= 0 || at != t.lastIndexOf('@'))
            throw new IllegalArgumentException(
                "email must contain exactly one '@' with a non-empty local part: \"" + t + "\"");
        String domain = t.substring(at + 1);
        if (domain.isEmpty() || !domain.contains("."))
            throw new IllegalArgumentException(
                "email domain must contain at least one '.': \"" + t + "\"");
        return t.toLowerCase();
    }

    public static String validateUniqueEmail(String email, String excludeStudentId) {
        String normalizedEmail = validateEmail(email);
        try {
            com.scis.db.DataManager.EmailCheckResult result = 
                com.scis.db.DataManager.checkEmailExists(normalizedEmail, excludeStudentId);
            if (result.exists) {
                throw new IllegalArgumentException(
                    "email address already exists: \"" + normalizedEmail + "\"");
            }
            return normalizedEmail;
        } catch (Exception e) {
            return normalizedEmail;
        }
    }

    public static String validateDepartment(String dept) {
        if (dept == null || dept.trim().isEmpty())
            throw new IllegalArgumentException("department must not be null or blank");
        String t = dept.trim();
        if (t.length() > 80)
            throw new IllegalArgumentException("department must be <= 80 characters, got " + t.length());
        if (t.matches("\\d+"))
            throw new IllegalArgumentException("department must not be purely numeric: \"" + t + "\"");
        return t;
    }

    public static int validateSemester(int sem) {
        if (sem < 1 || sem > 12)
            throw new IllegalArgumentException("semester must be between 1 and 12, got " + sem);
        return sem;
    }

    public static String validatePassword(String password) {
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("password must be at least 6 characters");
        return password;
    }

    public String[] getSubjects() {
        if (attendanceMap.isEmpty() && marksMap.isEmpty()) return new String[0];
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(attendanceMap.keySet()); keys.addAll(marksMap.keySet());
        return keys.toArray(new String[0]);
    }

    public void addSubject(String subject) {
        if (subject == null) return;
        String n = subject.trim(); if (n.isEmpty()) return;
        attendanceMap.computeIfAbsent(n, k -> new ArrayList<>());
        marksMap.computeIfAbsent(n, k -> new ArrayList<>());
    }

    public void removeSubject(String subject) {
        if (subject == null) return;
        String n = subject.trim(); if (n.isEmpty()) return;
        attendanceMap.remove(n); marksMap.remove(n);
    }

    public void addMarks(String subject, String testType, double marksObtained, double maxMarks) {
        if (subject == null || subject.trim().isEmpty()) return;
        String n = subject.trim();
        if (maxMarks <= 0 || marksObtained < 0) return;
        marksMap.computeIfAbsent(n, k -> new ArrayList<>())
                .add(new MarksRecord(n, testType, marksObtained, maxMarks));
    }

    public void addTask(StudentTask task) {
        if (tasks == null) tasks = new ArrayList<>();
        tasks.add(task);
    }

    public List<StudentTask> getTasks() {
        if (tasks == null) tasks = new ArrayList<>();
        return tasks;
    }

    public List<StudentTask> getUpcomingTasks() {
        if (tasks == null) tasks = new ArrayList<>();
        List<StudentTask> upcoming = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (StudentTask t : tasks) {
            if (!t.submitted && t.dueDate != null && !t.dueDate.isBefore(today)) upcoming.add(t);
        }
        upcoming.sort(Comparator.comparing(t -> t.dueDate));
        return upcoming;
    }

    public List<StudentTask> getOverdueTasks() {
        if (tasks == null) tasks = new ArrayList<>();
        List<StudentTask> overdue = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (StudentTask t : tasks) {
            if (!t.submitted && t.dueDate != null && t.dueDate.isBefore(today)) overdue.add(t);
        }
        return overdue;
    }

    public double getOverallAttendance() {
        int totalPresent = 0, totalClasses = 0;
        for (List<AttendanceRecord> records : attendanceMap.values())
            for (AttendanceRecord rec : records) {
                if (rec.isMedicalLeave()) continue;
                totalClasses++;
                if (rec.present) totalPresent++;
            }
        return totalClasses > 0 ? (totalPresent * 100.0 / totalClasses) : 0.0;
    }

    public double getSubjectAttendance(String subject) {
        List<AttendanceRecord> records = attendanceMap.get(subject);
        if (records == null || records.isEmpty()) return 0.0;
        int present = 0, total = 0;
        for (AttendanceRecord rec : records) {
            if (rec.isMedicalLeave()) continue;
            total++;
            if (rec.present) present++;
        }
        return total > 0 ? (present * 100.0 / total) : 0.0;
    }

    public int getApprovedMedicalLeaveCount(String subject) {
        if (medicalLeaves == null) return 0;
        int count = 0;
        for (MedicalLeave ml : medicalLeaves)
            if (ml.isApproved()
                    && (subject == null || subject.equals(ml.subject)
                        || "All Subjects".equals(ml.subject)))
                count++;
        return count;
    }

    public MedicalLeave getApprovedLeaveFor(String subject, LocalDate date) {
        if (medicalLeaves == null) return null;
        for (MedicalLeave ml : medicalLeaves)
            if (ml.isApproved() && ml.covers(date)
                    && (subject == null || subject.equals(ml.subject)
                        || "All Subjects".equals(ml.subject)))
                return ml;
        return null;
    }

    public List<MedicalLeave> getMedicalLeaves() {
        if (medicalLeaves == null) medicalLeaves = new ArrayList<>();
        return medicalLeaves;
    }

    public void addMedicalLeave(MedicalLeave leave) {
        getMedicalLeaves().add(leave);
    }

    public double getOverallPerformance() {
        double total = 0; int count = 0;
        for (List<MarksRecord> records : marksMap.values())
            for (MarksRecord rec : records) { total += (rec.marksObtained * 100.0 / rec.maxMarks); count++; }
        return count > 0 ? (total / count) : 0.0;
    }

    public double getSubjectPerformance(String subject) {
        List<MarksRecord> records = marksMap.get(subject);
        if (records == null || records.isEmpty()) return 0.0;
        double total = 0;
        for (MarksRecord rec : records) total += (rec.marksObtained * 100.0 / rec.maxMarks);
        return total / records.size();
    }

    public double[] getSubjectMarksForTests(String subject, String[] testTypes) {
        double[] marks = new double[testTypes.length];
        Arrays.fill(marks, Double.NaN);
        List<MarksRecord> records = marksMap.get(subject);
        if (records == null || records.isEmpty()) return marks;
        Map<String, Integer> idxByType = new HashMap<>();
        for (int i = 0; i < testTypes.length; i++) idxByType.put(normalizeTestType(testTypes[i]), i);
        for (MarksRecord rec : records) {
            if (rec == null || rec.testType == null) continue;
            Integer idx = idxByType.get(normalizeTestType(rec.testType));
            if (idx == null || rec.maxMarks <= 0) continue;
            marks[idx] = (rec.marksObtained * 100.0 / rec.maxMarks);
        }
        return marks;
    }

    private String normalizeTestType(String s) {
        if (s == null) return "";
        String t = s.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (t.equals("INTERNAL1") || t.equals("INTERNAL01") || t.equals("IA01")) return "IA1";
        if (t.equals("INTERNAL2") || t.equals("INTERNAL02") || t.equals("IA02")) return "IA2";
        return t;
    }

    public String getRiskLevel() {
        double a = getOverallAttendance(), p = getOverallPerformance();
        if (a < 65 || p < 50) return "HIGH";
        else if (a < 75 || p < 60) return "MEDIUM";
        else return "LOW";
    }

    public void addSampleData() {
        attendanceMap.computeIfAbsent("Mathematics", k -> new ArrayList<>()).add(new AttendanceRecord("Mathematics", LocalDate.now().minusDays(10), true));
        attendanceMap.computeIfAbsent("Mathematics", k -> new ArrayList<>()).add(new AttendanceRecord("Mathematics", LocalDate.now().minusDays(7), true));
        attendanceMap.computeIfAbsent("Mathematics", k -> new ArrayList<>()).add(new AttendanceRecord("Mathematics", LocalDate.now().minusDays(5), false));
        attendanceMap.computeIfAbsent("Mathematics", k -> new ArrayList<>()).add(new AttendanceRecord("Mathematics", LocalDate.now().minusDays(3), true));
        attendanceMap.computeIfAbsent("Mathematics", k -> new ArrayList<>()).add(new AttendanceRecord("Mathematics", LocalDate.now().minusDays(1), true));
        attendanceMap.computeIfAbsent("Physics", k -> new ArrayList<>()).add(new AttendanceRecord("Physics", LocalDate.now().minusDays(8), true));
        attendanceMap.computeIfAbsent("Physics", k -> new ArrayList<>()).add(new AttendanceRecord("Physics", LocalDate.now().minusDays(6), false));
        attendanceMap.computeIfAbsent("Physics", k -> new ArrayList<>()).add(new AttendanceRecord("Physics", LocalDate.now().minusDays(4), true));
        attendanceMap.computeIfAbsent("Physics", k -> new ArrayList<>()).add(new AttendanceRecord("Physics", LocalDate.now().minusDays(2), false));
        marksMap.computeIfAbsent("Mathematics", k -> new ArrayList<>()).add(new MarksRecord("Mathematics", "IA1", 85, 100));
        marksMap.computeIfAbsent("Mathematics", k -> new ArrayList<>()).add(new MarksRecord("Mathematics", "IA2", 78, 100));
        marksMap.computeIfAbsent("Mathematics", k -> new ArrayList<>()).add(new MarksRecord("Mathematics", "Assignment", 92, 100));
        marksMap.computeIfAbsent("Physics", k -> new ArrayList<>()).add(new MarksRecord("Physics", "IA1", 65, 100));
        marksMap.computeIfAbsent("Physics", k -> new ArrayList<>()).add(new MarksRecord("Physics", "IA2", 58, 100));
        marksMap.computeIfAbsent("Physics", k -> new ArrayList<>()).add(new MarksRecord("Physics", "Quiz", 72, 100));
        classAverageMarks.put("Mathematics", 75.0);
        classAverageMarks.put("Physics", 70.0);
    }

    public List<String> getAlerts() {
        List<String> alerts = new ArrayList<>();
        if (tasks != null) {
            LocalDate today = LocalDate.now();
            for (StudentTask t : tasks) {
                if (!t.submitted && t.dueDate != null) {
                    long days = today.until(t.dueDate, java.time.temporal.ChronoUnit.DAYS);
                    if (days < 0)
                        alerts.add("[OVERDUE] " + t.taskType + ": " + t.title + " (was due " + t.dueDate + ")");
                    else if (days <= 2)
                        alerts.add("[DUE SOON] " + t.taskType + ": " + t.title + " - due in " + days + " day(s)!");
                }
            }
        }
        for (String subject : getSubjects()) {
            double subAtt = getSubjectAttendance(subject);
            if (subAtt < 75 && subAtt > 0) {
                int classes = attendanceMap.get(subject).size();
                if (subAtt < 70)
                    alerts.add("[LOW ATT] " + subject + ": " + String.format("%.1f%%", subAtt) +
                            " - Need " + calculateClassesNeeded(subAtt, classes, 75) + " consecutive classes");
                else
                    alerts.add("[CAUTION] " + subject + ": " + String.format("%.1f%%", subAtt) + " - Close to danger zone!");
            }
            List<AttendanceRecord> records = attendanceMap.get(subject);
            int streak = 0;
            for (int i = records.size() - 1; i >= 0 && i >= records.size() - 6; i--) {
                if (!records.get(i).present) streak++; else break;
            }
            if (streak >= 3) alerts.add("[STREAK] " + subject + ": Absent " + streak + " consecutive days!");
            List<MarksRecord> marks = marksMap.get(subject);
            if (!marks.isEmpty()) {
                double avgMarks = 0;
                for (MarksRecord m : marks) avgMarks += (m.marksObtained * 100.0 / m.maxMarks);
                avgMarks /= marks.size();
                if (avgMarks < 50)
                    alerts.add("[LOW MARKS] " + subject + ": Avg " + String.format("%.1f%%", avgMarks) + " - Below passing!");
            }
        }
        if (alerts.isEmpty()) alerts.add("[ALL GOOD] No critical alerts! Keep up the good work.");
        return alerts;
    }

    private int calculateClassesNeeded(double currentAtt, int totalClasses, double targetAtt) {
        int present = (int)((currentAtt / 100.0) * totalClasses);
        for (int future = 1; future <= 20; future++) {
            double newAtt = ((present + future) * 100.0) / (totalClasses + future);
            if (newAtt >= targetAtt) return future;
        }
        return 20;
    }

    public String getPassFailStatus() {
        if (getOverallAttendance() >= 75 && getOverallPerformance() >= 60) return "PASS";
        else return "FAIL";
    }

    public String getSubjectPassFailStatus(String subject) {
        if (getSubjectAttendance(subject) >= 75 && getSubjectPerformance(subject) >= 60) return "PASS";
        else return "FAIL";
    }

    public void enrollInSubject(String subject) {
        if (enrolledSubjects == null) enrolledSubjects = new ArrayList<>();
        if (!enrolledSubjects.contains(subject)) enrolledSubjects.add(subject);
    }

    public List<String> getEnrolledSubjects() {
        if (enrolledSubjects == null) enrolledSubjects = new ArrayList<>();
        return enrolledSubjects;
    }

    public List<String> getRecommendations() {
        List<String> recommendations = new ArrayList<>();
        if (getRiskLevel().equals("HIGH"))
            recommendations.add("URGENT: Your attendance/performance is critical. Immediate action required!");
        for (String subject : getSubjects()) {
            double att = getSubjectAttendance(subject);
            if (att < 75 && att > 0)
                recommendations.add(subject + ": Attend next " +
                        calculateClassesNeeded(att, attendanceMap.get(subject).size(), 75) + " classes to reach 75%");
            List<MarksRecord> marks = marksMap.get(subject);
            if (!marks.isEmpty()) {
                double avg = 0;
                for (MarksRecord m : marks) avg += (m.marksObtained * 100.0 / m.maxMarks);
                avg /= marks.size();
                if (avg < 60)
                    recommendations.add(subject + ": Study " + (int)(8 - avg/10) + " hours/week to improve");
            }
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Great job! Continue maintaining your performance.");
            recommendations.add("Consider exploring advanced topics in your subjects.");
        }
        return recommendations;
    }
}
