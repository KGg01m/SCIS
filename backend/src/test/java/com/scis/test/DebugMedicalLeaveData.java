package com.scis.test;

import com.scis.db.DataManager;
import com.scis.model.Student;
import com.scis.model.MedicalLeave;

/**
 * Debug program to check medical leave data in database
 */
public class DebugMedicalLeaveData {
    
    public static void main(String[] args) {
        System.out.println("=== DEBUG: Medical Leave Data in Database ===\n");
        
        try {
            // Load the test student
            Student student = DataManager.loadStudentById("TEST-001");
            
            if (student == null) {
                System.out.println("❌ Test student TEST-001 not found!");
                return;
            }
            
            System.out.println("✅ Found student: " + student.name);
            System.out.println("Medical leaves: " + student.getMedicalLeaves().size());
            System.out.println();
            
            int index = 1;
            for (MedicalLeave ml : student.getMedicalLeaves()) {
                System.out.println("--- LEAVE " + index + " ---");
                System.out.println("Subject: " + ml.subject);
                System.out.println("Dates: " + ml.startDate + " → " + ml.endDate);
                System.out.println("Reason: " + ml.reason);
                System.out.println("Status: " + ml.getStatusLabel());
                System.out.println("Document Reference: " + ml.documentRef);
                System.out.println("Attachment Name: " + ml.attachmentName);
                System.out.println("Attachment Type: " + ml.attachmentType);
                System.out.println("Has Attachment: " + ml.hasAttachment());
                System.out.println("Attachment Data Size: " + 
                    (ml.getAttachmentData() != null ? ml.getAttachmentData().length : 0) + " bytes");
                System.out.println("Display Text: " + ml.getAttachmentDisplay());
                System.out.println();
                index++;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
