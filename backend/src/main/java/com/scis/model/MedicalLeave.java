// MedicalLeave - stores medical leave requests submitted by students
package com.scis.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Base64;

public class MedicalLeave implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { PENDING, APPROVED, REJECTED }

    public String id;
    public String subject;
    public LocalDate startDate;
    public LocalDate endDate;
    public String reason;
    public String documentRef;
    public String attachmentName;
    public String attachmentData;
    public String attachmentType;
    public Status status;
    public String reviewedBy;
    public String rejectionNote;
    public LocalDate submittedDate;
    public LocalDate reviewedDate;

    public MedicalLeave() {
        this.id            = UUID.randomUUID().toString().substring(0, 8);
        this.status        = Status.PENDING;
        this.submittedDate = LocalDate.now();
    }

    public MedicalLeave(String subject, LocalDate start, LocalDate end, String reason) {
        this();
        this.subject   = subject;
        this.startDate = start;
        this.endDate   = end;
        this.reason    = reason;
    }

    public long getDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public boolean covers(LocalDate date) {
        if (date == null || startDate == null || endDate == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean isApproved()  { return status == Status.APPROVED;  }
    public boolean isPending()   { return status == Status.PENDING;   }
    public boolean isRejected()  { return status == Status.REJECTED;  }

    public String getStatusLabel() {
        switch (status) {
            case APPROVED: return "Approved";
            case REJECTED: return "Rejected";
            default:       return "Pending";
        }
    }

    public void approve(String teacherId) {
        this.status       = Status.APPROVED;
        this.reviewedBy   = teacherId;
        this.reviewedDate = LocalDate.now();
    }

    public void reject(String teacherId, String note) {
        this.status         = Status.REJECTED;
        this.reviewedBy     = teacherId;
        this.reviewedDate   = LocalDate.now();
        this.rejectionNote  = note;
    }

    public boolean hasAttachment() {
        return attachmentData != null && !attachmentData.isEmpty();
    }

    public void setAttachment(String fileName, String mimeType, byte[] fileData) {
        this.attachmentName = fileName;
        this.attachmentType = mimeType;
        this.attachmentData = Base64.getEncoder().encodeToString(fileData);
    }

    public byte[] getAttachmentData() {
        if (attachmentData == null || attachmentData.isEmpty()) return null;
        return Base64.getDecoder().decode(attachmentData);
    }

    public String getAttachmentDisplay() {
        if (hasAttachment()) {
            return "📎 " + attachmentName;
        } else if (documentRef != null && !documentRef.trim().isEmpty()) {
            return "📝 " + documentRef;
        } else {
            return "None";
        }
    }
}
