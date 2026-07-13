package com.scis.export;

import com.scis.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVExporter
 * Tests: exportAttendance, exportMarks, exportPerformanceReport, buildPath
 */
@DisplayName("CSVExporter Tests")
class CSVExporterTest {

    @TempDir
    Path tempDir;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student("S001", "Alice Kumar", "alice@test.com",
                "Computer Science", 3, "pass123", "Test University");
        student.addSampleData();
    }

    // ─── exportAttendance ─────────────────────────────────────────────────

    @Test
    @DisplayName("exportAttendance creates file at given path")
    void testExportAttendanceCreatesFile() throws IOException {
        String path = tempDir.resolve("attendance.csv").toString();
        CSVExporter.exportAttendance(student, path);
        assertTrue(new File(path).exists());
    }

    @Test
    @DisplayName("exportAttendance CSV contains header row")
    void testExportAttendanceHeader() throws IOException {
        String path = tempDir.resolve("attendance.csv").toString();
        CSVExporter.exportAttendance(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("Student ID"), "Should have 'Student ID' header");
        assertTrue(content.contains("Subject"), "Should have 'Subject' header");
        assertTrue(content.contains("Status"), "Should have 'Status' header");
    }

    @Test
    @DisplayName("exportAttendance CSV contains student data rows")
    void testExportAttendanceDataRows() throws IOException {
        String path = tempDir.resolve("attendance.csv").toString();
        CSVExporter.exportAttendance(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("S001"), "Should contain student ID");
        assertTrue(content.contains("Present") || content.contains("Absent"),
                "Should contain presence status");
    }

    @Test
    @DisplayName("exportAttendance CSV contains subject names")
    void testExportAttendanceContainsSubjects() throws IOException {
        String path = tempDir.resolve("attendance.csv").toString();
        CSVExporter.exportAttendance(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("Mathematics") || content.contains("Physics"),
                "Should contain at least one subject");
    }

    @Test
    @DisplayName("exportAttendance returns the same file path provided")
    void testExportAttendanceReturnPath() throws IOException {
        String path = tempDir.resolve("att_out.csv").toString();
        String returned = CSVExporter.exportAttendance(student, path);
        assertEquals(path, returned);
    }

    @Test
    @DisplayName("exportAttendance produces empty content (header only) for student with no attendance")
    void testExportAttendanceNoData() throws IOException {
        Student empty = new Student("S002", "Empty", "e@test.com", "IT", 1, "password", "U");
        String path = tempDir.resolve("empty_att.csv").toString();
        CSVExporter.exportAttendance(empty, path);
        String content = Files.readString(Path.of(path));
        // Should have at least the header line
        assertTrue(content.contains("Student ID"));
    }

    // ─── exportMarks ──────────────────────────────────────────────────────

    @Test
    @DisplayName("exportMarks creates file at given path")
    void testExportMarksCreatesFile() throws IOException {
        String path = tempDir.resolve("marks.csv").toString();
        CSVExporter.exportMarks(student, path);
        assertTrue(new File(path).exists());
    }

    @Test
    @DisplayName("exportMarks CSV contains header row")
    void testExportMarksHeader() throws IOException {
        String path = tempDir.resolve("marks.csv").toString();
        CSVExporter.exportMarks(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("Test Type"), "Should have 'Test Type' header");
        assertTrue(content.contains("Marks Obtained"), "Should have 'Marks Obtained' header");
        assertTrue(content.contains("Percentage"), "Should have 'Percentage' header");
    }

    @Test
    @DisplayName("exportMarks CSV contains student ID and marks data")
    void testExportMarksDataRows() throws IOException {
        String path = tempDir.resolve("marks.csv").toString();
        CSVExporter.exportMarks(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("S001"));
        // Sample data has IA1, IA2, etc.
        assertTrue(content.contains("IA1") || content.contains("Assignment") || content.contains("Quiz"),
                "Should contain test type records");
    }

    @Test
    @DisplayName("exportMarks CSV contains PASS/FAIL status")
    void testExportMarksContainsStatus() throws IOException {
        String path = tempDir.resolve("marks.csv").toString();
        CSVExporter.exportMarks(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("PASS") || content.contains("FAIL"),
                "Should contain pass/fail status for each record");
    }

    @Test
    @DisplayName("exportMarks returns the same file path provided")
    void testExportMarksReturnPath() throws IOException {
        String path = tempDir.resolve("m_out.csv").toString();
        String returned = CSVExporter.exportMarks(student, path);
        assertEquals(path, returned);
    }

    // ─── exportPerformanceReport ──────────────────────────────────────────

    @Test
    @DisplayName("exportPerformanceReport creates file at given path")
    void testExportPerformanceCreatesFile() throws IOException {
        String path = tempDir.resolve("report.csv").toString();
        CSVExporter.exportPerformanceReport(student, path);
        assertTrue(new File(path).exists());
    }

    @Test
    @DisplayName("exportPerformanceReport contains STUDENT SUMMARY section")
    void testExportPerformanceSummarySection() throws IOException {
        String path = tempDir.resolve("report.csv").toString();
        CSVExporter.exportPerformanceReport(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("STUDENT SUMMARY"));
        assertTrue(content.contains("Student ID"));
        assertTrue(content.contains("S001"));
    }

    @Test
    @DisplayName("exportPerformanceReport contains ML PREDICTIONS section")
    void testExportPerformanceMLSection() throws IOException {
        String path = tempDir.resolve("report.csv").toString();
        CSVExporter.exportPerformanceReport(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("Predicted CGPA"), "Should contain ML CGPA prediction");
        assertTrue(content.contains("Fail Risk"), "Should contain fail risk data");
    }

    @Test
    @DisplayName("exportPerformanceReport contains SUBJECT BREAKDOWN section")
    void testExportPerformanceSubjectSection() throws IOException {
        String path = tempDir.resolve("report.csv").toString();
        CSVExporter.exportPerformanceReport(student, path);
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("SUBJECT BREAKDOWN"), "Should contain subject breakdown");
        assertTrue(content.contains("Attendance %"), "Should contain attendance column");
    }

    @Test
    @DisplayName("exportPerformanceReport returns the same file path provided")
    void testExportPerformanceReturnPath() throws IOException {
        String path = tempDir.resolve("perf_out.csv").toString();
        String returned = CSVExporter.exportPerformanceReport(student, path);
        assertEquals(path, returned);
    }

    // ─── buildPath ────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildPath starts with 'data/' directory prefix")
    void testBuildPathPrefix() {
        String path = CSVExporter.buildPath("attendance", "csv");
        assertTrue(path.startsWith("data/"));
    }

    @Test
    @DisplayName("buildPath ends with the correct extension")
    void testBuildPathExtension() {
        String path = CSVExporter.buildPath("marks", "csv");
        assertTrue(path.endsWith(".csv"));
    }

    @Test
    @DisplayName("buildPath includes the given prefix")
    void testBuildPathIncludesPrefix() {
        String path = CSVExporter.buildPath("my_report", "csv");
        assertTrue(path.contains("my_report"));
    }

    @Test
    @DisplayName("buildPath includes today's date")
    void testBuildPathIncludesTodaysDate() {
        String path = CSVExporter.buildPath("test", "csv");
        String today = LocalDate.now().toString();
        assertTrue(path.contains(today), "Path should contain today's date: " + today);
    }
}
