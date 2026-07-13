// MarksRecord - stores a single marks/test entry for a student
package com.scis.model;
import java.io.Serializable;

public class MarksRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    public String subject;
    public String testType;
    public double marksObtained;
    public double maxMarks;

    public MarksRecord() {}

    public MarksRecord(String subject, String testType, double obtained, double max) {
        this.subject = subject;
        this.testType = testType;
        this.marksObtained = obtained;
        this.maxMarks = max;
    }
}
