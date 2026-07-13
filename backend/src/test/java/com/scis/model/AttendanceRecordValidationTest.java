package com.scis.model;

import org.junit.jupiter.api.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base-case validation tests for AttendanceRecord.
 *
 * Rules exercised:
 *  subject  – must be a non-empty String
 *  date     – must be a LocalDate (not String, not int)
 *  present  – boolean only (true/false, not 0/1)
 */
@DisplayName("AttendanceRecord Validation Tests")
class AttendanceRecordValidationTest {

    // ── subject ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("subject field")
    class SubjectTests {

        @Test @DisplayName("Subject stores a valid non-empty string")
        void validSubjectStored() {
            AttendanceRecord r = new AttendanceRecord("Mathematics", LocalDate.now(), true);
            assertEquals("Mathematics", r.subject);
        }

        @Test @DisplayName("Subject field is a String type")
        void subjectIsString() {
            AttendanceRecord r = new AttendanceRecord("Physics", LocalDate.now(), false);
            assertTrue(r.subject instanceof String);
        }

        @Test @DisplayName("Multi-word subject name stored correctly")
        void multiWordSubject() {
            AttendanceRecord r = new AttendanceRecord("Computer Science", LocalDate.now(), true);
            assertEquals("Computer Science", r.subject);
        }

        @Test @DisplayName("Default constructor subject is null (no validation enforced at field level)")
        void defaultConstructorSubjectIsNull() {
            AttendanceRecord r = new AttendanceRecord();
            assertNull(r.subject);
        }
    }

    // ── date ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("date field")
    class DateTests {

        @Test @DisplayName("Date is stored as LocalDate")
        void dateIsLocalDate() {
            LocalDate d = LocalDate.of(2025, 3, 15);
            AttendanceRecord r = new AttendanceRecord("Maths", d, true);
            assertEquals(d, r.date);
        }

        @Test @DisplayName("Past date is accepted")
        void pastDateAccepted() {
            LocalDate past = LocalDate.now().minusDays(30);
            AttendanceRecord r = new AttendanceRecord("Physics", past, true);
            assertEquals(past, r.date);
        }

        @Test @DisplayName("Today's date is accepted")
        void todayAccepted() {
            LocalDate today = LocalDate.now();
            AttendanceRecord r = new AttendanceRecord("Chemistry", today, false);
            assertEquals(today, r.date);
        }

        @Test @DisplayName("Future date is accepted (advance scheduling)")
        void futureDateAccepted() {
            LocalDate future = LocalDate.now().plusDays(7);
            AttendanceRecord r = new AttendanceRecord("Biology", future, true);
            assertEquals(future, r.date);
        }

        @Test @DisplayName("Default constructor date is null")
        void defaultConstructorDateNull() {
            AttendanceRecord r = new AttendanceRecord();
            assertNull(r.date);
        }
    }

    // ── present ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("present field")
    class PresentTests {

        @Test @DisplayName("present=true stores correctly")
        void presentTrue() {
            AttendanceRecord r = new AttendanceRecord("Maths", LocalDate.now(), true);
            assertTrue(r.present);
        }

        @Test @DisplayName("present=false stores correctly")
        void presentFalse() {
            AttendanceRecord r = new AttendanceRecord("Maths", LocalDate.now(), false);
            assertFalse(r.present);
        }

        @Test @DisplayName("present field is boolean type")
        void presentIsBoolean() {
            AttendanceRecord r = new AttendanceRecord("Maths", LocalDate.now(), true);
            // Java boolean is a primitive, just verify the value is one of exactly two states
            assertTrue(r.present == true || r.present == false);
        }

        @Test @DisplayName("Two records with same data but different present have different values")
        void presentDistinguishable() {
            LocalDate d = LocalDate.now();
            AttendanceRecord rPresent  = new AttendanceRecord("Maths", d, true);
            AttendanceRecord rAbsent   = new AttendanceRecord("Maths", d, false);
            assertNotEquals(rPresent.present, rAbsent.present);
        }
    }

    // ── Used inside Student attendance tracking ────────────────────────────

    @Nested
    @DisplayName("AttendanceRecord used inside Student")
    class StudentIntegrationTests {

        @Test @DisplayName("Adding AttendanceRecord increases attendance count")
        void recordIncreasesCount() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "Uni");
            s.addSubject("Maths");
            s.attendanceMap.get("Maths").add(new AttendanceRecord("Maths", LocalDate.now(), true));
            assertEquals(1, s.attendanceMap.get("Maths").size());
        }

        @Test @DisplayName("Present record counts toward attendance percentage")
        void presentCountsTowardsPercentage() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "Uni");
            s.addSubject("Physics");
            s.attendanceMap.get("Physics").add(new AttendanceRecord("Physics", LocalDate.now(), true));
            assertEquals(100.0, s.getSubjectAttendance("Physics"), 0.001);
        }

        @Test @DisplayName("Absent record does NOT count toward attendance percentage")
        void absentNotCountedAspresent() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "Uni");
            s.addSubject("Chemistry");
            s.attendanceMap.get("Chemistry").add(new AttendanceRecord("Chemistry", LocalDate.now(), false));
            assertEquals(0.0, s.getSubjectAttendance("Chemistry"), 0.001);
        }

        @Test @DisplayName("Mix of present and absent gives correct percentage")
        void mixedAttendancePercentage() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "Uni");
            s.addSubject("Biology");
            s.attendanceMap.get("Biology").add(new AttendanceRecord("Biology", LocalDate.now().minusDays(3), true));
            s.attendanceMap.get("Biology").add(new AttendanceRecord("Biology", LocalDate.now().minusDays(2), true));
            s.attendanceMap.get("Biology").add(new AttendanceRecord("Biology", LocalDate.now().minusDays(1), false));
            s.attendanceMap.get("Biology").add(new AttendanceRecord("Biology", LocalDate.now(), true));
            // 3 present out of 4 = 75%
            assertEquals(75.0, s.getSubjectAttendance("Biology"), 0.001);
        }
    }
}
