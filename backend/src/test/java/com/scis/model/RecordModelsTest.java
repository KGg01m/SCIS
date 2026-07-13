package com.scis.model;

import org.junit.jupiter.api.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AttendanceRecord and MarksRecord model classes
 */
@DisplayName("Record Model Tests")
class RecordModelsTest {

    // ─── AttendanceRecord ─────────────────────────────────────────────────

    @Nested
    @DisplayName("AttendanceRecord")
    class AttendanceRecordTests {

        @Test
        @DisplayName("Full constructor sets all fields correctly")
        void testFullConstructor() {
            LocalDate date = LocalDate.of(2025, 3, 10);
            AttendanceRecord record = new AttendanceRecord("Mathematics", date, true);
            assertEquals("Mathematics", record.subject);
            assertEquals(date, record.date);
            assertTrue(record.present);
        }

        @Test
        @DisplayName("Default constructor creates object without throwing")
        void testDefaultConstructor() {
            assertDoesNotThrow(() -> new AttendanceRecord());
        }

        @Test
        @DisplayName("Absent record has present = false")
        void testAbsentRecord() {
            AttendanceRecord record = new AttendanceRecord("Physics", LocalDate.now(), false);
            assertFalse(record.present);
        }

        @Test
        @DisplayName("Present record has present = true")
        void testPresentRecord() {
            AttendanceRecord record = new AttendanceRecord("Chemistry", LocalDate.now(), true);
            assertTrue(record.present);
        }

        @Test
        @DisplayName("Subject field stores value correctly")
        void testSubjectField() {
            AttendanceRecord record = new AttendanceRecord("English", LocalDate.now(), true);
            assertEquals("English", record.subject);
        }

        @Test
        @DisplayName("Date field stores value correctly")
        void testDateField() {
            LocalDate specificDate = LocalDate.of(2025, 1, 15);
            AttendanceRecord record = new AttendanceRecord("Science", specificDate, false);
            assertEquals(specificDate, record.date);
        }
    }

    // ─── MarksRecord ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("MarksRecord")
    class MarksRecordTests {

        @Test
        @DisplayName("Full constructor sets all fields correctly")
        void testFullConstructor() {
            MarksRecord record = new MarksRecord("Mathematics", "IA1", 85.0, 100.0);
            assertEquals("Mathematics", record.subject);
            assertEquals("IA1", record.testType);
            assertEquals(85.0, record.marksObtained, 0.001);
            assertEquals(100.0, record.maxMarks, 0.001);
        }

        @Test
        @DisplayName("Default constructor creates object without throwing")
        void testDefaultConstructor() {
            assertDoesNotThrow(() -> new MarksRecord());
        }

        @Test
        @DisplayName("marksObtained can be 0")
        void testZeroMarks() {
            MarksRecord record = new MarksRecord("Physics", "Quiz", 0.0, 50.0);
            assertEquals(0.0, record.marksObtained, 0.001);
        }

        @Test
        @DisplayName("marksObtained equals maxMarks for full marks")
        void testFullMarks() {
            MarksRecord record = new MarksRecord("CS", "Assignment", 100.0, 100.0);
            assertEquals(record.maxMarks, record.marksObtained, 0.001);
        }

        @Test
        @DisplayName("testType field stores value correctly")
        void testTestTypeField() {
            MarksRecord record = new MarksRecord("Chemistry", "Lab Report", 72.0, 100.0);
            assertEquals("Lab Report", record.testType);
        }

        @Test
        @DisplayName("Percentage can be calculated from record fields")
        void testPercentageCalculation() {
            MarksRecord record = new MarksRecord("Math", "IA2", 75.0, 100.0);
            double percentage = record.marksObtained * 100.0 / record.maxMarks;
            assertEquals(75.0, percentage, 0.001);
        }

        @Test
        @DisplayName("Non-100 maxMarks percentage is calculated correctly")
        void testNon100MaxMarks() {
            MarksRecord record = new MarksRecord("English", "Test", 18.0, 25.0);
            double percentage = record.marksObtained * 100.0 / record.maxMarks;
            assertEquals(72.0, percentage, 0.001);
        }
    }
}
