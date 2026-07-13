// ClassRoom - represents a classroom with unique code for teacher-student linkage
package com.scis.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClassRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    public String classCode;
    public String subjectName;
    public String teacherId;
    public List<String> enrolledStudentIds = new ArrayList<>();

    public ClassRoom() {}

    public ClassRoom(String classCode, String subjectName, String teacherId) {
        if (classCode == null || classCode.trim().isEmpty())
            throw new IllegalArgumentException("classCode must not be blank");
        if (subjectName == null || subjectName.trim().isEmpty())
            throw new IllegalArgumentException("subjectName must not be blank");
        if (teacherId == null || teacherId.trim().isEmpty())
            throw new IllegalArgumentException("teacherId must not be blank");
        this.classCode   = classCode.trim().toUpperCase();
        this.subjectName = subjectName.trim();
        this.teacherId   = teacherId.trim();
    }
}
