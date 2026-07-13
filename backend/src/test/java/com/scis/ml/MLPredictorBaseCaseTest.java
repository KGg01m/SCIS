package com.scis.ml;

import com.scis.model.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended base-case tests for MLPredictor.
 *
 * Covers boundaries, edge inputs, and numeric contracts not in the
 * original MLPredictorTest.
 */
@DisplayName("MLPredictor Base Case Tests")
class MLPredictorBaseCaseTest {

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student("ML001", "Test Student", "t@test.com", "CS", 3, "pass12", "Uni");
    }

    // ═══════════════════════════════════════════════════════════════
    // predictCGPA — boundary values
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("predictCGPA – Boundary Values")
    class PredictCGPATests {

        @Test @DisplayName("0% attendance, 0% performance → CGPA = 0.0")
        void zeroInputsGiveZeroCGPA() {
            assertEquals(0.0, MLPredictor.predictCGPA(0, 0), 0.001);
        }

        @Test @DisplayName("100% attendance, 100% performance → CGPA is at or near 10.0")
        void perfectInputsGivePerfectCGPA() {
            double cgpa = MLPredictor.predictCGPA(100, 100);
            assertTrue(cgpa >= 9.0 && cgpa <= 10.0,
                "Expected near-perfect CGPA, got " + cgpa);
        }

        @Test @DisplayName("Result is always in [0.0, 10.0] for any double inputs")
        void alwaysInRange() {
            double[][] cases = {{0,0},{50,50},{75,60},{100,100},{110,110},{-10,-10},{200,200}};
            for (double[] c : cases) {
                double v = MLPredictor.predictCGPA(c[0], c[1]);
                assertTrue(v >= 0 && v <= 10,
                    "CGPA out of range for att=" + c[0] + " perf=" + c[1] + ": " + v);
            }
        }

        @Test @DisplayName("Performance weight (0.7) dominates attendance weight (0.3)")
        void performanceWeightedMoreThanAttendance() {
            // Same average but performance high vs attendance high
            double highPerf = MLPredictor.predictCGPA(70, 90);
            double highAtt  = MLPredictor.predictCGPA(90, 70);
            assertTrue(highPerf > highAtt,
                "Higher performance should give higher CGPA (weight 0.7 > 0.3)");
        }

        @Test @DisplayName("Attendance exactly at ATT_DANGER threshold is NOT penalised by 0.88 factor")
        void attendanceAtDangerNotDoublePenalised() {
            double atDanger    = MLPredictor.predictCGPA(MLPredictor.ATT_DANGER, 80);
            double belowDanger = MLPredictor.predictCGPA(MLPredictor.ATT_DANGER - 1, 80);
            assertTrue(atDanger > belowDanger,
                "At-danger attendance should not incur the <65 penalty");
        }

        @Test @DisplayName("CGPA is monotonically non-decreasing with better attendance (same perf)")
        void cgpaIncreasesWithAttendance() {
            double low  = MLPredictor.predictCGPA(60,  80);
            double mid  = MLPredictor.predictCGPA(75,  80);
            double high = MLPredictor.predictCGPA(95,  80);
            assertTrue(low <= mid && mid <= high,
                "CGPA should be monotonically non-decreasing with attendance");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // calculateFailRisk — boundary values
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateFailRisk – Boundary Values")
    class FailRiskTests {

        @Test @DisplayName("Perfect metrics → risk = 0.0")
        void perfectNoRisk() {
            assertEquals(0.0, MLPredictor.calculateFailRisk(100, 100), 0.001);
        }

        @Test @DisplayName("Worst-case inputs → risk = 1.0 (capped)")
        void worstCaseMaxRisk() {
            assertEquals(1.0, MLPredictor.calculateFailRisk(0, 0), 0.001);
        }

        @Test @DisplayName("Risk is never negative")
        void riskNeverNegative() {
            assertTrue(MLPredictor.calculateFailRisk(100, 100) >= 0.0);
        }

        @Test @DisplayName("Risk is never above 1.0")
        void riskNeverAboveOne() {
            assertTrue(MLPredictor.calculateFailRisk(0, 0) <= 1.0);
        }

        @Test @DisplayName("Attendance exactly at ATT_CAUTION (75) → no attendance-risk contribution")
        void attendanceAtCautionNoPenalty() {
            double atCaution    = MLPredictor.calculateFailRisk(MLPredictor.ATT_CAUTION, 80);
            double belowCaution = MLPredictor.calculateFailRisk(MLPredictor.ATT_CAUTION - 1, 80);
            assertTrue(atCaution < belowCaution,
                "Risk at caution threshold should be lower than below it");
        }

        @Test @DisplayName("Low attendance alone adds to risk even with good performance")
        void lowAttendanceAloneAddsRisk() {
            double riskLowAtt  = MLPredictor.calculateFailRisk(50, 90);
            double riskHighAtt = MLPredictor.calculateFailRisk(90, 90);
            assertTrue(riskLowAtt > riskHighAtt);
        }

        @Test @DisplayName("Low performance alone adds to risk even with good attendance")
        void lowPerformanceAloneAddsRisk() {
            double riskLowPerf  = MLPredictor.calculateFailRisk(90, 30);
            double riskHighPerf = MLPredictor.calculateFailRisk(90, 90);
            assertTrue(riskLowPerf > riskHighPerf);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // calculateAttendanceDropRisk — edge cases
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateAttendanceDropRisk – Edge Cases")
    class AttDropRiskTests {

        @Test @DisplayName("Student with no subjects → risk = 0.0")
        void noSubjectsZeroRisk() {
            assertEquals(0.0, MLPredictor.calculateAttendanceDropRisk(student), 0.001);
        }

        @Test @DisplayName("Subject with < 3 records is skipped (risk stays 0.0)")
        void fewerThan3RecordsSkipped() {
            student.addSubject("Maths");
            student.attendanceMap.get("Maths")
                    .add(new AttendanceRecord("Maths", LocalDate.now(), true));
            student.attendanceMap.get("Maths")
                    .add(new AttendanceRecord("Maths", LocalDate.now().minusDays(1), true));
            assertEquals(0.0, MLPredictor.calculateAttendanceDropRisk(student), 0.001);
        }

        @Test @DisplayName("All recent classes absent → high drop risk")
        void allAbsentRecentlyHighRisk() {
            student.addSubject("Physics");
            for (int i = 0; i < 5; i++)
                student.attendanceMap.get("Physics")
                        .add(new AttendanceRecord("Physics", LocalDate.now().minusDays(i), false));
            double risk = MLPredictor.calculateAttendanceDropRisk(student);
            assertTrue(risk > 0.4, "Expected high risk, got " + risk);
        }

        @Test @DisplayName("All recent classes present → low drop risk")
        void allPresentLowDropRisk() {
            student.addSubject("Chemistry");
            for (int i = 0; i < 10; i++)
                student.attendanceMap.get("Chemistry")
                        .add(new AttendanceRecord("Chemistry", LocalDate.now().minusDays(i), true));
            double risk = MLPredictor.calculateAttendanceDropRisk(student);
            assertTrue(risk < 0.5, "Expected low risk, got " + risk);
        }

        @Test @DisplayName("Result is always in [0.0, 1.0]")
        void riskAlwaysInRange() {
            student.addSampleData();
            double risk = MLPredictor.calculateAttendanceDropRisk(student);
            assertTrue(risk >= 0.0 && risk <= 1.0, "Risk out of range: " + risk);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // calculateConfidence — edge cases
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateConfidence – Edge Cases")
    class ConfidenceTests {

        @Test @DisplayName("Student with no data starts at base confidence = 45.0")
        void emptyStudentBaseConfidence() {
            assertEquals(45.0, MLPredictor.calculateConfidence(student), 0.001);
        }

        @Test @DisplayName("More data points → higher confidence")
        void moreDataHigherConfidence() {
            double before = MLPredictor.calculateConfidence(student);
            student.addSampleData();
            double after = MLPredictor.calculateConfidence(student);
            assertTrue(after > before);
        }

        @Test @DisplayName("Confidence is capped at 95.0 regardless of data volume")
        void confidenceCappedAt95() {
            student.addSubject("Maths");
            for (int i = 0; i < 100; i++) {
                student.attendanceMap.get("Maths")
                        .add(new AttendanceRecord("Maths", LocalDate.now().minusDays(i), true));
                student.addMarks("Maths", "IA" + i, 80, 100);
            }
            assertTrue(MLPredictor.calculateConfidence(student) <= 95.0);
        }

        @Test @DisplayName("Confidence is always >= 0.0")
        void confidenceAlwaysNonNegative() {
            assertTrue(MLPredictor.calculateConfidence(student) >= 0.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // predictGradeCategory — all grade boundaries
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("predictGradeCategory – All Grade Boundaries")
    class GradeCategoryTests {

        @Test @DisplayName("Performance >= 80 with good attendance → Grade A")
        void gradeA() {
            assertEquals("A", MLPredictor.predictGradeCategory(80.0, 90.0));
            assertEquals("A", MLPredictor.predictGradeCategory(100.0, 90.0));
        }

        @Test @DisplayName("Performance 65–79 with good attendance → Grade B")
        void gradeB() {
            assertEquals("B", MLPredictor.predictGradeCategory(65.0, 90.0));
            assertEquals("B", MLPredictor.predictGradeCategory(79.0, 90.0));
        }

        @Test @DisplayName("Performance 50–64 with good attendance → Grade C")
        void gradeC() {
            assertEquals("C", MLPredictor.predictGradeCategory(50.0, 90.0));
            assertEquals("C", MLPredictor.predictGradeCategory(64.0, 90.0));
        }

        @Test @DisplayName("Performance 40–49 with good attendance → Grade D")
        void gradeD() {
            assertEquals("D", MLPredictor.predictGradeCategory(40.0, 90.0));
            assertEquals("D", MLPredictor.predictGradeCategory(49.0, 90.0));
        }

        @Test @DisplayName("Performance < 40 → Grade F")
        void gradeF() {
            assertEquals("F", MLPredictor.predictGradeCategory(39.0, 90.0));
            assertEquals("F", MLPredictor.predictGradeCategory(0.0,  90.0));
        }

        @Test @DisplayName("Very low attendance applies attendance penalty and may downgrade grade")
        void lowAttendanceDowngradesGrade() {
            // 82% perf = Grade A normally, but low attendance applies penalty
            String goodAtt = MLPredictor.predictGradeCategory(82.0, 90.0);
            String lowAtt  = MLPredictor.predictGradeCategory(82.0, 60.0); // attendance < 65 → penalty 0.85
            // After penalty: 82 * 0.85 = 69.7 → Grade B (not A)
            // Good attendance must yield equal or better grade than low attendance
            assertTrue(goodAtt.compareTo(lowAtt) <= 0,
                "Good attendance grade (" + goodAtt + ") should be <= low att grade (" + lowAtt + ") alphabetically");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // getRiskLevel — string mapping
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRiskLevel – String Return Values")
    class RiskLevelTests {

        @Test @DisplayName("failRisk > 0.6 → 'HIGH' (not 'High' or 'high')")
        void highRiskIsUpperCase() {
            assertEquals("HIGH", MLPredictor.getRiskLevel(0.61));
        }

        @Test @DisplayName("failRisk 0.3 to 0.6 → 'MEDIUM'")
        void mediumRisk() {
            assertEquals("MEDIUM", MLPredictor.getRiskLevel(0.30));
            assertEquals("MEDIUM", MLPredictor.getRiskLevel(0.60));
        }

        @Test @DisplayName("failRisk < 0.3 → 'LOW'")
        void lowRisk() {
            assertEquals("LOW", MLPredictor.getRiskLevel(0.0));
            assertEquals("LOW", MLPredictor.getRiskLevel(0.29));
        }

        @Test @DisplayName("getRiskLevel returns a non-null, non-empty String")
        void riskLevelIsNonNullString() {
            String level = MLPredictor.getRiskLevel(0.5);
            assertNotNull(level);
            assertFalse(level.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // predictFinalMarks
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("predictFinalMarks – Edge Cases")
    class PredictFinalMarksTests {

        @Test @DisplayName("Result is capped at 100.0")
        void resultCappedAt100() {
            double result = MLPredictor.predictFinalMarks(100.0);
            assertTrue(result <= 100.0);
        }

        @Test @DisplayName("0% performance predicts a non-negative final mark")
        void zeroPerfNonNegative() {
            assertTrue(MLPredictor.predictFinalMarks(0.0) >= 0.0);
        }

        @Test @DisplayName("Higher performance predicts higher final marks")
        void higherPerfHigherPrediction() {
            double low  = MLPredictor.predictFinalMarks(40.0);
            double high = MLPredictor.predictFinalMarks(80.0);
            assertTrue(high > low);
        }
    }
}
