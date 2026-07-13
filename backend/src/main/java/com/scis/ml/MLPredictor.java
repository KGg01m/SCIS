// ML Predictor - calculates CGPA, fail risk, grades, and predictions for students
package com.scis.ml;

import com.scis.model.*;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MLPredictor {

    public static final double GRADE_A_THRESHOLD = 80.0;
    public static final double GRADE_B_THRESHOLD = 65.0;
    public static final double GRADE_C_THRESHOLD = 50.0;
    public static final double GRADE_D_THRESHOLD = 40.0;

    public static final double ATT_DANGER = 65.0;
    public static final double ATT_CAUTION = 75.0;
    public static final double PERF_DANGER = 50.0;
    public static final double PERF_CAUTION = 60.0;

    public static double predictCGPA(double attendance, double performance) {
        double base = (performance * 0.7 + attendance * 0.3) / 10.0;
        if (attendance < ATT_DANGER) base *= 0.88;
        else if (attendance < ATT_CAUTION) base *= 0.95;
        if (performance < PERF_DANGER) base *= 0.82;
        else if (performance < PERF_CAUTION) base *= 0.92;
        return Math.max(0, Math.min(10.0, Math.round(base * 100.0) / 100.0));
    }

    public static double calculateFailRisk(double attendance, double performance) {
        double risk = 0.0;
        if (attendance < 55) risk += 0.45;
        else if (attendance < ATT_DANGER) risk += 0.30;
        else if (attendance < ATT_CAUTION) risk += 0.15;
        if (performance < 35) risk += 0.55;
        else if (performance < PERF_DANGER) risk += 0.38;
        else if (performance < PERF_CAUTION) risk += 0.20;
        return Math.min(1.0, risk);
    }

    public static double calculateAttendanceDropRisk(Student student) {
        double risk = 0.0;
        for (String subject : student.getSubjects()) {
            List<AttendanceRecord> records = student.attendanceMap.get(subject);
            if (records == null || records.size() < 3) continue;
            int recentAbsences = 0, recentTotal = Math.min(5, records.size());
            for (int i = records.size() - 1; i >= records.size() - recentTotal; i--)
                if (!records.get(i).present) recentAbsences++;
            double recentAbsenceRate = (double) recentAbsences / recentTotal;
            double currentAtt = student.getSubjectAttendance(subject);
            double subjectRisk = 0;
            if (currentAtt < ATT_CAUTION) subjectRisk += 0.4;
            subjectRisk += recentAbsenceRate * 0.6;
            risk = Math.max(risk, subjectRisk);
        }
        return Math.min(1.0, risk);
    }

    public static double calculateConfidence(Student student) {
        int dataPoints = 0;
        for (List<MarksRecord> m : student.marksMap.values()) dataPoints += m.size();
        for (List<AttendanceRecord> a : student.attendanceMap.values()) dataPoints += a.size();
        return Math.min(95.0, 45.0 + (dataPoints * 2.5));
    }

    public static String predictGradeCategory(double performance, double attendance) {
        double effectivePerf = performance;
        if (attendance < ATT_DANGER) effectivePerf *= 0.85;
        else if (attendance < ATT_CAUTION) effectivePerf *= 0.93;
        if (effectivePerf >= GRADE_A_THRESHOLD) return "A";
        if (effectivePerf >= GRADE_B_THRESHOLD) return "B";
        if (effectivePerf >= GRADE_C_THRESHOLD) return "C";
        if (effectivePerf >= GRADE_D_THRESHOLD) return "D";
        return "F";
    }

    public static String getRiskLevel(double failRisk) {
        if (failRisk > 0.6) return "HIGH";
        if (failRisk >= 0.3) return "MEDIUM";
        return "LOW";
    }

    public static String getRiskIndicator(double failRisk) {
        if (failRisk > 0.6) return "[HIGH RISK]";
        if (failRisk > 0.3) return "[MODERATE RISK]";
        return "[SAFE]";
    }

    public static double predictFinalMarks(double currentPerformance) {
        double predicted = currentPerformance * 1.08;
        return Math.min(100.0, Math.round(predicted * 10.0) / 10.0);
    }

    public static String predict(Student student) {
        return predictSemesterOutcome(student);
    }

    public static String predictSemesterOutcome(Student student) {
        double attendance = student.getOverallAttendance();
        double performance = student.getOverallPerformance();
        double cgpa = predictCGPA(attendance, performance);
        double failRisk = calculateFailRisk(attendance, performance);
        double attDropRisk = calculateAttendanceDropRisk(student);
        double finalMarks = predictFinalMarks(performance);
        double confidence = calculateConfidence(student);
        String grade = predictGradeCategory(performance, attendance);
        String riskLevel = getRiskLevel(failRisk);
        String riskIndicator = getRiskIndicator(failRisk);

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════╗\n");
        sb.append("║    ML PREDICTIONS — Smart Campus Analytics   ║\n");
        sb.append("╚══════════════════════════════════════════════╝\n\n");

        sb.append("┌─ CURRENT STATISTICS ─────────────────────────\n");
        sb.append(String.format("│  Overall Attendance : %.1f%%\n", attendance));
        sb.append(String.format("│  Overall Performance: %.1f%%\n", performance));
        sb.append(String.format("│  Risk Level         : %s\n", riskLevel));
        sb.append("└──────────────────────────────────────────────\n\n");

        sb.append("┌─ PREDICTIONS ────────────────────────────────\n");
        sb.append(String.format("│  Predicted CGPA     : %.2f / 10.0\n", cgpa));
        sb.append(String.format("│  Predicted Grade    : %s\n", grade));
        sb.append(String.format("│  Predicted Finals   : %.1f%%\n", finalMarks));
        sb.append(String.format("│  Fail Risk          : %.0f%%  %s\n", failRisk * 100, riskIndicator));
        sb.append(String.format("│  Att. Drop Risk     : %.0f%%\n", attDropRisk * 100));
        sb.append(String.format("│  Prediction Confidence: %.0f%%\n", confidence));
        sb.append("└──────────────────────────────────────────────\n\n");

        sb.append("┌─ GRADE SCALE ────────────────────────────────\n");
        sb.append("│  A = 80%+   B = 65-79%   C = 50-64%\n");
        sb.append("│  D = 40-49%  F = Below 40%\n");
        sb.append("└──────────────────────────────────────────────\n\n");

        sb.append("┌─ SUBJECT ANALYSIS ───────────────────────────\n");
        for (String subject : student.getSubjects()) {
            double sa = student.getSubjectAttendance(subject);
            double sp = student.getSubjectPerformance(subject);
            double sr = calculateFailRisk(sa, sp);
            String sg = predictGradeCategory(sp, sa);
            int mlCount = student.getApprovedMedicalLeaveCount(subject);
            sb.append(String.format("│  %s:\n", subject));
            sb.append(String.format("│    Att: %.1f%%  Perf: %.1f%%  Grade: %s  Risk: %.0f%%\n",
                sa, sp, sg, sr * 100));
            if (mlCount > 0)
                sb.append(String.format("│    Medical Leaves (approved): %d — excluded from att.\n", mlCount));
        }
        sb.append("└──────────────────────────────────────────────\n\n");

        sb.append("┌─ RECOMMENDATIONS ────────────────────────────\n");
        if (failRisk > 0.6) {
            sb.append("│  URGENT: Immediate academic intervention needed!\n");
            sb.append("│  > Contact academic advisor immediately\n");
            sb.append("│  > Attend all remaining classes without fail\n");
        } else if (failRisk > 0.3) {
            sb.append("│  MODERATE RISK: Improvement needed\n");
            sb.append("│  > Focus on weak subjects\n");
            sb.append("│  > Improve attendance consistency\n");
        } else {
            sb.append("│  LOW RISK: You are on track!\n");
            sb.append("│  > Maintain your performance levels\n");
            sb.append("│  > Explore advanced topics\n");
        }
        if (attDropRisk > 0.5) {
            sb.append("│  WARNING: High attendance drop risk detected!\n");
            sb.append("│  > Recent absences are increasing — attend now\n");
        }
        sb.append("└──────────────────────────────────────────────\n\n");

        sb.append(String.format("Generated: %s | Confidence: %.0f%%\n",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), confidence));
        return sb.toString();
    }

    public static String generatePDFReport(Student student) {
        double attendance = student.getOverallAttendance();
        double performance = student.getOverallPerformance();
        double failRisk = calculateFailRisk(attendance, performance);
        double confidence = calculateConfidence(student);
        String grade = predictGradeCategory(performance, attendance);

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("         STUDENT PERFORMANCE REPORT\n");
        sb.append("        Smart Campus Intelligence System\n");
        sb.append("═══════════════════════════════════════════════════\n\n");

        sb.append("STUDENT PROFILE\n");
        sb.append("───────────────────────────────────────────────────\n");
        sb.append("  Student ID  : ").append(student.studentId).append("\n");
        sb.append("  Name        : ").append(student.name).append("\n");
        sb.append("  Email       : ").append(student.email).append("\n");
        sb.append("  Department  : ").append(student.department).append("\n");
        sb.append("  Semester    : ").append(student.semester).append("\n");
        sb.append("  College     : ").append(student.collegeName).append("\n");
        sb.append("  Report Date : ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))).append("\n\n");

        sb.append("OVERALL STATISTICS\n");
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  Overall Attendance    : %.1f%%\n", attendance));
        sb.append(String.format("  Overall Performance   : %.1f%%\n", performance));
        sb.append(String.format("  Predicted Grade       : %s\n", grade));
        sb.append(String.format("  Risk Level            : %s\n", student.getRiskLevel()));
        sb.append(String.format("  Pass/Fail Status      : %s\n\n", student.getPassFailStatus()));

        sb.append("ML PREDICTIONS\n");
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  Predicted CGPA        : %.2f/10.0\n", predictCGPA(attendance, performance)));
        sb.append(String.format("  Fail Risk             : %.0f%% %s\n", failRisk*100, getRiskIndicator(failRisk)));
        sb.append(String.format("  Attendance Drop Risk  : %.0f%%\n", calculateAttendanceDropRisk(student)*100));
        sb.append(String.format("  Prediction Confidence : %.0f%%\n\n", confidence));

        sb.append("SUBJECT-WISE BREAKDOWN\n");
        sb.append("───────────────────────────────────────────────────\n");
        for (String subject : student.getSubjects()) {
            double sa = student.getSubjectAttendance(subject);
            double sp = student.getSubjectPerformance(subject);
            String sg = predictGradeCategory(sp, sa);
            String ss = student.getSubjectPassFailStatus(subject);
            int mlC = student.getApprovedMedicalLeaveCount(subject);
            sb.append(String.format("  %s\n", subject));
            sb.append(String.format("    Attendance  : %.1f%%  %s", sa, sa<75?"[LOW]":"[OK]"));
            if (mlC > 0) sb.append(String.format("  (Med. leave excl.: %d)", mlC));
            sb.append("\n");
            sb.append(String.format("    Performance : %.1f%%  Grade: %s  %s\n\n", sp, sg, ss));
            List<MarksRecord> marks = student.marksMap.get(subject);
            if (marks != null && !marks.isEmpty()) {
                sb.append("    Test Records:\n");
                for (MarksRecord m : marks)
                    sb.append(String.format("      %-14s: %.1f/%.1f (%.1f%%)\n",
                        m.testType, m.marksObtained, m.maxMarks, m.marksObtained*100.0/m.maxMarks));
                sb.append("\n");
            }
        }

        sb.append("ALERTS\n");
        sb.append("───────────────────────────────────────────────────\n");
        for (String alert : student.getAlerts())
            sb.append("  • ").append(alert).append("\n");

        sb.append("\nRECOMMENDATIONS\n");
        sb.append("───────────────────────────────────────────────────\n");
        List<String> recs = student.getRecommendations();
        for (int i = 0; i < recs.size(); i++)
            sb.append("  ").append(i+1).append(". ").append(recs.get(i)).append("\n");

        sb.append("\n═══════════════════════════════════════════════════\n");
        sb.append("           End of Report\n");
        sb.append("═══════════════════════════════════════════════════\n");
        return sb.toString();
    }
}
