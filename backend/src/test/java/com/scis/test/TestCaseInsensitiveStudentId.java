package com.scis.test;

import com.scis.db.DataManager;
import com.scis.model.Student;

/**
 * Test program to verify case-insensitive student ID functionality
 */
public class TestCaseInsensitiveStudentId {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Case-Insensitive Student ID ===\n");
        
        try {
            // Test 1: Create student with uppercase ID
            System.out.println("1. Creating student with uppercase ID: KH123");
            Student student1 = new Student("KH123", "Test Student 1", 
                "test1@student.edu", "CSE", 1, "password123", "Test College");
            student1.addSubject("Mathematics");
            DataManager.saveStudent(student1);
            
            // Test 2: Try to create same student with lowercase ID (should fail/overwrite)
            System.out.println("2. Creating student with lowercase ID: kh123");
            Student student2 = new Student("kh123", "Test Student 2", 
                "test2@student.edu", "CSE", 1, "password456", "Test College");
            student2.addSubject("Computer Science");
            DataManager.saveStudent(student2);
            
            // Test 3: Load with uppercase
            System.out.println("3. Loading student with uppercase ID: KH123");
            Student loaded1 = DataManager.loadStudent("KH123", "password123");
            if (loaded1 != null) {
                System.out.println("   ✅ Found: " + loaded1.name + " (" + loaded1.studentId + ")");
            } else {
                System.out.println("   ❌ Not found");
            }
            
            // Test 4: Load with lowercase
            System.out.println("4. Loading student with lowercase ID: kh123");
            Student loaded2 = DataManager.loadStudent("kh123", "password456");
            if (loaded2 != null) {
                System.out.println("   ✅ Found: " + loaded2.name + " (" + loaded2.studentId + ")");
            } else {
                System.out.println("   ❌ Not found");
            }
            
            // Test 5: Load with mixed case
            System.out.println("5. Loading student with mixed case ID: Kh123");
            Student loaded3 = DataManager.loadStudent("Kh123", "password456");
            if (loaded3 != null) {
                System.out.println("   ✅ Found: " + loaded3.name + " (" + loaded3.studentId + ")");
            } else {
                System.out.println("   ❌ Not found");
            }
            
            // Test 6: Check if student exists with different cases
            System.out.println("6. Checking student existence:");
            System.out.println("   KH123 exists: " + DataManager.studentExists("KH123"));
            System.out.println("   kh123 exists: " + DataManager.studentExists("kh123"));
            System.out.println("   Kh123 exists: " + DataManager.studentExists("Kh123"));
            
            // Test 7: Load by ID with different cases
            System.out.println("7. Loading by ID (no password):");
            Student byId1 = DataManager.loadStudentById("KH123");
            Student byId2 = DataManager.loadStudentById("kh123");
            System.out.println("   KH123: " + (byId1 != null ? byId1.name : "null"));
            System.out.println("   kh123: " + (byId2 != null ? byId2.name : "null"));
            
            System.out.println("\n✅ Case-insensitive student ID test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
