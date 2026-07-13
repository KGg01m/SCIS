package com.scis.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Base64;

/**
 * MedicalLeave — a medical leave request submitted by a student
 * (or recorded by a teacher on behalf of the student).
 *
 * Key rules:
 *  - Approved leaves are EXCLUDED from attendance percentage calculation
 *    (they count neither as present nor absent — denominator shrinks).
 *  - Pending/Rejected leaves are counted as absent.
 *  - A leave can span multiple days (startDate → endDate inclusive).
 */
public class MedicalLeave implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { PENDING, APPROVED, REJECTED }

    public String    id;
    public String    subject;          // which subject the leave covers (or "All Subjects")
    public LocalDate startDate;
    public LocalDate endDate;
    public String    reason;           // student's description
    public String    documentRef;      // e.g. "Hospital receipt, Dr. Sharma" — text reference
    public String    attachmentName;   // uploaded file name (e.g. "prescription.pdf")
    public String    attachmentData;   // base64 encoded file data
    public String    attachmentType;   // MIME type of the file
    public Status    status;
    public String    reviewedBy;       // teacher ID who approved/rejected
    public String    rejectionNote;    // teacher note on rejection
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

    /** Number of calendar days this leave spans (inclusive). */
    public long getDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** True if the given date falls within this leave window. */
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

    /** Check if this leave has a file attachment. */
    public boolean hasAttachment() {
        return attachmentData != null && !attachmentData.isEmpty();
    }

    /** Set file attachment data (base64 encoded). */
    public void setAttachment(String fileName, String mimeType, byte[] fileData) {
        this.attachmentName = fileName;
        this.attachmentType = mimeType;
        this.attachmentData = Base64.getEncoder().encodeToString(fileData);
    }

    /** Get file attachment data (decoded from base64). */
    public byte[] getAttachmentData() {
        if (attachmentData == null || attachmentData.isEmpty()) return null;
        return Base64.getDecoder().decode(attachmentData);
    }

    /** Get display text for attachment column in table. */
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
