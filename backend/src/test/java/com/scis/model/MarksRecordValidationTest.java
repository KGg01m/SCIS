package com.scis.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base-case validation tests for MarksRecord.
 *
 * Rules exercised:
 *  subject         – must be a non-empty String
 *  testType        – must be a non-empty String (e.g. "IA1", "Quiz")
 *  marksObtained   – double, must be >= 0 and <= maxMarks
 *  maxMarks        – double, must be > 0
 *  percentage      – derived from marksObtained / maxMarks * 100 (not stored)
 */
@DisplayName("MarksRecord Validation Tests")
class MarksRecordValidationTest {

    // ── subject ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("subject field")
    class SubjectTests {

        @Test @DisplayName("Subject is stored as String correctly")
        void subjectStoredAsString() {
            MarksRecord r = new MarksRecord("Mathematics", "IA1", 85, 100);
            assertEquals("Mathematics", r.subject);
            assertTrue(r.subject instanceof String);
        }

        @Test @DisplayName("Multi-word subject stored correctly")
        void multiWordSubject() {
            MarksRecord r = new MarksRecord("Data Structures", "IA1", 70, 100);
            assertEquals("Data Structures", r.subject);
        }

        @Test @DisplayName("Default constructor subject is null")
        void defaultSubjectNull() {
            MarksRecord r = new MarksRecord();
            assertNull(r.subject);
        }
    }

    // ── testType ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("testType field")
    class TestTypeTests {

        @Test @DisplayName("testType 'IA1' stored as String")
        void testTypeIA1() {
            MarksRecord r = new MarksRecord("Maths", "IA1", 80, 100);
            assertEquals("IA1", r.testType);
            assertTrue(r.testType instanceof String);
        }

        @Test @DisplayName("testType 'Quiz' stored correctly")
        void testTypeQuiz() {
            MarksRecord r = new MarksRecord("Physics", "Quiz", 20, 25);
            assertEquals("Quiz", r.testType);
        }

        @Test @DisplayName("testType 'Lab Report' (multi-word) stored correctly")
        void testTypeLabReport() {
            MarksRecord r = new MarksRecord("Chemistry", "Lab Report", 45, 50);
            assertEquals("Lab Report", r.testType);
        }

        @Test @DisplayName("testType 'Assignment' stored correctly")
        void testTypeAssignment() {
            MarksRecord r = new MarksRecord("CS", "Assignment", 92, 100);
            assertEquals("Assignment", r.testType);
        }

        @Test @DisplayName("Default constructor testType is null")
        void defaultTestTypeNull() {
            MarksRecord r = new MarksRecord();
            assertNull(r.testType);
        }
    }

    // ── marksObtained ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("marksObtained field")
    class MarksObtainedTests {

        @Test @DisplayName("marksObtained = 0 is a valid base case")
        void zeroMarksObtained() {
            MarksRecord r = new MarksRecord("Physics", "IA1", 0.0, 100.0);
            assertEquals(0.0, r.marksObtained, 0.001);
        }

        @Test @DisplayName("marksObtained equals maxMarks (full marks)")
        void fullMarksObtained() {
            MarksRecord r = new MarksRecord("Maths", "IA2", 100.0, 100.0);
            assertEquals(100.0, r.marksObtained, 0.001);
        }

        @Test @DisplayName("marksObtained is stored as double, not String")
        void marksObtainedIsDouble() {
            MarksRecord r = new MarksRecord("Maths", "Test", 75.5, 100.0);
            assertTrue(r.marksObtained == 75.5);
        }

        @Test @DisplayName("marksObtained with decimal value stored correctly")
        void decimalMarksObtained() {
            MarksRecord r = new MarksRecord("English", "Essay", 18.5, 25.0);
            assertEquals(18.5, r.marksObtained, 0.001);
        }
    }

    // ── maxMarks ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("maxMarks field")
    class MaxMarksTests {

        @Test @DisplayName("maxMarks = 100 is standard and valid")
        void maxMarks100() {
            MarksRecord r = new MarksRecord("Maths", "IA1", 85, 100);
            assertEquals(100.0, r.maxMarks, 0.001);
        }

        @Test @DisplayName("maxMarks = 50 (partial test) stored correctly")
        void maxMarks50() {
            MarksRecord r = new MarksRecord("Physics", "Quiz", 35, 50);
            assertEquals(50.0, r.maxMarks, 0.001);
        }

        @Test @DisplayName("maxMarks = 25 (small quiz) stored correctly")
        void maxMarks25() {
            MarksRecord r = new MarksRecord("Chemistry", "MCQ", 18, 25);
            assertEquals(25.0, r.maxMarks, 0.001);
        }

        @Test @DisplayName("maxMarks is stored as double, not String")
        void maxMarksIsDouble() {
            MarksRecord r = new MarksRecord("Maths", "Test", 70, 100);
            assertTrue(r.maxMarks > 0);
        }
    }

    // ── percentage calculation ─────────────────────────────────────────────

    @Nested
    @DisplayName("Percentage derived from marksObtained / maxMarks")
    class PercentageTests {

        @Test @DisplayName("85 out of 100 = 85.0%")
        void percentage85() {
            MarksRecord r = new MarksRecord("Maths", "IA1", 85, 100);
            double pct = r.marksObtained * 100.0 / r.maxMarks;
            assertEquals(85.0, pct, 0.001);
        }

        @Test @DisplayName("0 out of 100 = 0.0%")
        void percentageZero() {
            MarksRecord r = new MarksRecord("Physics", "IA1", 0, 100);
            double pct = r.marksObtained * 100.0 / r.maxMarks;
            assertEquals(0.0, pct, 0.001);
        }

        @Test @DisplayName("100 out of 100 = 100.0%")
        void percentage100() {
            MarksRecord r = new MarksRecord("CS", "IA2", 100, 100);
            double pct = r.marksObtained * 100.0 / r.maxMarks;
            assertEquals(100.0, pct, 0.001);
        }

        @Test @DisplayName("18 out of 25 = 72.0%")
        void percentageNon100Max() {
            MarksRecord r = new MarksRecord("English", "Test", 18, 25);
            double pct = r.marksObtained * 100.0 / r.maxMarks;
            assertEquals(72.0, pct, 0.001);
        }

        @Test @DisplayName("Student.addMarks ignores zero or negative maxMarks")
        void studentAddMarksIgnoresZeroMax() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "Uni");
            int before = s.marksMap.size();
            s.addMarks("Maths", "IA1", 80, 0);    // maxMarks=0 → ignored
            s.addMarks("Maths", "IA1", 80, -10);  // maxMarks<0 → ignored
            // Neither entry was added because maxMarks was invalid
            if (s.marksMap.containsKey("Maths")) {
                assertEquals(0, s.marksMap.get("Maths").size());
            }
        }

        @Test @DisplayName("Student.addMarks ignores negative marksObtained")
        void studentAddMarksIgnoresNegativeMarks() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "Uni");
            s.addMarks("Maths", "IA1", -5, 100);  // negative marks → ignored
            if (s.marksMap.containsKey("Maths")) {
                assertEquals(0, s.marksMap.get("Maths").size());
            }
        }
    }
}
