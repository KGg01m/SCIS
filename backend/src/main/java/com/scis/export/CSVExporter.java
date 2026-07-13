// CSV Exporter - exports student attendance and marks to CSV format
package com.scis.export;

import com.scis.model.*;
import com.scis.ml.MLPredictor;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CSVExporter {

    public static String exportAttendance(Student student, String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Student ID,Name,Subject,Date,Status,Subject Attendance %\n");
        for (String subject : student.getSubjects()) {
            List<AttendanceRecord> records = student.attendanceMap.get(subject);
            double subAtt = student.getSubjectAttendance(subject);
            if (records == null) continue;
            List<AttendanceRecord> sorted = new ArrayList<>(records);
            sorted.sort(Comparator.comparing(r -> r.date));
            for (AttendanceRecord rec : sorted) {
                sb.append(csvEscape(student.studentId)).append(",");
                sb.append(csvEscape(student.name)).append(",");
                sb.append(csvEscape(subject)).append(",");
                sb.append(rec.date.toString()).append(",");
                sb.append(rec.present ? "Present" : "Absent").append(",");
                sb.append(String.format("%.1f%%", subAtt)).append("\n");
            }
        }
        writeFile(filePath, sb.toString());
        return filePath;
    }

    public static String exportMarks(Student student, String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Student ID,Name,Subject,Test Type,Marks Obtained,Max Marks,Percentage,Status,Predicted Grade\n");
        for (String subject : student.getSubjects()) {
            List<MarksRecord> records = student.marksMap.get(subject);
            if (records == null || records.isEmpty()) continue;
            for (MarksRecord rec : records) {
                double pct = rec.marksObtained * 100.0 / rec.maxMarks;
                String status = pct >= 40 ? "PASS" : "FAIL";
                String grade = MLPredictor.predictGradeCategory(pct, student.getSubjectAttendance(subject));
                sb.append(csvEscape(student.studentId)).append(",");
                sb.append(csvEscape(student.name)).append(",");
                sb.append(csvEscape(subject)).append(",");
                sb.append(csvEscape(rec.testType)).append(",");
                sb.append(String.format("%.1f", rec.marksObtained)).append(",");
                sb.append(String.format("%.1f", rec.maxMarks)).append(",");
                sb.append(String.format("%.1f%%", pct)).append(",");
                sb.append(status).append(",");
                sb.append(grade).append("\n");
            }
        }
        writeFile(filePath, sb.toString());
        return filePath;
    }

    public static String exportPerformanceReport(Student student, String filePath) throws IOException {
        double att = student.getOverallAttendance();
        double perf = student.getOverallPerformance();
        double failRisk = MLPredictor.calculateFailRisk(att, perf);
        double attDropRisk = MLPredictor.calculateAttendanceDropRisk(student);
        double confidence = MLPredictor.calculateConfidence(student);

        StringBuilder sb = new StringBuilder();
        sb.append("SMART CAMPUS INTELLIGENCE SYSTEM - PERFORMANCE REPORT\n");
        sb.append(String.format("Generated: %s\n\n",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));

        sb.append("STUDENT SUMMARY\n");
        sb.append("Field,Value\n");
        sb.append("Student ID,").append(csvEscape(student.studentId)).append("\n");
        sb.append("Name,").append(csvEscape(student.name)).append("\n");
        sb.append("Department,").append(csvEscape(student.department)).append("\n");
        sb.append("Semester,").append(student.semester).append("\n");
        sb.append(String.format("Overall Attendance,%.1f%%\n", att));
        sb.append(String.format("Overall Performance,%.1f%%\n", perf));
        sb.append("Risk Level,").append(student.getRiskLevel()).append("\n");
        sb.append("Pass/Fail Status,").append(student.getPassFailStatus()).append("\n");
        sb.append(String.format("Predicted CGPA,%.2f\n", MLPredictor.predictCGPA(att, perf)));
        sb.append(String.format("Predicted Grade,%s\n", MLPredictor.predictGradeCategory(perf, att)));
        sb.append(String.format("Fail Risk,%.0f%%\n", failRisk * 100));
        sb.append(String.format("Attendance Drop Risk,%.0f%%\n", attDropRisk * 100));
        sb.append(String.format("Prediction Confidence,%.0f%%\n\n", confidence));

        sb.append("SUBJECT BREAKDOWN\n");
        sb.append("Subject,Attendance %,Performance %,Grade,Risk %,Status\n");
        for (String subject : student.getSubjects()) {
            double sa = student.getSubjectAttendance(subject);
            double sp = student.getSubjectPerformance(subject);
            double sr = MLPredictor.calculateFailRisk(sa, sp);
            String sg = MLPredictor.predictGradeCategory(sp, sa);
            String ss = student.getSubjectPassFailStatus(subject);
            sb.append(csvEscape(subject)).append(",");
            sb.append(String.format("%.1f%%", sa)).append(",");
            sb.append(String.format("%.1f%%", sp)).append(",");
            sb.append(sg).append(",");
            sb.append(String.format("%.0f%%", sr * 100)).append(",");
            sb.append(ss).append("\n");
        }

        writeFile(filePath, sb.toString());
        return filePath;
    }

    private static String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n"))
            return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }

    private static void writeFile(String filePath, String content) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.print(content);
        }
    }

    public static String buildPath(String prefix, String extension) {
        return "data/" + prefix + "_" + LocalDate.now().toString() + "." + extension;
    }
}
