// LeaveApplication - stores student leave applications
package com.scis.model;

import java.io.Serializable;
import java.time.LocalDate;

public class LeaveApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public String studentId;
    public LocalDate startDate;
    public LocalDate endDate;
    public String reason;
    public String type;
    public String status;

    public LeaveApplication() {}

    public LeaveApplication(String id, String studentId, LocalDate startDate, LocalDate endDate, String type, String reason) {
        this.id = id;
        this.studentId = studentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.reason = reason;
        this.status = "PENDING";
    }
}
