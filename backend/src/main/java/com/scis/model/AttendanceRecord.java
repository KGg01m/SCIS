// AttendanceRecord - stores a single attendance entry for a student
package com.scis.model;
import java.io.Serializable;
import java.time.LocalDate;

public class AttendanceRecord implements Serializable {
    private static final long serialVersionUID = 2L;

    public String subject;
    public LocalDate date;
    public boolean present;
    public boolean excused;
    public String medicalLeaveId;

    public AttendanceRecord() {}

    public AttendanceRecord(String subject, LocalDate date, boolean present) {
        this.subject = subject;
        this.date = date;
        this.present = present;
    }

    public AttendanceRecord(String subject, LocalDate date, boolean present, boolean excused) {
        this.subject = subject;
        this.date = date;
        this.present = present;
        this.excused = excused;
    }

    public boolean isMedicalLeave() {
        return !present && medicalLeaveId != null && !medicalLeaveId.isEmpty();
    }
}
