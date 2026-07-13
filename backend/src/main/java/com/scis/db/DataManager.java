package com.scis.db;

import com.scis.auth.PasswordUtils;
import com.scis.model.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.time.LocalDate;
import java.util.*;
/**
 * DataManager -- MongoDB persistence layer for SCIS
 *
 * Database  : smartcampus
 * Collection: students  (one document per student)
 *
 * Each student document embeds all attendance records, marks, and tasks
 * as nested arrays directly inside the student document. This gives fast
 * single-document reads and writes for the typical single-student session.
 */
public class DataManager {
    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME   = "smartcampus";
    private static final String COLL_NAME = "students";
    private static MongoClient               mongoClient;
    private static MongoCollection<Document> collection;
    
    /** Result class for email existence checking */
    public static class EmailCheckResult {
        public final boolean exists;
        public final String existingStudentId;
        
        public EmailCheckResult(boolean exists, String existingStudentId) {
            this.exists = exists;
            this.existingStudentId = existingStudentId;
        }
    }
    
    static {
        try {
            mongoClient = MongoClients.create(MONGO_URI);
            collection  = mongoClient.getDatabase(DB_NAME).getCollection(COLL_NAME);
            collection.createIndex(Indexes.ascending("studentId"), new IndexOptions().unique(true));
            collection.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
            System.out.println("[DataManager] Connected -> " + DB_NAME + "." + COLL_NAME);
        } catch (Exception e) {
            System.err.println("[DataManager] MongoDB init failed: " + e.getMessage());
        }
    }
    
    // === Helper Methods =========================================================
    /**
     * Normalizes student ID to lowercase for case-insensitive handling.
     * All student IDs will be stored and queried in lowercase.
     */
    private static String normalizeStudentId(String studentId) {
        return studentId != null ? studentId.toLowerCase().trim() : null;
    }
    
    /**
     * Checks if an email already exists in the database (both student and teacher collections).
     * @param email The email to check (should be normalized to lowercase)
     * @param excludeStudentId Student ID to exclude from the check (for updates)
     * @return EmailCheckResult indicating if email exists and which student owns it
     */
    public static EmailCheckResult checkEmailExists(String email, String excludeStudentId) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            
            // Check student collection first
            Document existing = collection.find(Filters.eq("email", normalizedEmail)).first();
            if (existing != null) {
                String existingStudentId = existing.getString("studentId");
                // If we're excluding a student ID (for updates), don't count it as a conflict
                if (excludeStudentId != null && excludeStudentId.equalsIgnoreCase(existingStudentId)) {
                    return new EmailCheckResult(false, null);
                }
                return new EmailCheckResult(true, existingStudentId);
            }
            
            // Check teacher collection directly to prevent cross-type email duplication
            try {
                MongoCollection<Document> teacherCollection = mongoClient.getDatabase("smartcampus").getCollection("teachers");
                Document teacherExisting = teacherCollection.find(Filters.eq("email", normalizedEmail)).first();
                if (teacherExisting != null) {
                    return new EmailCheckResult(true, "TEACHER_" + teacherExisting.getString("teacherId"));
                }
            } catch (Exception ex) {
                System.err.println("[DataManager] Teacher email check failed: " + ex.getMessage());
            }
            
            return new EmailCheckResult(false, null);
        } catch (Exception e) {
            System.err.println("[DataManager] checkEmailExists: " + e.getMessage());
            return new EmailCheckResult(false, null);
        }
    }
    
    // === Public API ===========================================================
    public static boolean studentExists(String studentId) {
        try { 
            String normalizedId = normalizeStudentId(studentId);
            return collection.find(Filters.eq("studentId", normalizedId)).first() != null; 
        }
        catch (Exception e) { return false; }
    }
    public static void saveStudent(Student student) {
        if (student == null) {
            System.err.println("[DataManager] saveStudent: student is null");
            return;
        }
        
        try {
            // Validate email uniqueness before saving
            EmailCheckResult emailCheck = checkEmailExists(student.email, student.studentId);
            if (emailCheck.exists) {
                throw new IllegalArgumentException(
                    "Email address already exists: " + student.email + " (used by student ID: " + emailCheck.existingStudentId + ")");
            }
            
            // Normalize student ID to lowercase for case-insensitive handling
            String originalId = student.studentId;
            student.studentId = normalizeStudentId(originalId);
            
            // Ensure email is stored in lowercase
            student.email = student.email.toLowerCase().trim();
            
            // Ensure password is always stored as a BCrypt hash
            if (student.password != null
                    && !PasswordUtils.isHashed(student.password)) {
                student.password = PasswordUtils.hash(student.password);
            }
            Bson filter = Filters.eq("studentId", student.studentId);
            collection.replaceOne(filter, toDoc(student),
                new ReplaceOptions().upsert(true));
                
            // Keep the normalized ID to ensure consistency throughout the application
            // The UI layer can handle display formatting if needed
        } catch (Exception e) {
            System.err.println("[DataManager] saveStudent: " + e.getMessage());
            throw e; // Re-throw to allow caller to handle the exception
        }
    }
    public static Student loadStudent(String studentId, String password) {
        try {
            // Normalize student ID for case-insensitive lookup
            String normalizedId = normalizeStudentId(studentId);
            
            // Fetch by studentId only — password check is done in application layer
            // to support both BCrypt hashes and legacy plain-text passwords.
            Document d = collection.find(
                Filters.eq("studentId", normalizedId)).first();
            if (d == null) return null;

            String stored = d.getString("password");
            boolean authenticated;
            if (PasswordUtils.isHashed(stored)) {
                // Modern: BCrypt verification
                authenticated = PasswordUtils.verify(password, stored);
            } else {
                // Legacy plain-text — verify then migrate on success
                authenticated = password.equals(stored);
                if (authenticated) {
                    // Migrate to BCrypt in-place
                    String hash = PasswordUtils.hash(password);
                    collection.updateOne(
                        Filters.eq("studentId", normalizedId),
                        Updates.set("password", hash));
                    d.put("password", hash);
                }
            }
            Student student = authenticated ? fromDoc(d) : null;
            // Keep the normalized ID to ensure consistency throughout the application
            return student;
        } catch (Exception e) {
            System.err.println("[DataManager] loadStudent: " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads a student by ID only — no password check.
     * Used by teacher modules to look up student names and records.
     */
    public static Student loadStudentById(String studentId) {
        try {
            String normalizedId = normalizeStudentId(studentId);
            Document d = collection.find(
                Filters.eq("studentId", normalizedId)).first();
            // Keep the normalized ID to ensure consistency throughout the application
            return d != null ? fromDoc(d) : null;
        } catch (Exception e) {
            System.err.println("[DataManager] loadStudentById: " + e.getMessage());
            return null;
        }
    }

        /**
     * Looks up a student by email only (used for password-reset flows).
     * Returns the student document without authenticating.
     */
    public static Student findByEmail(String email) {
        try {
            Document d = collection.find(
                Filters.eq("email", email.trim().toLowerCase())).first();
            return d != null ? fromDoc(d) : null;
        } catch (Exception e) {
            System.err.println("[DataManager] findByEmail: " + e.getMessage());
            return null;
        }
    }

    /**
     * Updates a student's password (after successful OTP verification).
     * The new password is BCrypt-hashed automatically.
     */
    public static boolean updatePassword(String studentId, String newPlainPassword) {
        try {
            String normalizedId = normalizeStudentId(studentId);
            String hash = PasswordUtils.hash(newPlainPassword);
            collection.updateOne(
                Filters.eq("studentId", normalizedId),
                Updates.set("password", hash));
            return true;
        } catch (Exception e) {
            System.err.println("[DataManager] updatePassword: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks the student's email as verified in the database.
     */
    public static boolean markEmailVerified(String studentId) {
        try {
            String normalizedId = normalizeStudentId(studentId);
            collection.updateOne(
                Filters.eq("studentId", normalizedId),
                Updates.set("emailVerified", true));
            return true;
        } catch (Exception e) {
            System.err.println("[DataManager] markEmailVerified: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the student's email has been verified.
     */
    public static boolean isEmailVerified(String studentId) {
        try {
            String normalizedId = normalizeStudentId(studentId);
            Document d = collection.find(
                Filters.eq("studentId", normalizedId)).first();
            if (d == null) return false;
            Boolean v = d.getBoolean("emailVerified");
            return Boolean.TRUE.equals(v);
        } catch (Exception e) {
            System.err.println("[DataManager] isEmailVerified: " + e.getMessage());
            return false;
        }
    }
    /**
     * Returns all students that are enrolled in the given subject.
     * Matches both attendanceMap keys and enrolledSubjects list.
     */
    @SuppressWarnings("unchecked")
    public static java.util.List<Student> findAllStudentsBySubject(String subject) {
        java.util.List<Student> result = new java.util.ArrayList<>();
        try {
            for (Document d : collection.find()) {
                Student s = fromDoc(d);
                boolean hasSubject = s.attendanceMap.containsKey(subject)
                    || s.marksMap.containsKey(subject)
                    || (s.enrolledSubjects != null && s.enrolledSubjects.contains(subject));
                if (hasSubject) result.add(s);
            }
        } catch (Exception e) {
            System.err.println("[DataManager] findAllStudentsBySubject: " + e.getMessage());
        }
        return result;
    }

    /**
     * Returns all student IDs for a given subject (lightweight — no full hydration).
     */
    @SuppressWarnings("unchecked")
    public static java.util.List<String> findStudentIdsBySubject(String subject) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        try {
            for (Document d : collection.find()) {
                String sid = d.getString("studentId");
                // Check attendanceMap keys
                Document attDoc = (Document) d.get("attendance");
                java.util.List<Document> attList = (java.util.List<Document>) d.get("attendance");
                boolean found = false;
                if (attList != null)
                    for (Document a : attList)
                        if (subject.equals(a.getString("subject"))) { found = true; break; }
                if (!found) {
                    java.util.List<Document> marksList = (java.util.List<Document>) d.get("marks");
                    if (marksList != null)
                        for (Document m : marksList)
                            if (subject.equals(m.getString("subject"))) { found = true; break; }
                }
                if (!found) {
                    java.util.List<String> enrolled = (java.util.List<String>) d.get("enrolledSubjects");
                    if (enrolled != null && enrolled.contains(subject)) found = true;
                }
                if (found && sid != null) ids.add(sid);
            }
        } catch (Exception e) {
            System.err.println("[DataManager] findStudentIdsBySubject: " + e.getMessage());
        }
        return ids;
    }

    // === Student -> Document ==================================================
    private static Document toDoc(Student s) {
        List<Document> att = new ArrayList<>();
        for (Map.Entry<String, List<AttendanceRecord>> e : s.attendanceMap.entrySet())
            for (AttendanceRecord r : e.getValue())
                att.add(new Document().append("subject", r.subject)
                    .append("date", r.date.toString()).append("present", r.present)
                    .append("medicalLeaveId", r.medicalLeaveId));
        List<Document> marks = new ArrayList<>();
        for (Map.Entry<String, List<MarksRecord>> e : s.marksMap.entrySet())
            for (MarksRecord r : e.getValue())
                marks.add(new Document().append("subject", r.subject)
                    .append("testType", r.testType).append("marksObtained", r.marksObtained).append("maxMarks", r.maxMarks));
        List<Document> tasks = new ArrayList<>();
        for (StudentTask t : s.getTasks())
            tasks.add(new Document().append("id", t.id).append("title", t.title)
                .append("taskType", t.taskType).append("subject", t.subject)
                .append("dueDate", t.dueDate != null ? t.dueDate.toString() : null)
                .append("description", t.description).append("priority", t.priority)
                .append("submitted", t.submitted)
                .append("submittedDate", t.submittedDate != null ? t.submittedDate.toString() : null)
                .append("notes", t.notes)
                .append("createdDate", t.createdDate != null ? t.createdDate.toString() : null));
        // Medical leaves
        List<Document> leaves = new ArrayList<>();
        if (s.medicalLeaves != null)
            for (com.scis.model.MedicalLeave ml : s.medicalLeaves)
                leaves.add(new Document()
                    .append("id",            ml.id)
                    .append("subject",       ml.subject)
                    .append("startDate",     ml.startDate != null ? ml.startDate.toString() : null)
                    .append("endDate",       ml.endDate   != null ? ml.endDate.toString()   : null)
                    .append("reason",        ml.reason)
                    .append("documentRef",   ml.documentRef)
                    .append("attachmentName", ml.attachmentName)
                    .append("attachmentData", ml.attachmentData)
                    .append("attachmentType", ml.attachmentType)
                    .append("status",        ml.status != null ? ml.status.name() : "PENDING")
                    .append("reviewedBy",    ml.reviewedBy)
                    .append("rejectionNote", ml.rejectionNote)
                    .append("submittedDate", ml.submittedDate != null ? ml.submittedDate.toString() : null)
                    .append("reviewedDate",  ml.reviewedDate  != null ? ml.reviewedDate.toString()  : null));

        return new Document()
            .append("studentId", normalizeStudentId(s.studentId)).append("name", s.name)
            .append("email", s.email).append("department", s.department)
            .append("semester", s.semester).append("password", s.password)
            .append("collegeName", s.collegeName)
            .append("overallPassingMarks", s.overallPassingMarks)
            .append("classAverageMarks", dblMap(s.classAverageMarks))
            .append("passingMarks", dblMap(s.passingMarks))
            .append("enrolledSubjects", s.enrolledSubjects != null ? s.enrolledSubjects : new ArrayList<>())
            .append("attendance", att).append("marks", marks).append("tasks", tasks)
            .append("medicalLeaves", leaves);
    }
    // === Document -> Student ==================================================
    @SuppressWarnings("unchecked")
    private static Student fromDoc(Document d) {
        Student s = new Student();
        s.studentId           = d.getString("studentId");
        s.name                = d.getString("name");
        s.email               = d.getString("email");
        s.department          = d.getString("department");
        s.semester            = d.getInteger("semester", 1);
        s.password            = d.getString("password");
        s.collegeName         = def(d, "collegeName", "Unknown College");
        s.overallPassingMarks = num(d, "overallPassingMarks", 40.0);
        fillDblMap((Document) d.get("classAverageMarks"), s.classAverageMarks);
        fillDblMap((Document) d.get("passingMarks"), s.passingMarks);
        List<Document> aL = (List<Document>) d.get("attendance");
        if (aL != null) for (Document a : aL) {
            String subj = a.getString("subject");
            s.attendanceMap.computeIfAbsent(subj, k -> new ArrayList<>())
                .add(new AttendanceRecord(subj, LocalDate.parse(a.getString("date")), Boolean.TRUE.equals(a.getBoolean("present"))));
                // restore medicalLeaveId if present
                List<AttendanceRecord> lastList = s.attendanceMap.get(subj);
                if (lastList != null && !lastList.isEmpty())
                    lastList.get(lastList.size()-1).medicalLeaveId = a.getString("medicalLeaveId");
        }
        for (String subj : s.attendanceMap.keySet()) s.marksMap.computeIfAbsent(subj, k -> new ArrayList<>());
        List<Document> mL = (List<Document>) d.get("marks");
        if (mL != null) for (Document m : mL) {
            String subj = m.getString("subject");
            s.marksMap.computeIfAbsent(subj, k -> new ArrayList<>())
                .add(new MarksRecord(subj, m.getString("testType"), num(m, "marksObtained", 0.0), num(m, "maxMarks", 100.0)));
        }
        List<Document> tL = (List<Document>) d.get("tasks");
        if (tL != null) for (Document t : tL) {
            StudentTask task  = new StudentTask();
            task.id            = def(t, "id", task.id);
            task.title         = t.getString("title");
            task.taskType      = t.getString("taskType");
            task.subject       = t.getString("subject");
            task.description   = t.getString("description");
            task.priority      = t.getInteger("priority", 2);
            task.submitted     = Boolean.TRUE.equals(t.getBoolean("submitted"));
            task.notes         = t.getString("notes");
            task.dueDate       = date(t.getString("dueDate"));
            task.submittedDate = date(t.getString("submittedDate"));
            task.createdDate   = date(t.getString("createdDate"));
            s.getTasks().add(task);
        }
        // Enrolled subjects
        Object esObj = d.get("enrolledSubjects");
        if (esObj instanceof java.util.List) {
            for (Object o : (java.util.List<?>) esObj)
                if (o instanceof String) s.getEnrolledSubjects().add((String) o);
        }

        // Medical leaves
        java.util.List<Document> leaveList = (java.util.List<Document>) d.get("medicalLeaves");
        if (leaveList != null) {
            for (Document ld : leaveList) {
                com.scis.model.MedicalLeave ml = new com.scis.model.MedicalLeave();
                ml.id            = def(ld, "id", ml.id);
                ml.subject       = ld.getString("subject");
                ml.reason        = ld.getString("reason");
                ml.documentRef   = ld.getString("documentRef");
                ml.attachmentName = ld.getString("attachmentName");
                ml.attachmentData = ld.getString("attachmentData");
                ml.attachmentType = ld.getString("attachmentType");
                ml.reviewedBy    = ld.getString("reviewedBy");
                ml.rejectionNote = ld.getString("rejectionNote");
                ml.startDate     = date(ld.getString("startDate"));
                ml.endDate       = date(ld.getString("endDate"));
                ml.submittedDate = date(ld.getString("submittedDate"));
                ml.reviewedDate  = date(ld.getString("reviewedDate"));
                String statusStr = ld.getString("status");
                try {
                    ml.status = statusStr != null
                        ? com.scis.model.MedicalLeave.Status.valueOf(statusStr)
                        : com.scis.model.MedicalLeave.Status.PENDING;
                } catch (Exception ignored) {
                    ml.status = com.scis.model.MedicalLeave.Status.PENDING;
                }
                s.getMedicalLeaves().add(ml);
            }
        }

        // Link medicalLeaveId back to attendance records
        if (s.medicalLeaves != null) {
            for (String subj : s.attendanceMap.keySet()) {
                for (AttendanceRecord rec : s.attendanceMap.get(subj)) {
                    if (!rec.present && rec.medicalLeaveId == null) {
                        com.scis.model.MedicalLeave cover =
                            s.getApprovedLeaveFor(subj, rec.date);
                        if (cover != null) rec.medicalLeaveId = cover.id;
                    }
                }
            }
        }
        return s;
    }
    // === College-scoped queries ===============================================

    /**
     * Returns every distinct collegeName stored in the students collection,
     * sorted alphabetically and excluding blank / "Unknown College" entries.
     */
    public static java.util.List<String> getAllCollegeNames() {
        java.util.TreeSet<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try {
            for (Document d : collection.find()) {
                String c = d.getString("collegeName");
                if (c != null && !c.trim().isEmpty()
                        && !c.equalsIgnoreCase("Unknown College"))
                    names.add(c.trim());
            }
        } catch (Exception e) {
            System.err.println("[DataManager] getAllCollegeNames: " + e.getMessage());
        }
        return new java.util.ArrayList<>(names);
    }

    /**
     * Returns all students whose collegeName exactly matches (case-insensitive)
     * the supplied college name.
     */
    public static java.util.List<Student> findAllStudentsByCollege(String college) {
        java.util.List<Student> result = new java.util.ArrayList<>();
        if (college == null || college.trim().isEmpty()) return result;
        try {
            for (Document d : collection.find(
                    Filters.regex("collegeName",
                        "^" + java.util.regex.Pattern.quote(college.trim()) + "$",
                        "i"))) {
                result.add(fromDoc(d));
            }
        } catch (Exception e) {
            System.err.println("[DataManager] findAllStudentsByCollege: " + e.getMessage());
        }
        return result;
    }

    /**
     * Returns all students in the given subject whose collegeName matches.
     */
    public static java.util.List<Student> findStudentsBySubjectAndCollege(
            String subject, String college) {
        java.util.List<Student> all = findAllStudentsByCollege(college);
        java.util.List<Student> result = new java.util.ArrayList<>();
        for (Student s : all) {
            boolean hasSubject = s.attendanceMap.containsKey(subject)
                || s.marksMap.containsKey(subject)
                || (s.enrolledSubjects != null && s.enrolledSubjects.contains(subject));
            if (hasSubject) result.add(s);
        }
        return result;
    }

    private static String def(Document d,String k,String fb){Object v=d.get(k);return v!=null?v.toString():fb;}
    private static double num(Document d,String k,double fb){Object v=d.get(k);return v instanceof Number?((Number)v).doubleValue():fb;}
    private static LocalDate date(String s){try{return(s!=null&&!s.isEmpty())?LocalDate.parse(s):null;}catch(Exception e){return null;}}
    private static void fillDblMap(Document src,Map<String,Double> dest){if(src==null)return;for(String k:src.keySet()){Object v=src.get(k);if(v instanceof Number)dest.put(k,((Number)v).doubleValue());}}
    private static Document dblMap(Map<String,Double> m){Document d=new Document();if(m!=null)m.forEach(d::append);return d;}
}
