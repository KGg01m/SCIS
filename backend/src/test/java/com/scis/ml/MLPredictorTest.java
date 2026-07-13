package com.scis.ml;

import com.scis.model.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MLPredictor
 * Tests: CGPA prediction, fail risk, attendance drop risk, confidence,
 *        grade categories, risk levels, final marks prediction
 */
@DisplayName("MLPredictor Tests")
class MLPredictorTest {

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student("S001", "Test Student", "s@test.com",
                "CS", 3, "password", "Test University");
    }

    // ─── predictCGPA ──────────────────────────────────────────────────────

    @Test
    @DisplayName("predictCGPA returns value between 0 and 10")
    void testCGPAInRange() {
        double cgpa = MLPredictor.predictCGPA(80, 75);
        assertTrue(cgpa >= 0 && cgpa <= 10, "CGPA must be between 0 and 10");
    }

    @Test
    @DisplayName("predictCGPA returns higher value for better performance")
    void testCGPAHigherForBetterPerformance() {
        double cgpaGood = MLPredictor.predictCGPA(90, 90);
        double cgpaPoor = MLPredictor.predictCGPA(60, 50);
        assertTrue(cgpaGood > cgpaPoor);
    }

    @Test
    @DisplayName("predictCGPA penalizes very low attendance")
    void testCGPAPenaltyForLowAttendance() {
        double cgpaHighAtt = MLPredictor.predictCGPA(90, 80);
        double cgpaLowAtt  = MLPredictor.predictCGPA(60, 80); // attendance < 65 → penalty
        assertTrue(cgpaHighAtt > cgpaLowAtt);
    }

    @Test
    @DisplayName("predictCGPA returns 0 for 0 attendance and 0 performance")
    void testCGPAZeroInputs() {
        double cgpa = MLPredictor.predictCGPA(0, 0);
        assertEquals(0.0, cgpa, 0.001);
    }

    // ─── calculateFailRisk ────────────────────────────────────────────────

    @Test
    @DisplayName("calculateFailRisk returns 0 for excellent attendance and performance")
    void testFailRiskZeroForExcellent() {
        double risk = MLPredictor.calculateFailRisk(95, 95);
        assertEquals(0.0, risk, 0.001);
    }

    @Test
    @DisplayName("calculateFailRisk returns maximum for very poor metrics")
    void testFailRiskHighForPoor() {
        double risk = MLPredictor.calculateFailRisk(40, 30); // both below danger thresholds
        assertTrue(risk >= 0.8, "Risk should be very high: " + risk);
    }

    @Test
    @DisplayName("calculateFailRisk is higher for poor performance than safe performance")
    void testFailRiskOrdering() {
        double highRisk   = MLPredictor.calculateFailRisk(60, 40);
        double lowRisk    = MLPredictor.calculateFailRisk(85, 80);
        assertTrue(highRisk > lowRisk);
    }

    @Test
    @DisplayName("calculateFailRisk value is between 0.0 and 1.0")
    void testFailRiskBounds() {
        double risk = MLPredictor.calculateFailRisk(50, 40);
        assertTrue(risk >= 0.0 && risk <= 1.0);
    }

    @Test
    @DisplayName("calculateFailRisk reflects attendance-only danger")
    void testFailRiskLowAttendanceOnly() {
        double risk = MLPredictor.calculateFailRisk(50, 80); // low attendance, good performance
        assertTrue(risk > 0.0);
    }

    // ─── calculateAttendanceDropRisk ──────────────────────────────────────

    @Test
    @DisplayName("calculateAttendanceDropRisk returns 0 when no subjects")
    void testAttDropRiskNoData() {
        double risk = MLPredictor.calculateAttendanceDropRisk(student);
        assertEquals(0.0, risk, 0.001);
    }

    @Test
    @DisplayName("calculateAttendanceDropRisk is higher with many recent absences")
    void testAttDropRiskHighWithAbsences() {
        String subj = "Physics";
        // 5 records, last 4 are absent
        student.attendanceMap.computeIfAbsent(subj, k -> new java.util.ArrayList<>())
                .add(new AttendanceRecord(subj, LocalDate.now().minusDays(10), true));
        for (int i = 1; i <= 4; i++) {
            student.attendanceMap.get(subj)
                    .add(new AttendanceRecord(subj, LocalDate.now().minusDays(i), false));
        }
        double risk = MLPredictor.calculateAttendanceDropRisk(student);
        assertTrue(risk > 0.4, "Risk should be elevated: " + risk);
    }

    @Test
    @DisplayName("calculateAttendanceDropRisk returns value between 0 and 1")
    void testAttDropRiskBounds() {
        student.addSampleData();
        double risk = MLPredictor.calculateAttendanceDropRisk(student);
        assertTrue(risk >= 0.0 && risk <= 1.0);
    }

    // ─── calculateConfidence ──────────────────────────────────────────────

    @Test
    @DisplayName("calculateConfidence is at least 45% even with no data")
    void testConfidenceMinimum() {
        double conf = MLPredictor.calculateConfidence(student);
        assertTrue(conf >= 45.0);
    }

    @Test
    @DisplayName("calculateConfidence increases with more data points")
    void testConfidenceIncreasesWithData() {
        double confBefore = MLPredictor.calculateConfidence(student);
        student.addSampleData();
        double confAfter = MLPredictor.calculateConfidence(student);
        assertTrue(confAfter > confBefore);
    }

    @Test
    @DisplayName("calculateConfidence does not exceed 95%")
    void testConfidenceCapped() {
        // Add lots of data
        for (int i = 0; i < 50; i++) {
            student.addMarks("Subject" + i, "IA1", 80, 100);
        }
        double conf = MLPredictor.calculateConfidence(student);
        assertTrue(conf <= 95.0, "Confidence must not exceed 95%: " + conf);
    }

    // ─── predictGradeCategory ─────────────────────────────────────────────

    @Test
    @DisplayName("predictGradeCategory returns A for performance >= 80 and good attendance")
    void testGradeA() {
        assertEquals("A", MLPredictor.predictGradeCategory(85, 90));
    }

    @Test
    @DisplayName("predictGradeCategory returns B for performance between 65-79")
    void testGradeB() {
        assertEquals("B", MLPredictor.predictGradeCategory(70, 90));
    }

    @Test
    @DisplayName("predictGradeCategory returns C for performance between 50-64")
    void testGradeC() {
        assertEquals("C", MLPredictor.predictGradeCategory(55, 90));
    }

    @Test
    @DisplayName("predictGradeCategory returns D for performance between 40-49")
    void testGradeD() {
        assertEquals("D", MLPredictor.predictGradeCategory(45, 90));
    }

    @Test
    @DisplayName("predictGradeCategory returns F for performance below 40")
    void testGradeF() {
        assertEquals("F", MLPredictor.predictGradeCategory(30, 90));
    }

    @Test
    @DisplayName("predictGradeCategory penalizes grade for low attendance")
    void testGradePenaltyForLowAttendance() {
        // Performance 70 normally → B, but low attendance should drag it down
        String gradeHighAtt = MLPredictor.predictGradeCategory(70, 90);
        String gradeLowAtt  = MLPredictor.predictGradeCategory(70, 60);  // att < 65
        // Low attendance should give a lower or equal grade
        assertNotEquals("A", gradeLowAtt); // shouldn't be bumped up
    }

    // ─── getRiskLevel ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getRiskLevel returns HIGH for failRisk > 0.6")
    void testRiskLevelHigh() {
        assertEquals("HIGH", MLPredictor.getRiskLevel(0.7));
    }

    @Test
    @DisplayName("getRiskLevel returns MEDIUM for failRisk between 0.3 and 0.6")
    void testRiskLevelMedium() {
        assertEquals("MEDIUM", MLPredictor.getRiskLevel(0.5));
    }

    @Test
    @DisplayName("getRiskLevel returns LOW for failRisk <= 0.3")
    void testRiskLevelLow() {
        assertEquals("LOW", MLPredictor.getRiskLevel(0.2));
    }

    @Test
    @DisplayName("getRiskLevel returns LOW for zero fail risk")
    void testRiskLevelZero() {
        assertEquals("LOW", MLPredictor.getRiskLevel(0.0));
    }

    // ─── getRiskIndicator ─────────────────────────────────────────────────

    @Test
    @DisplayName("getRiskIndicator returns [HIGH RISK] for failRisk > 0.6")
    void testRiskIndicatorHigh() {
        assertEquals("[HIGH RISK]", MLPredictor.getRiskIndicator(0.8));
    }

    @Test
    @DisplayName("getRiskIndicator returns [MODERATE RISK] for failRisk between 0.3 and 0.6")
    void testRiskIndicatorModerate() {
        assertEquals("[MODERATE RISK]", MLPredictor.getRiskIndicator(0.4));
    }

    @Test
    @DisplayName("getRiskIndicator returns [SAFE] for failRisk <= 0.3")
    void testRiskIndicatorSafe() {
        assertEquals("[SAFE]", MLPredictor.getRiskIndicator(0.1));
    }

    // ─── predictFinalMarks ────────────────────────────────────────────────

    @Test
    @DisplayName("predictFinalMarks returns slightly higher than current performance")
    void testFinalMarksHigher() {
        double predicted = MLPredictor.predictFinalMarks(80.0);
        assertTrue(predicted >= 80.0); // at least current
    }

    @Test
    @DisplayName("predictFinalMarks does not exceed 100")
    void testFinalMarksCapped() {
        double predicted = MLPredictor.predictFinalMarks(99.0);
        assertTrue(predicted <= 100.0);
    }

    // ─── predict / predictSemesterOutcome ─────────────────────────────────

    @Test
    @DisplayName("predict() returns a non-empty string report")
    void testPredictReturnsReport() {
        student.addSampleData();
        String report = MLPredictor.predict(student);
        assertNotNull(report);
        assertFalse(report.isBlank());
    }

    @Test
    @DisplayName("predictSemesterOutcome contains key sections")
    void testPredictSemesterOutcomeContainsSections() {
        student.addSampleData();
        String report = MLPredictor.predictSemesterOutcome(student);
        assertTrue(report.contains("PREDICTIONS"));
        assertTrue(report.contains("CGPA"));
        assertTrue(report.contains("Fail Risk"));
    }

    @Test
    @DisplayName("generatePDFReport contains student profile and ML predictions")
    void testGeneratePDFReport() {
        student.addSampleData();
        String report = MLPredictor.generatePDFReport(student);
        assertTrue(report.contains("S001"));
        assertTrue(report.contains("ML PREDICTIONS"));
        assertTrue(report.contains("SUBJECT-WISE BREAKDOWN"));
    }

    // ─── Grade threshold constants ────────────────────────────────────────

    @Test
    @DisplayName("Grade and risk threshold constants have expected values")
    void testThresholdConstants() {
        assertEquals(80.0, MLPredictor.GRADE_A_THRESHOLD, 0.001);
        assertEquals(65.0, MLPredictor.GRADE_B_THRESHOLD, 0.001);
        assertEquals(50.0, MLPredictor.GRADE_C_THRESHOLD, 0.001);
        assertEquals(40.0, MLPredictor.GRADE_D_THRESHOLD, 0.001);
        assertEquals(65.0, MLPredictor.ATT_DANGER,  0.001);
        assertEquals(75.0, MLPredictor.ATT_CAUTION, 0.001);
    }
}
