package com.scis.model;

import org.junit.jupiter.api.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StudentTask model
 * Tests: constructors, status labels, priority labels, days until due
 */
@DisplayName("StudentTask Model Tests")
class StudentTaskTest {

    // ─── Constructors ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Default constructor sets defaults correctly")
    void testDefaultConstructor() {
        StudentTask task = new StudentTask();
        assertNotNull(task.id, "ID should be auto-generated");
        assertFalse(task.submitted);
        assertEquals(2, task.priority); // Medium
        assertNotNull(task.createdDate);
        assertEquals(LocalDate.now(), task.createdDate);
    }

    @Test
    @DisplayName("Full constructor sets all fields")
    void testFullConstructor() {
        LocalDate due = LocalDate.now().plusDays(5);
        StudentTask task = new StudentTask("Essay", "Assignment", "English",
                due, "Write 500 words", 1);
        assertEquals("Essay", task.title);
        assertEquals("Assignment", task.taskType);
        assertEquals("English", task.subject);
        assertEquals(due, task.dueDate);
        assertEquals("Write 500 words", task.description);
        assertEquals(1, task.priority);
    }

    @Test
    @DisplayName("Each new task gets a unique ID")
    void testUniqueIds() {
        StudentTask t1 = new StudentTask();
        StudentTask t2 = new StudentTask();
        assertNotEquals(t1.id, t2.id);
    }

    // ─── getPriorityLabel ─────────────────────────────────────────────────

    @Test
    @DisplayName("Priority 1 → 'High'")
    void testPriorityHigh() {
        StudentTask task = new StudentTask();
        task.priority = 1;
        assertEquals("High", task.getPriorityLabel());
    }

    @Test
    @DisplayName("Priority 2 → 'Medium'")
    void testPriorityMedium() {
        StudentTask task = new StudentTask();
        task.priority = 2;
        assertEquals("Medium", task.getPriorityLabel());
    }

    @Test
    @DisplayName("Priority 3 → 'Low'")
    void testPriorityLow() {
        StudentTask task = new StudentTask();
        task.priority = 3;
        assertEquals("Low", task.getPriorityLabel());
    }

    @Test
    @DisplayName("Unknown priority defaults to 'Medium'")
    void testPriorityDefault() {
        StudentTask task = new StudentTask();
        task.priority = 99;
        assertEquals("Medium", task.getPriorityLabel());
    }

    // ─── getStatusLabel ───────────────────────────────────────────────────

    @Test
    @DisplayName("Submitted task → 'Submitted'")
    void testStatusSubmitted() {
        StudentTask task = new StudentTask();
        task.submitted = true;
        task.dueDate = LocalDate.now().minusDays(1); // even if overdue
        assertEquals("Submitted", task.getStatusLabel());
    }

    @Test
    @DisplayName("Past due, not submitted → 'Overdue'")
    void testStatusOverdue() {
        StudentTask task = new StudentTask();
        task.submitted = false;
        task.dueDate = LocalDate.now().minusDays(3);
        assertEquals("Overdue", task.getStatusLabel());
    }

    @Test
    @DisplayName("Due within 2 days, not submitted → 'Due Soon'")
    void testStatusDueSoon() {
        StudentTask task = new StudentTask();
        task.submitted = false;
        task.dueDate = LocalDate.now().plusDays(1);
        assertEquals("Due Soon", task.getStatusLabel());
    }

    @Test
    @DisplayName("Due in future (>2 days), not submitted → 'Pending'")
    void testStatusPending() {
        StudentTask task = new StudentTask();
        task.submitted = false;
        task.dueDate = LocalDate.now().plusDays(10);
        assertEquals("Pending", task.getStatusLabel());
    }

    @Test
    @DisplayName("No due date → 'Pending'")
    void testStatusNoDueDate() {
        StudentTask task = new StudentTask();
        task.submitted = false;
        task.dueDate = null;
        assertEquals("Pending", task.getStatusLabel());
    }

    @Test
    @DisplayName("Due exactly today (0 days) → 'Due Soon'")
    void testStatusDueToday() {
        StudentTask task = new StudentTask();
        task.submitted = false;
        task.dueDate = LocalDate.now();
        assertEquals("Due Soon", task.getStatusLabel());
    }

    // ─── getDaysUntilDue ──────────────────────────────────────────────────

    @Test
    @DisplayName("getDaysUntilDue returns Long.MAX_VALUE when dueDate is null")
    void testDaysUntilDueNullDate() {
        StudentTask task = new StudentTask();
        task.dueDate = null;
        assertEquals(Long.MAX_VALUE, task.getDaysUntilDue());
    }

    @Test
    @DisplayName("getDaysUntilDue returns positive value for future due date")
    void testDaysUntilDueFuture() {
        StudentTask task = new StudentTask();
        task.dueDate = LocalDate.now().plusDays(7);
        assertEquals(7, task.getDaysUntilDue());
    }

    @Test
    @DisplayName("getDaysUntilDue returns negative value for past due date")
    void testDaysUntilDuePast() {
        StudentTask task = new StudentTask();
        task.dueDate = LocalDate.now().minusDays(3);
        assertEquals(-3, task.getDaysUntilDue());
    }

    @Test
    @DisplayName("getDaysUntilDue returns 0 for today")
    void testDaysUntilDueToday() {
        StudentTask task = new StudentTask();
        task.dueDate = LocalDate.now();
        assertEquals(0, task.getDaysUntilDue());
    }

    // ─── TASK_TYPES constant ─────────────────────────────────────────────

    @Test
    @DisplayName("TASK_TYPES array is non-null and non-empty")
    void testTaskTypesArrayExists() {
        assertNotNull(StudentTask.TASK_TYPES);
        assertTrue(StudentTask.TASK_TYPES.length > 0);
    }

    @Test
    @DisplayName("TASK_TYPES includes 'Assignment' and 'Quiz'")
    void testTaskTypesContainsCommonTypes() {
        boolean hasAssignment = false, hasQuiz = false;
        for (String type : StudentTask.TASK_TYPES) {
            if ("Assignment".equals(type)) hasAssignment = true;
            if ("Quiz".equals(type)) hasQuiz = true;
        }
        assertTrue(hasAssignment, "Expected 'Assignment' in TASK_TYPES");
        assertTrue(hasQuiz, "Expected 'Quiz' in TASK_TYPES");
    }
}
