// StudentTask - stores student tasks, assignments and deadlines
package com.scis.model;

import java.io.Serializable;
import java.time.LocalDate;

public class StudentTask implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String[] TASK_TYPES = {
        "Quiz", "Test", "Assignment", "Project", "Lab Report",
        "Presentation", "Homework", "Exam", "Viva", "Other"
    };

    public String id;
    public String title;
    public String taskType;
    public String subject;
    public LocalDate dueDate;
    public String description;
    public int priority;
    public boolean submitted;
    public LocalDate submittedDate;
    public String notes;
    public LocalDate createdDate;

    public StudentTask() {
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8);
        this.createdDate = LocalDate.now();
        this.submitted = false;
        this.priority = 2;
    }

    public StudentTask(String title, String taskType, String subject,
                       LocalDate dueDate, String description, int priority) {
        this();
        this.title = title;
        this.taskType = taskType;
        this.subject = subject;
        this.dueDate = dueDate;
        this.description = description;
        this.priority = priority;
    }

    public String getPriorityLabel() {
        switch (priority) {
            case 1: return "High";
            case 2: return "Medium";
            case 3: return "Low";
            default: return "Medium";
        }
    }

    public String getStatusLabel() {
        if (submitted) return "Submitted";
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) return "Overdue";
        if (dueDate != null) {
            long days = LocalDate.now().until(dueDate, java.time.temporal.ChronoUnit.DAYS);
            if (days <= 2) return "Due Soon";
        }
        return "Pending";
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;
        return LocalDate.now().until(dueDate, java.time.temporal.ChronoUnit.DAYS);
    }
}
