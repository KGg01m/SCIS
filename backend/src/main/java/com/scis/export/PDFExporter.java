// PDF Exporter - generates student performance reports as PDF files
package com.scis.export;

import com.scis.model.*;
import com.scis.ml.MLPredictor;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class PDFExporter {

    private static final int PAGE_WIDTH  = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN_LEFT = 50;
    private static final int MARGIN_TOP  = 60;
    private static final int LINE_H      = 18;
    private static final int COL_W       = PAGE_WIDTH - MARGIN_LEFT * 2;

    public static void exportReport(Student student, String outputPath) throws Exception {
        writePDF(student, outputPath);
    }

    private static void writePDF(Student student, String path) throws IOException {
        double att    = student.getOverallAttendance();
        double perf   = student.getOverallPerformance();
        double cgpa   = MLPredictor.predictCGPA(att, perf);
        double fail   = MLPredictor.calculateFailRisk(att, perf);
        double attDrop= MLPredictor.calculateAttendanceDropRisk(student);
        double conf   = MLPredictor.calculateConfidence(student);
        String grade  = MLPredictor.predictGradeCategory(perf, att);
        String risk   = student.getRiskLevel();
        String status = student.getPassFailStatus();
        String date   = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        List<String[]> lines = new ArrayList<>();

        lines.add(new String[]{"SMART CAMPUS INTELLIGENCE SYSTEM", "h1"});
        lines.add(new String[]{"Student Performance Report", "h2"});
        lines.add(new String[]{"Generated: " + date, "sub"});
        lines.add(new String[]{"", "sp"});
        lines.add(new String[]{"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "sep"});
        lines.add(new String[]{"STUDENT PROFILE", "h2"});
        lines.add(new String[]{"Name        : " + student.name, "body"});
        lines.add(new String[]{"Student ID  : " + student.studentId, "body"});
        lines.add(new String[]{"Email       : " + student.email, "body"});
        lines.add(new String[]{"Department  : " + student.department, "body"});
        lines.add(new String[]{"Semester    : " + student.semester, "body"});
        lines.add(new String[]{"College     : " + student.collegeName, "body"});
        lines.add(new String[]{"", "sp"});
        lines.add(new String[]{"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "sep"});
        lines.add(new String[]{"OVERALL STATISTICS", "h2"});
        lines.add(new String[]{String.format("Overall Attendance    : %.1f%%", att), "body"});
        lines.add(new String[]{String.format("Overall Performance   : %.1f%%", perf), "body"});
        lines.add(new String[]{"Risk Level            : " + risk, "body"});
        lines.add(new String[]{"Pass/Fail Status      : " + status, "body"});
        lines.add(new String[]{"", "sp"});
        lines.add(new String[]{"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "sep"});
        lines.add(new String[]{"ML PREDICTIONS", "h2"});
        lines.add(new String[]{String.format("Predicted CGPA        : %.2f / 10.0", cgpa), "body"});
        lines.add(new String[]{"Predicted Grade       : " + grade, "body"});
        lines.add(new String[]{String.format("Fail Risk             : %.0f%%  %s", fail*100, MLPredictor.getRiskIndicator(fail)), "body"});
        lines.add(new String[]{String.format("Attendance Drop Risk  : %.0f%%", attDrop*100), "body"});
        lines.add(new String[]{String.format("Prediction Confidence : %.0f%%", conf), "body"});
        lines.add(new String[]{"", "sp"});
        lines.add(new String[]{"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "sep"});
        lines.add(new String[]{"SUBJECT-WISE BREAKDOWN", "h2"});

        for (String subject : student.getSubjects()) {
            double sa = student.getSubjectAttendance(subject);
            double sp = student.getSubjectPerformance(subject);
            double sr = MLPredictor.calculateFailRisk(sa, sp);
            String sg = MLPredictor.predictGradeCategory(sp, sa);
            String ss = student.getSubjectPassFailStatus(subject);
            lines.add(new String[]{subject, "h3"});
            lines.add(new String[]{String.format("  Attendance  : %.1f%%  %s", sa, sa<75?"[LOW - BELOW 75%]":"[OK]"), "body"});
            lines.add(new String[]{String.format("  Performance : %.1f%%  Grade: %s  Status: %s", sp, sg, ss), "body"});
            lines.add(new String[]{String.format("  Fail Risk   : %.0f%%  %s", sr*100, MLPredictor.getRiskIndicator(sr)), "body"});

            List<MarksRecord> marks = student.marksMap.get(subject);
            if (marks != null && !marks.isEmpty()) {
                lines.add(new String[]{"  Test Records:", "body"});
                for (MarksRecord m : marks)
                    lines.add(new String[]{String.format("    %-14s: %.1f/%.0f (%.0f%%)", m.testType, m.marksObtained, m.maxMarks, m.marksObtained*100/m.maxMarks), "body"});
            }
            lines.add(new String[]{"", "sp"});
        }

        lines.add(new String[]{"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "sep"});
        lines.add(new String[]{"ALERTS", "h2"});
        for (String alert : student.getAlerts())
            lines.add(new String[]{"• " + alert, "body"});

        lines.add(new String[]{"", "sp"});
        lines.add(new String[]{"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "sep"});
        lines.add(new String[]{"RECOMMENDATIONS", "h2"});
        List<String> recs = student.getRecommendations();
        for (int i = 0; i < recs.size(); i++)
            lines.add(new String[]{(i+1) + ". " + recs.get(i), "body"});

        lines.add(new String[]{"", "sp"});
        lines.add(new String[]{"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "sep"});
        lines.add(new String[]{"End of Report — Smart Campus Intelligence System", "sub"});

        renderToPDF(lines, path);
    }

    private static void renderToPDF(List<String[]> contentLines, String outputPath) throws IOException {
        ByteArrayOutputStream pageContent = new ByteArrayOutputStream();
        PrintWriter pc = new PrintWriter(pageContent);

        float y = PAGE_HEIGHT - MARGIN_TOP;

        pc.println("BT");
        pc.println("1 0 0 1 " + MARGIN_LEFT + " " + y + " Tm");

        List<Integer> pageBreaks = new ArrayList<>();
        pageBreaks.add(0);

        for (String[] entry : contentLines) {
            String text  = entry[0];
            String type  = entry[1];

            float fontSize = 10;
            String fontKey = "F1";
            float leading = LINE_H;

            switch (type) {
                case "h1":  fontSize = 18; fontKey = "F2"; leading = 28; break;
                case "h2":  fontSize = 13; fontKey = "F2"; leading = 22; break;
                case "h3":  fontSize = 11; fontKey = "F2"; leading = 18; break;
                case "sub": fontSize = 9;  fontKey = "F1"; leading = 14; break;
                case "sep": fontSize = 7;  fontKey = "F1"; leading = 12; break;
                case "sp":  leading = 8;   pc.printf("0 -%.1f TD%n", leading); continue;
                default:    fontSize = 10; fontKey = "F1"; leading = 16; break;
            }

            pc.println("/" + fontKey + " " + (int)fontSize + " Tf");
            String safe = text.replace("\\","\\\\").replace("(","\\(").replace(")","\\)")
                              .replaceAll("[^\u0000-\u007E]","?");
            pc.println("(" + safe + ") Tj");
            pc.printf("0 -%.1f TD%n", leading);
        }

        pc.println("ET");
        pc.flush();

        byte[] pageBytes = pageContent.toByteArray();

        List<Integer> offsets = new ArrayList<>();
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(pdf);

        pw.print("%PDF-1.4\n");
        pw.flush();

        offsets.add(pdf.size());
        pw.print("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        pw.flush();

        offsets.add(pdf.size());
        pw.print("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        pw.flush();

        offsets.add(pdf.size());
        pw.print("3 0 obj\n<< /Type /Page /Parent 2 0 R\n");
        pw.print("   /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "]\n");
        pw.print("   /Contents 4 0 R\n");
        pw.print("   /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >>\n");
        pw.print(">>\nendobj\n");
        pw.flush();

        offsets.add(pdf.size());
        pw.print("4 0 obj\n<< /Length " + pageBytes.length + ">>\nstream\n");
        pw.flush();
        pdf.write(pageBytes);
        pw = new PrintWriter(pdf);
        pw.print("\nendstream\nendobj\n");
        pw.flush();

        offsets.add(pdf.size());
        pw.print("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");
        pw.flush();

        offsets.add(pdf.size());
        pw.print("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");
        pw.flush();

        int xrefOffset = pdf.size();
        pw.print("xref\n0 7\n");
        pw.print("0000000000 65535 f \n");
        for (int off : offsets)
            pw.printf("%010d 00000 n \n", off);
        pw.flush();

        pw.print("trailer\n<< /Size 7 /Root 1 0 R >>\n");
        pw.print("startxref\n" + xrefOffset + "\n%%EOF\n");
        pw.flush();

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(pdf.toByteArray());
        }
    }
}
