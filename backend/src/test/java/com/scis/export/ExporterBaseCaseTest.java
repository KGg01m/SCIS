package com.scis.export;

import com.scis.model.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base-case tests for CSVExporter and PDFExporter.
 *
 * Each test cleans up its temp files via @AfterEach.
 */
@DisplayName("Exporter Base Case Tests")
class ExporterBaseCaseTest {

    private Student student;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("scis_export_base_");
        student = new Student("EXP001", "Priya Sharma", "priya@test.com",
                "Computer Science", 4, "pass12", "Test University");
        student.addSampleData();
        student.addTask(new StudentTask("Final Project", "Project",
                "Mathematics", LocalDate.now().plusDays(10), "Group project", 1));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .map(Path::toFile)
                      .forEach(File::delete);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CSVExporter — exportAttendance
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CSVExporter – exportAttendance")
    class CSVAttendanceTests {

        @Test @DisplayName("Creates a non-empty file")
        void createsNonEmptyFile() throws Exception {
            File out = tempDir.resolve("attendance.csv").toFile();
            CSVExporter.exportAttendance(student, out.getAbsolutePath());
            assertTrue(out.exists());
            assertTrue(out.length() > 0);
        }

        @Test @DisplayName("File contains a header row")
        void hasHeaderRow() throws Exception {
            File out = tempDir.resolve("att_header.csv").toFile();
            CSVExporter.exportAttendance(student, out.getAbsolutePath());
            List<String> lines = Files.readAllLines(out.toPath());
            assertFalse(lines.isEmpty(), "File must not be empty");
            // Header must mention subject and date
            String header = lines.get(0).toLowerCase();
            assertTrue(header.contains("subject") || header.contains("date"),
                "Header row missing expected column names: " + lines.get(0));
        }

        @Test @DisplayName("File contains actual subject data")
        void containsSubjectData() throws Exception {
            File out = tempDir.resolve("att_data.csv").toFile();
            CSVExporter.exportAttendance(student, out.getAbsolutePath());
            String content = Files.readString(out.toPath());
            assertTrue(content.contains("Mathematics") || content.contains("Physics"),
                "CSV should contain subject names from sample data");
        }

        @Test @DisplayName("File has at least header + 1 data row")
        void hasHeaderPlusDataRows() throws Exception {
            File out = tempDir.resolve("att_rows.csv").toFile();
            CSVExporter.exportAttendance(student, out.getAbsolutePath());
            List<String> lines = Files.readAllLines(out.toPath());
            assertTrue(lines.size() >= 2, "Expected header + at least one data row");
        }

        @Test @DisplayName("Student name (String) appears in the CSV")
        void studentNameInCSV() throws Exception {
            File out = tempDir.resolve("att_name.csv").toFile();
            CSVExporter.exportAttendance(student, out.getAbsolutePath());
            String content = Files.readString(out.toPath());
            assertTrue(content.contains(student.name), "CSV must contain student name string");
        }

        @Test @DisplayName("Student ID (String) appears in the CSV")
        void studentIdInCSV() throws Exception {
            File out = tempDir.resolve("att_id.csv").toFile();
            CSVExporter.exportAttendance(student, out.getAbsolutePath());
            String content = Files.readString(out.toPath());
            assertTrue(content.contains(student.studentId), "CSV must contain student ID string");
        }

        @Test @DisplayName("Export for student with no attendance creates header-only file")
        void emptyStudentCreatesFile() throws Exception {
            Student empty = new Student("E001", "Empty Student", "e@test.com", "CS", 1, "pass12", "Uni");
            File out = tempDir.resolve("empty_att.csv").toFile();
            CSVExporter.exportAttendance(empty, out.getAbsolutePath());
            assertTrue(out.exists());
        }

        @Test @DisplayName("Present/Absent text appears in exported CSV (not 1/0 or true/false)")
        void presentAbsentTextInCSV() throws Exception {
            File out = tempDir.resolve("att_present.csv").toFile();
            CSVExporter.exportAttendance(student, out.getAbsolutePath());
            String content = Files.readString(out.toPath());
            assertTrue(content.contains("Present") || content.contains("Absent"),
                "CSV should use 'Present'/'Absent' labels, not raw booleans");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CSVExporter — exportMarks
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CSVExporter – exportMarks")
    class CSVMarksTests {

        @Test @DisplayName("Creates a non-empty file")
        void createsNonEmptyFile() throws Exception {
            File out = tempDir.resolve("marks.csv").toFile();
            CSVExporter.exportMarks(student, out.getAbsolutePath());
            assertTrue(out.exists());
            assertTrue(out.length() > 0);
        }

        @Test @DisplayName("File contains test type data as String (e.g. IA1, Quiz)")
        void containsTestTypeAsString() throws Exception {
            File out = tempDir.resolve("marks_type.csv").toFile();
            CSVExporter.exportMarks(student, out.getAbsolutePath());
            String content = Files.readString(out.toPath());
            assertTrue(content.contains("IA1") || content.contains("Quiz") || content.contains("Assignment"),
                "Test type strings must appear in marks CSV");
        }

        @Test @DisplayName("File contains numeric marks (not blank)")
        void containsNumericMarks() throws Exception {
            File out = tempDir.resolve("marks_num.csv").toFile();
            CSVExporter.exportMarks(student, out.getAbsolutePath());
            String content = Files.readString(out.toPath());
            // Contains digits as mark values
            assertTrue(content.matches("(?s).*[0-9]+.*"), "CSV should contain numeric mark values");
        }

        @Test @DisplayName("Export for student with no marks creates file without crashing")
        void emptyStudentMarksNoException() throws Exception {
            Student empty = new Student("E002", "No Marks", "nm@test.com", "EE", 2, "pass12", "Uni");
            File out = tempDir.resolve("no_marks.csv").toFile();
            assertDoesNotThrow(() -> CSVExporter.exportMarks(empty, out.getAbsolutePath()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PDFExporter
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PDFExporter – exportReport")
    class PDFTests {

        @Test @DisplayName("Creates a non-empty PDF file")
        void createsNonEmptyPDF() throws Exception {
            File out = tempDir.resolve("report.pdf").toFile();
            PDFExporter.exportReport(student, out.getAbsolutePath());
            assertTrue(out.exists());
            assertTrue(out.length() > 0);
        }

        @Test @DisplayName("File begins with %%PDF magic bytes")
        void fileIsPdfFormat() throws Exception {
            File out = tempDir.resolve("report_fmt.pdf").toFile();
            PDFExporter.exportReport(student, out.getAbsolutePath());
            byte[] bytes = Files.readAllBytes(out.toPath());
            assertTrue(bytes.length >= 4, "PDF too small");
            assertEquals('%', (char) bytes[0]);
            assertEquals('P', (char) bytes[1]);
            assertEquals('D', (char) bytes[2]);
            assertEquals('F', (char) bytes[3]);
        }

        @Test @DisplayName("Student name (String) appears somewhere inside the PDF")
        void studentNameInPDF() throws Exception {
            File out = tempDir.resolve("report_name.pdf").toFile();
            PDFExporter.exportReport(student, out.getAbsolutePath());
            String raw = new String(Files.readAllBytes(out.toPath()));
            assertTrue(raw.contains(student.name),
                "PDF content must include the student name string: " + student.name);
        }

        @Test @DisplayName("Student ID (String) appears in the PDF")
        void studentIdInPDF() throws Exception {
            File out = tempDir.resolve("report_id.pdf").toFile();
            PDFExporter.exportReport(student, out.getAbsolutePath());
            String raw = new String(Files.readAllBytes(out.toPath()));
            assertTrue(raw.contains(student.studentId),
                "PDF content must include the student ID string: " + student.studentId);
        }

        @Test @DisplayName("Empty student export does not throw")
        void emptyStudentNoException() throws Exception {
            Student empty = new Student("E003", "Empty Student", "es@test.com", "ME", 3, "pass12", "Uni");
            File out = tempDir.resolve("empty_report.pdf").toFile();
            assertDoesNotThrow(() -> PDFExporter.exportReport(empty, out.getAbsolutePath()));
        }

        @Test @DisplayName("Multiple sequential exports do not conflict")
        void sequentialExportsDoNotConflict() throws Exception {
            File f1 = tempDir.resolve("r1.pdf").toFile();
            File f2 = tempDir.resolve("r2.pdf").toFile();
            PDFExporter.exportReport(student, f1.getAbsolutePath());
            PDFExporter.exportReport(student, f2.getAbsolutePath());
            assertTrue(f1.exists() && f2.exists());
        }

        @Test @DisplayName("Export with tasks included does not throw")
        void exportWithTasksNoException() throws Exception {
            student.addTask(new StudentTask("Midterm", "Exam", "Physics",
                    LocalDate.now().plusDays(3), "Study ch 1-5", 1));
            File out = tempDir.resolve("report_tasks.pdf").toFile();
            assertDoesNotThrow(() -> PDFExporter.exportReport(student, out.getAbsolutePath()));
        }
    }
}
