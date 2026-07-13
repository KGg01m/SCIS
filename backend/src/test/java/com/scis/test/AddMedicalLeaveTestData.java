package com.scis.test;

import com.scis.db.DataManager;
import com.scis.model.MedicalLeave;
import com.scis.model.Student;

import java.time.LocalDate;
import java.util.Base64;

/**
 * Test program to add sample medical leave data with documents
 */
public class AddMedicalLeaveTestData {
    
    public static void main(String[] args) {
        System.out.println("Adding test medical leave data...");
        
        try {
            // Create a test student if not exists
            Student student = new Student("TEST-001", "Test Student", 
                "test@student.edu", "CSE", 1, "password123", "Test College");
            
            // Add some subjects
            student.addSubject("Mathematics");
            student.addSubject("Computer Science");
            
            // Create medical leave with file attachment
            MedicalLeave leave1 = new MedicalLeave("Mathematics", 
                LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 17), 
                "Fever and headache");
            leave1.documentRef = "Dr. Sharma, Apollo Hospital - OPD123";
            
            // Add a sample file attachment (small text file as base64)
            String sampleContent = "This is a sample medical certificate.\nPatient: Test Student\nDate: 15/01/2025\nDoctor: Dr. Sharma\nDiagnosis: Viral Fever\nRecommended Rest: 3 days";
            byte[] fileData = sampleContent.getBytes();
            leave1.setAttachment("medical_certificate.txt", "text/plain", fileData);
            
            // Create medical leave with only document reference
            MedicalLeave leave2 = new MedicalLeave("Computer Science",
                LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 12),
                "Sprained wrist");
            leave2.documentRef = "Dr. Kumar, City Hospital - FR456";
            
            // Create medical leave with no document
            MedicalLeave leave3 = new MedicalLeave("Mathematics",
                LocalDate.of(2025, 3, 5), LocalDate.of(2025, 3, 6),
                "Common cold");
            
            // Add leaves to student
            student.addMedicalLeave(leave1);
            student.addMedicalLeave(leave2);
            student.addMedicalLeave(leave3);
            
            // Save student
            DataManager.saveStudent(student);
            
            System.out.println("✅ Test data added successfully!");
            System.out.println("Student ID: " + student.studentId);
            System.out.println("Medical leaves added: " + student.getMedicalLeaves().size());
            
            for (MedicalLeave ml : student.getMedicalLeaves()) {
                System.out.println("  - " + ml.subject + ": " + ml.getAttachmentDisplay() + 
                    " (Status: " + ml.getStatusLabel() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error adding test data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
