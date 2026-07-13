package com.scis.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base-case validation tests for Student field rules.
 *
 * Every public-facing constraint is exercised here:
 *   • studentId  – type (String only), format, length
 *   • name       – type (String only), must start with letter, no pure-numeric
 *   • email      – '@' rule, domain '.' rule
 *   • department – not purely numeric
 *   • semester   – int range 1-12
 *   • password   – minimum 6 chars
 *   • collegeName – null/blank → "Unknown College"
 */
@DisplayName("Student Field Validation Tests")
class StudentValidationTest {

    // ═══════════════════════════════════════════════════════════════
    // studentId
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("studentId validation")
    class StudentIdTests {

        @Test @DisplayName("Valid alphanumeric id is accepted")
        void validId() {
            assertDoesNotThrow(() -> Student.validateStudentId("CS2024001"));
        }

        @Test @DisplayName("Valid id with hyphens and underscores is accepted")
        void validIdWithHyphenUnderscore() {
            assertDoesNotThrow(() -> Student.validateStudentId("CS-2024_01"));
        }

        @Test @DisplayName("Null id throws IllegalArgumentException")
        void nullIdThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateStudentId(null));
        }

        @Test @DisplayName("Blank id throws IllegalArgumentException")
        void blankIdThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateStudentId("   "));
        }

        @Test @DisplayName("Empty string id throws IllegalArgumentException")
        void emptyIdThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateStudentId(""));
        }

        @Test @DisplayName("Id with spaces throws (spaces not allowed)")
        void idWithSpaceThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateStudentId("CS 2024"));
        }

        @Test @DisplayName("Id with special characters (@#$) throws")
        void idWithSpecialCharsThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateStudentId("CS@2024#01"));
        }

        @Test @DisplayName("Id longer than 30 characters throws")
        void idTooLongThrows() {
            String longId = "A".repeat(31);
            assertThrows(IllegalArgumentException.class, () -> Student.validateStudentId(longId));
        }

        @Test @DisplayName("Id exactly 30 characters is accepted")
        void idExactly30Chars() {
            String id30 = "A".repeat(30);
            assertDoesNotThrow(() -> Student.validateStudentId(id30));
        }

        @Test @DisplayName("Id with only digits is accepted")
        void idAllDigits() {
            assertDoesNotThrow(() -> Student.validateStudentId("12345"));
        }

        @Test @DisplayName("Returned id is trimmed of whitespace")
        void idIsTrimmed() {
            assertEquals("CS001", Student.validateStudentId("  CS001  "));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // name
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("name validation")
    class NameTests {

        @Test @DisplayName("Valid single name is accepted")
        void validSingleName() {
            assertDoesNotThrow(() -> Student.validateName("Alice"));
        }

        @Test @DisplayName("Valid full name with space is accepted")
        void validFullName() {
            assertDoesNotThrow(() -> Student.validateName("Alice Kumar"));
        }

        @Test @DisplayName("Name with hyphen is accepted")
        void nameWithHyphen() {
            assertDoesNotThrow(() -> Student.validateName("Anne-Marie"));
        }

        @Test @DisplayName("Name with apostrophe is accepted (O'Brien)")
        void nameWithApostrophe() {
            assertDoesNotThrow(() -> Student.validateName("O'Brien"));
        }

        @Test @DisplayName("Name with dot is accepted (Dr. Smith)")
        void nameWithDot() {
            assertDoesNotThrow(() -> Student.validateName("Dr. Smith"));
        }

        @Test @DisplayName("Null name throws IllegalArgumentException")
        void nullNameThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateName(null));
        }

        @Test @DisplayName("Blank name throws IllegalArgumentException")
        void blankNameThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateName("   "));
        }

        @Test @DisplayName("Purely numeric name throws (names must be strings starting with letter)")
        void numericNameThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateName("12345"));
        }

        @Test @DisplayName("Name starting with a digit throws")
        void nameStartingWithDigitThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateName("1Alice"));
        }

        @Test @DisplayName("Name starting with a special character throws")
        void nameStartingWithSpecialCharThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateName("@Alice"));
        }

        @Test @DisplayName("Name with special chars like @ or # throws")
        void nameWithHashThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateName("Alice#Kumar"));
        }

        @Test @DisplayName("Name longer than 100 characters throws")
        void nameTooLongThrows() {
            String longName = "A" + "b".repeat(100); // 101 chars
            assertThrows(IllegalArgumentException.class, () -> Student.validateName(longName));
        }

        @Test @DisplayName("Name exactly 100 characters is accepted")
        void nameExactly100Chars() {
            String name100 = "A" + "b".repeat(99); // 100 chars
            assertDoesNotThrow(() -> Student.validateName(name100));
        }

        @Test @DisplayName("Returned name is trimmed")
        void nameIsTrimmed() {
            assertEquals("Alice", Student.validateName("  Alice  "));
        }

        @Test @DisplayName("Name with only spaces throws")
        void nameOnlySpacesThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateName("     "));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // email
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("email validation")
    class EmailTests {

        @Test @DisplayName("Valid standard email is accepted")
        void validEmail() {
            assertDoesNotThrow(() -> Student.validateEmail("alice@example.com"));
        }

        @Test @DisplayName("Valid email with subdomain is accepted")
        void validEmailSubdomain() {
            assertDoesNotThrow(() -> Student.validateEmail("student@cs.university.edu"));
        }

        @Test @DisplayName("Null email throws")
        void nullEmailThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateEmail(null));
        }

        @Test @DisplayName("Blank email throws")
        void blankEmailThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateEmail("   "));
        }

        @Test @DisplayName("Email without '@' throws")
        void emailNoAtThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateEmail("aliceexample.com"));
        }

        @Test @DisplayName("Email with '@' at start (no local part) throws")
        void emailAtStartThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateEmail("@example.com"));
        }

        @Test @DisplayName("Email with two '@' symbols throws")
        void emailTwoAtThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateEmail("a@b@c.com"));
        }

        @Test @DisplayName("Email with domain missing '.' throws")
        void emailDomainNoDotThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateEmail("alice@localhost"));
        }

        @Test @DisplayName("Email with empty domain throws")
        void emailEmptyDomainThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateEmail("alice@"));
        }

        @Test @DisplayName("Returned email is trimmed")
        void emailIsTrimmed() {
            assertEquals("a@b.com", Student.validateEmail("  a@b.com  "));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // department
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("department validation")
    class DepartmentTests {

        @Test @DisplayName("Valid department name is accepted")
        void validDepartment() {
            assertDoesNotThrow(() -> Student.validateDepartment("Computer Science"));
        }

        @Test @DisplayName("Null department throws")
        void nullDeptThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateDepartment(null));
        }

        @Test @DisplayName("Blank department throws")
        void blankDeptThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateDepartment("   "));
        }

        @Test @DisplayName("Purely numeric department throws (must not be a number)")
        void numericDeptThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateDepartment("12345"));
        }

        @Test @DisplayName("Department longer than 80 characters throws")
        void deptTooLongThrows() {
            String longDept = "D".repeat(81);
            assertThrows(IllegalArgumentException.class, () -> Student.validateDepartment(longDept));
        }

        @Test @DisplayName("Department exactly 80 characters is accepted")
        void deptExactly80() {
            assertDoesNotThrow(() -> Student.validateDepartment("D".repeat(80)));
        }

        @Test @DisplayName("Department is trimmed before storage")
        void deptIsTrimmed() {
            assertEquals("EE", Student.validateDepartment("  EE  "));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // semester
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("semester validation")
    class SemesterTests {

        @Test @DisplayName("Semester 1 is valid (lower bound)")
        void semester1Valid() {
            assertDoesNotThrow(() -> Student.validateSemester(1));
        }

        @Test @DisplayName("Semester 12 is valid (upper bound)")
        void semester12Valid() {
            assertDoesNotThrow(() -> Student.validateSemester(12));
        }

        @Test @DisplayName("Semester 6 is valid (mid-range)")
        void semester6Valid() {
            assertDoesNotThrow(() -> Student.validateSemester(6));
        }

        @Test @DisplayName("Semester 0 throws (below range)")
        void semester0Throws() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateSemester(0));
        }

        @Test @DisplayName("Semester -1 throws (negative)")
        void semesterNegativeThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateSemester(-1));
        }

        @Test @DisplayName("Semester 13 throws (above range)")
        void semester13Throws() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateSemester(13));
        }

        @Test @DisplayName("Semester 100 throws")
        void semester100Throws() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateSemester(100));
        }

        @Test @DisplayName("Integer.MAX_VALUE throws")
        void semesterMaxIntThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validateSemester(Integer.MAX_VALUE));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // password
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("password validation")
    class PasswordTests {

        @Test @DisplayName("Password with 6 characters is accepted (minimum)")
        void passwordMinLength() {
            assertDoesNotThrow(() -> Student.validatePassword("abc123"));
        }

        @Test @DisplayName("Password with more than 6 characters is accepted")
        void passwordLong() {
            assertDoesNotThrow(() -> Student.validatePassword("securePass2024!"));
        }

        @Test @DisplayName("Null password throws")
        void nullPasswordThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validatePassword(null));
        }

        @Test @DisplayName("Empty string password throws")
        void emptyPasswordThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validatePassword(""));
        }

        @Test @DisplayName("Password with 5 characters throws (below minimum)")
        void passwordTooShortThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validatePassword("ab12"));
        }

        @Test @DisplayName("Password with exactly 5 characters throws")
        void password5CharsThrows() {
            assertThrows(IllegalArgumentException.class, () -> Student.validatePassword("abcde"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // collegeName
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("collegeName default fallback")
    class CollegeNameTests {

        @Test @DisplayName("Null collegeName in constructor defaults to 'Unknown College'")
        void nullCollegeDefaults() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", null);
            assertEquals("Unknown College", s.collegeName);
        }

        @Test @DisplayName("Blank collegeName in constructor defaults to 'Unknown College'")
        void blankCollegeDefaults() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "   ");
            assertEquals("Unknown College", s.collegeName);
        }

        @Test @DisplayName("Valid collegeName is stored correctly")
        void validCollegeStored() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "IIT Delhi");
            assertEquals("IIT Delhi", s.collegeName);
        }

        @Test @DisplayName("College name is trimmed before storage")
        void collegeIsTrimmed() {
            Student s = new Student("S001", "Alice", "a@b.com", "CS", 1, "pass12", "  MIT  ");
            assertEquals("MIT", s.collegeName);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Full constructor — combined validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Full constructor combined validation")
    class FullConstructorTests {

        @Test @DisplayName("Valid student is created without exception")
        void validStudentCreated() {
            assertDoesNotThrow(() ->
                new Student("CS001", "Alice Kumar", "alice@test.com", "Computer Science", 3, "pass12", "Test Uni")
            );
        }

        @Test @DisplayName("Invalid studentId in constructor throws")
        void invalidIdInConstructorThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                new Student("CS 001", "Alice", "a@b.com", "CS", 1, "pass12", "Uni")
            );
        }

        @Test @DisplayName("Numeric-only name in constructor throws")
        void numericNameInConstructorThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                new Student("S001", "12345", "a@b.com", "CS", 1, "pass12", "Uni")
            );
        }

        @Test @DisplayName("Invalid email in constructor throws")
        void invalidEmailInConstructorThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                new Student("S001", "Alice", "notanemail", "CS", 1, "pass12", "Uni")
            );
        }

        @Test @DisplayName("Semester 0 in constructor throws")
        void invalidSemesterInConstructorThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                new Student("S001", "Alice", "a@b.com", "CS", 0, "pass12", "Uni")
            );
        }

        @Test @DisplayName("Short password in constructor throws")
        void shortPasswordInConstructorThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                new Student("S001", "Alice", "a@b.com", "CS", 1, "123", "Uni")
            );
        }

        @Test @DisplayName("All fields are correctly stored after valid construction")
        void allFieldsStoredCorrectly() {
            Student s = new Student("CS-001", "Bob Lee", "bob@uni.edu", "Electrical Engineering", 4, "mypass1", "Top Uni");
            assertEquals("CS-001", s.studentId);
            assertEquals("Bob Lee", s.name);
            assertEquals("bob@uni.edu", s.email);
            assertEquals("Electrical Engineering", s.department);
            assertEquals(4, s.semester);
            assertEquals("mypass1", s.password);
            assertEquals("Top Uni", s.collegeName);
        }
    }
}
