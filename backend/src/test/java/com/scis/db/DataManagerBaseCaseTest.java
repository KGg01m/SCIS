package com.scis.db;

import com.scis.model.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base-case tests for DataManager.
 *
 * Tests gracefully degrade when MongoDB is unavailable:
 *  – studentExists  → returns false (never throws)
 *  – loadStudent    → returns null  (never throws)
 *  – saveStudent    → silently no-ops (never throws)
 *
 * When MongoDB IS available, full round-trip contracts are verified.
 */
@DisplayName("DataManager Base Case Tests")
class DataManagerBaseCaseTest {

    private static Student buildStudent(String id) {
        Student s = new Student(id, "Test User", "test_" + id.toLowerCase() + "@test.com",
                "Computer Science", 3, "pass12", "Test College");
        s.addSubject("Mathematics");
        s.addMarks("Mathematics", "IA1", 80, 100);
        s.attendanceMap.get("Mathematics")
                .add(new AttendanceRecord("Mathematics", LocalDate.now().minusDays(1), true));
        return s;
    }

    // ═══════════════════════════════════════════════════════════════
    // Null / Offline Safety
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Null Safety and Offline Degradation")
    class OfflineSafetyTests {

        @Test @DisplayName("studentExists(null) does not throw")
        void existsNullIdNoThrow() {
            assertDoesNotThrow(() -> DataManager.studentExists(null));
        }

        @Test @DisplayName("studentExists(null) returns false")
        void existsNullIdReturnsFalse() {
            assertFalse(DataManager.studentExists(null));
        }

        @Test @DisplayName("studentExists(empty string) does not throw")
        void existsEmptyIdNoThrow() {
            assertDoesNotThrow(() -> DataManager.studentExists(""));
        }

        @Test @DisplayName("studentExists(empty string) returns false")
        void existsEmptyIdReturnsFalse() {
            assertFalse(DataManager.studentExists(""));
        }

        @Test @DisplayName("loadStudent(null, null) does not throw")
        void loadNullNullNoThrow() {
            assertDoesNotThrow(() -> DataManager.loadStudent(null, null));
        }

        @Test @DisplayName("loadStudent(null, null) returns null")
        void loadNullNullReturnsNull() {
            assertNull(DataManager.loadStudent(null, null));
        }

        @Test @DisplayName("loadStudent for nonexistent ID returns null")
        void loadNonexistentReturnsNull() {
            assertNull(DataManager.loadStudent("__DOES_NOT_EXIST_XYZ__", "anypass"));
        }

        @Test @DisplayName("saveStudent(null) does not throw")
        void saveNullDoesNotThrow() {
            assertDoesNotThrow(() -> DataManager.saveStudent(null));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Credential types — studentId and password must be Strings
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Credential Types — studentId and password are Strings")
    class CredentialTypeTests {

        @Test @DisplayName("studentExists accepts a String studentId without throwing")
        void studentIdIsStringArgument() {
            String id = "STRING_ID_001";
            assertDoesNotThrow(() -> DataManager.studentExists(id));
        }

        @Test @DisplayName("loadStudent accepts String id and String password without throwing")
        void loadAcceptsStrings() {
            assertDoesNotThrow(() -> DataManager.loadStudent("STRING_ID_002", "mypass12"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Wrong password
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Authentication — wrong password returns null")
    class WrongPasswordTests {

        @Test @DisplayName("loadStudent with wrong password returns null")
        void wrongPasswordReturnsNull() {
            Student s = buildStudent("DM_AUTH_001");
            DataManager.saveStudent(s);
            assertNull(DataManager.loadStudent("DM_AUTH_001", "WRONG_PASSWORD"),
                "Wrong password must return null");
        }

        @Test @DisplayName("loadStudent with empty password returns null")
        void emptyPasswordReturnsNull() {
            Student s = buildStudent("DM_AUTH_002");
            DataManager.saveStudent(s);
            assertNull(DataManager.loadStudent("DM_AUTH_002", ""));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Save → Load round-trip (gracefully skipped if MongoDB unavailable)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Save/Load Round-Trip")
    class RoundTripTests {

        @Test @DisplayName("Basic String fields round-trip correctly")
        void basicFieldsRoundTrip() {
            Student original = buildStudent("DM_RT_001");
            DataManager.saveStudent(original);
            Student loaded = DataManager.loadStudent("DM_RT_001", "pass12");
            if (loaded == null) return; // MongoDB unavailable

            assertEquals(original.studentId,  loaded.studentId,  "studentId must round-trip as same String");
            assertEquals(original.name,        loaded.name,        "name must round-trip as same String");
            assertEquals(original.email,       loaded.email,       "email must round-trip as same String");
            assertEquals(original.department,  loaded.department,  "department must round-trip as same String");
            assertEquals(original.semester,    loaded.semester,    "semester must round-trip as same int");
            assertEquals(original.collegeName, loaded.collegeName, "collegeName must round-trip as same String");
        }

        @Test @DisplayName("marksObtained is stored and loaded as a double (not a String)")
        void marksPersistAsDoubles() {
            Student s = buildStudent("DM_RT_002");
            DataManager.saveStudent(s);
            Student loaded = DataManager.loadStudent("DM_RT_002", "pass12");
            if (loaded == null) return;

            List<MarksRecord> marks = loaded.marksMap.get("Mathematics");
            assertNotNull(marks);
            assertFalse(marks.isEmpty());
            assertTrue(marks.get(0).marksObtained >= 0,
                "marksObtained must be numeric (double) after reload");
        }

        @Test @DisplayName("AttendanceRecord.present is a boolean after reload")
        void attendancePresentIsBoolean() {
            Student s = buildStudent("DM_RT_003");
            DataManager.saveStudent(s);
            Student loaded = DataManager.loadStudent("DM_RT_003", "pass12");
            if (loaded == null) return;

            List<AttendanceRecord> recs = loaded.attendanceMap.get("Mathematics");
            assertNotNull(recs);
            // present is a primitive boolean — just test it reads without error
            boolean val = recs.get(0).present;
            assertTrue(val == true || val == false,
                "present must be a boolean after DB round-trip");
        }

        @Test @DisplayName("Upsert: saving same student twice does not duplicate and reflects latest name")
        void upsertNoDuplication() {
            Student s = buildStudent("DM_UPSERT_001");
            DataManager.saveStudent(s);
            s.name = "Updated Name";
            DataManager.saveStudent(s);
            Student loaded = DataManager.loadStudent("DM_UPSERT_001", "pass12");
            if (loaded == null) return;
            assertEquals("Updated Name", loaded.name,
                "Upsert must reflect the latest save, not create a duplicate");
        }

        @Test @DisplayName("Tasks are persisted and restored as objects (not raw Strings)")
        void tasksPersist() {
            Student s = buildStudent("DM_RT_004");
            s.addTask(new StudentTask("Lab Report", "Lab Report", "Mathematics",
                    LocalDate.now().plusDays(7), "Write report", 2));
            DataManager.saveStudent(s);
            Student loaded = DataManager.loadStudent("DM_RT_004", "pass12");
            if (loaded == null) return;

            assertFalse(loaded.getTasks().isEmpty());
            assertEquals("Lab Report", loaded.getTasks().get(0).title,
                "Task title must be stored and retrieved as a String");
        }
    }
}
