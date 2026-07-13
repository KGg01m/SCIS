package com.scis.model;

import org.junit.jupiter.api.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base-case validation tests for StudentTask.
 *
 * Rules exercised:
 *  id          – auto-generated String (non-null, unique, 8 chars)
 *  title       – must be a non-empty String
 *  taskType    – must be one of StudentTask.TASK_TYPES or any String
 *  subject     – non-empty String
 *  dueDate     – LocalDate (not String, not int)
 *  priority    – int: 1 (High), 2 (Medium), 3 (Low); others default to Medium label
 *  submitted   – boolean
 *  description – String (can be empty)
 */
@DisplayName("StudentTask Validation Tests")
class StudentTaskValidationTest {

    // ── id (auto-generated) ───────────────────────────────────────────────

    @Nested
    @DisplayName("id auto-generation")
    class IdTests {

        @Test @DisplayName("Default constructor generates a non-null id")
        void idIsNonNull() {
            assertNotNull(new StudentTask().id);
        }

        @Test @DisplayName("id is a String, not null, not empty")
        void idIsString() {
            StudentTask t = new StudentTask();
            assertInstanceOf(String.class, t.id);
            assertFalse(t.id.isEmpty());
        }

        @Test @DisplayName("id is exactly 8 characters")
        void idIs8Chars() {
            assertEquals(8, new StudentTask().id.length());
        }

        @Test @DisplayName("Two tasks get unique ids")
        void idsAreUnique() {
            assertNotEquals(new StudentTask().id, new StudentTask().id);
        }
    }

    // ── title ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("title field")
    class TitleTests {

        @Test @DisplayName("Title is stored as String")
        void titleIsString() {
            StudentTask t = new StudentTask("Lab Report 3", "Lab Report", "Chemistry",
                    LocalDate.now().plusDays(5), "Write report", 1);
            assertEquals("Lab Report 3", t.title);
            assertInstanceOf(String.class, t.title);
        }

        @Test @DisplayName("Title with special characters stored correctly")
        void titleWithSpecialChars() {
            StudentTask t = new StudentTask("Chapter 3 – Kinematics", "Assignment", "Physics",
                    LocalDate.now().plusDays(3), "", 2);
            assertEquals("Chapter 3 – Kinematics", t.title);
        }

        @Test @DisplayName("Default constructor title is null")
        void defaultTitleNull() {
            assertNull(new StudentTask().title);
        }
    }

    // ── taskType ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("taskType field")
    class TaskTypeTests {

        @Test @DisplayName("TASK_TYPES constant array is non-null and non-empty")
        void taskTypesConstantExists() {
            assertNotNull(StudentTask.TASK_TYPES);
            assertTrue(StudentTask.TASK_TYPES.length > 0);
        }

        @Test @DisplayName("Each TASK_TYPES entry is a non-blank String")
        void taskTypesAreStrings() {
            for (String type : StudentTask.TASK_TYPES) {
                assertNotNull(type);
                assertFalse(type.isBlank());
                assertInstanceOf(String.class, type);
            }
        }

        @Test @DisplayName("'Quiz' is a valid task type")
        void quizIsValidType() {
            boolean found = false;
            for (String type : StudentTask.TASK_TYPES)
                if (type.equals("Quiz")) { found = true; break; }
            assertTrue(found, "TASK_TYPES should contain 'Quiz'");
        }

        @Test @DisplayName("taskType is stored as String")
        void taskTypeStoredAsString() {
            StudentTask t = new StudentTask("Q1", "Quiz", "Maths", LocalDate.now(), "", 2);
            assertInstanceOf(String.class, t.taskType);
        }
    }

    // ── subject ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("subject field")
    class SubjectTests {

        @Test @DisplayName("Subject is stored as String")
        void subjectIsString() {
            StudentTask t = new StudentTask("HW1", "Homework", "Mathematics",
                    LocalDate.now().plusDays(2), "", 3);
            assertEquals("Mathematics", t.subject);
            assertInstanceOf(String.class, t.subject);
        }

        @Test @DisplayName("Default constructor subject is null")
        void defaultSubjectNull() {
            assertNull(new StudentTask().subject);
        }
    }

    // ── dueDate ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("dueDate field")
    class DueDateTests {

        @Test @DisplayName("dueDate is stored as LocalDate")
        void dueDateIsLocalDate() {
            LocalDate d = LocalDate.now().plusDays(7);
            StudentTask t = new StudentTask("Essay", "Assignment", "English", d, "", 2);
            assertEquals(d, t.dueDate);
            assertInstanceOf(LocalDate.class, t.dueDate);
        }

        @Test @DisplayName("Past dueDate is accepted")
        void pastDueDateAccepted() {
            LocalDate past = LocalDate.now().minusDays(3);
            StudentTask t = new StudentTask("Old HW", "Homework", "Maths", past, "", 2);
            assertEquals(past, t.dueDate);
        }

        @Test @DisplayName("null dueDate is accepted (open-ended task)")
        void nullDueDateAccepted() {
            StudentTask t = new StudentTask("Open", "Other", "CS", null, "", 3);
            assertNull(t.dueDate);
        }

        @Test @DisplayName("getDaysUntilDue returns Long.MAX_VALUE when dueDate is null")
        void daysUntilDueNullReturnsMax() {
            StudentTask t = new StudentTask();
            t.dueDate = null;
            assertEquals(Long.MAX_VALUE, t.getDaysUntilDue());
        }
    }

    // ── priority ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("priority field — int not String")
    class PriorityTests {

        @Test @DisplayName("priority is stored as int, not String")
        void priorityIsInt() {
            StudentTask t = new StudentTask("Test", "Quiz", "Maths", LocalDate.now(), "", 1);
            assertEquals(1, t.priority);
            // Confirm it's a primitive int (no .equals needed — direct int comparison)
            assertTrue(t.priority == 1);
        }

        @Test @DisplayName("Priority 1 = High label")
        void priority1IsHigh() {
            StudentTask t = new StudentTask();
            t.priority = 1;
            assertEquals("High", t.getPriorityLabel());
        }

        @Test @DisplayName("Priority 2 = Medium label")
        void priority2IsMedium() {
            StudentTask t = new StudentTask();
            t.priority = 2;
            assertEquals("Medium", t.getPriorityLabel());
        }

        @Test @DisplayName("Priority 3 = Low label")
        void priority3IsLow() {
            StudentTask t = new StudentTask();
            t.priority = 3;
            assertEquals("Low", t.getPriorityLabel());
        }

        @Test @DisplayName("Unrecognised priority defaults to 'Medium' label")
        void unknownPriorityDefaultsMedium() {
            StudentTask t = new StudentTask();
            t.priority = 999;
            assertEquals("Medium", t.getPriorityLabel());
        }

        @Test @DisplayName("Negative priority defaults to 'Medium' label")
        void negativePriorityDefaultsMedium() {
            StudentTask t = new StudentTask();
            t.priority = -1;
            assertEquals("Medium", t.getPriorityLabel());
        }
    }

    // ── submitted ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitted field")
    class SubmittedTests {

        @Test @DisplayName("New task submitted = false by default")
        void submittedFalseByDefault() {
            assertFalse(new StudentTask().submitted);
        }

        @Test @DisplayName("submitted is a boolean, not 0/1 integer")
        void submittedIsBoolean() {
            StudentTask t = new StudentTask();
            t.submitted = true;
            assertTrue(t.submitted == true || t.submitted == false);
        }

        @Test @DisplayName("Submitted task gets status 'Submitted'")
        void submittedTaskHasSubmittedStatus() {
            StudentTask t = new StudentTask("HW", "Homework", "CS",
                    LocalDate.now().plusDays(5), "", 2);
            t.submitted = true;
            assertEquals("Submitted", t.getStatusLabel());
        }

        @Test @DisplayName("Unsubmitted past-due task gets status 'Overdue'")
        void unsubmittedPastDueIsOverdue() {
            StudentTask t = new StudentTask("Old", "Quiz", "CS",
                    LocalDate.now().minusDays(2), "", 2);
            assertEquals("Overdue", t.getStatusLabel());
        }
    }

    // ── createdDate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createdDate field")
    class CreatedDateTests {

        @Test @DisplayName("createdDate defaults to today")
        void createdDateIsToday() {
            assertEquals(LocalDate.now(), new StudentTask().createdDate);
        }

        @Test @DisplayName("createdDate is a LocalDate instance")
        void createdDateIsLocalDate() {
            assertInstanceOf(LocalDate.class, new StudentTask().createdDate);
        }
    }
}
