package com.scis.teacher.db;

import com.scis.auth.PasswordUtils;
import com.scis.teacher.model.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.*;

/**
 * TeacherDataManager — MongoDB persistence layer for teacher data.
 *
 * Database  : smartcampus
 * Collection: teachers  (completely separate from "students")
 *
 * Teacher documents embed:
 *  - subjectClassMap  — subjects + class sections taught
 *  - attendanceLog    — all attendance records the teacher has entered
 *  - marksLog         — all marks records the teacher has entered
 *  - assignments      — assignments created by the teacher
 *  - announcements    — notices posted by the teacher
 */
public class TeacherDataManager {

    private static final String MONGO_URI  = "mongodb://localhost:27017";
    private static final String DB_NAME    = "smartcampus";
    private static final String COLL_NAME  = "teachers";

    private static MongoClient               mongoClient;
    private static MongoCollection<Document> collection;
    
    /** Result class for email existence checking */
    public static class EmailCheckResult {
        public final boolean exists;
        public final String existingTeacherId;
        
        public EmailCheckResult(boolean exists, String existingTeacherId) {
            this.exists = exists;
            this.existingTeacherId = existingTeacherId;
        }
    }

    static {
        try {
            mongoClient = MongoClients.create(MONGO_URI);
            collection  = mongoClient.getDatabase(DB_NAME)
                                     .getCollection(COLL_NAME);
            collection.createIndex(
                Indexes.ascending("teacherId"),
                new IndexOptions().unique(true));
            collection.createIndex(
                Indexes.ascending("email"),
                new IndexOptions().unique(true));
            System.out.println(
                "[TeacherDataManager] Connected -> " + DB_NAME + "." + COLL_NAME);
        } catch (Exception e) {
            System.err.println(
                "[TeacherDataManager] MongoDB init failed: " + e.getMessage());
        }
    }

    private TeacherDataManager() {}

    /**
     * Checks if an email already exists in the database (both teacher and student collections).
     * @param email The email to check (should be normalized to lowercase)
     * @param excludeTeacherId Teacher ID to exclude from the check (for updates)
     * @return EmailCheckResult indicating if email exists and which teacher owns it
     */
    public static EmailCheckResult checkEmailExists(String email, String excludeTeacherId) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            
            // Check teacher collection first
            Document existing = collection.find(Filters.eq("email", normalizedEmail)).first();
            if (existing != null) {
                String existingTeacherId = existing.getString("teacherId");
                // If we're excluding a teacher ID (for updates), don't count it as a conflict
                if (excludeTeacherId != null && excludeTeacherId.equals(existingTeacherId)) {
                    return new EmailCheckResult(false, null);
                }
                return new EmailCheckResult(true, existingTeacherId);
            }
            
            // Check student collection directly to prevent cross-type email duplication
            try {
                MongoCollection<Document> studentCollection = mongoClient.getDatabase("smartcampus").getCollection("students");
                Document studentExisting = studentCollection.find(Filters.eq("email", normalizedEmail)).first();
                if (studentExisting != null) {
                    return new EmailCheckResult(true, "STUDENT_" + studentExisting.getString("studentId"));
                }
            } catch (Exception ex) {
                System.err.println("[TeacherDataManager] Student email check failed: " + ex.getMessage());
            }
            
            return new EmailCheckResult(false, null);
        } catch (Exception e) {
            System.err.println("[TeacherDataManager] checkEmailExists: " + e.getMessage());
            return new EmailCheckResult(false, null);
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public static boolean teacherExists(String teacherId) {
        try {
            return collection.find(
                Filters.eq("teacherId", teacherId)).first() != null;
        } catch (Exception e) { return false; }
    }

    public static void saveTeacher(Teacher teacher) {
        if (teacher == null) {
            System.err.println("[TeacherDataManager] saveTeacher: teacher is null");
            return;
        }
        
        try {
            // Validate email uniqueness before saving
            EmailCheckResult emailCheck = checkEmailExists(teacher.email, teacher.teacherId);
            if (emailCheck.exists) {
                throw new IllegalArgumentException(
                    "Email address already exists: " + teacher.email + " (used by teacher ID: " + emailCheck.existingTeacherId + ")");
            }
            
            // Ensure email is stored in lowercase
            teacher.email = teacher.email.toLowerCase().trim();
            
            if (teacher.password != null
                    && !PasswordUtils.isHashed(teacher.password)) {
                teacher.password = PasswordUtils.hash(teacher.password);
            }
            Bson filter = Filters.eq("teacherId", teacher.teacherId);
            collection.replaceOne(filter, toDoc(teacher),
                new ReplaceOptions().upsert(true));
        } catch (Exception e) {
            System.err.println("[TeacherDataManager] saveTeacher: " + e.getMessage());
            throw e; // Re-throw to allow caller to handle the exception
        }
    }

    public static Teacher loadTeacher(String teacherId, String password) {
        try {
            Document d = collection.find(
                Filters.eq("teacherId", teacherId)).first();
            if (d == null) return null;

            String stored = d.getString("password");
            boolean authenticated;
            if (PasswordUtils.isHashed(stored)) {
                authenticated = PasswordUtils.verify(password, stored);
            } else {
                authenticated = password.equals(stored);
                if (authenticated) {
                    String hash = PasswordUtils.hash(password);
                    collection.updateOne(
                        Filters.eq("teacherId", teacherId),
                        Updates.set("password", hash));
                }
            }
            return authenticated ? fromDoc(d) : null;
        } catch (Exception e) {
            System.err.println("[TeacherDataManager] loadTeacher: " + e.getMessage());
            return null;
        }
    }

    public static Teacher findTeacherByEmail(String email) {
        try {
            Document d = collection.find(
                Filters.eq("email", email.trim().toLowerCase())).first();
            return d != null ? fromDoc(d) : null;
        } catch (Exception e) {
            System.err.println("[TeacherDataManager] findByEmail: " + e.getMessage());
            return null;
        }
    }

    public static boolean updateTeacherPassword(String teacherId, String newPlain) {
        try {
            collection.updateOne(
                Filters.eq("teacherId", teacherId),
                Updates.set("password", PasswordUtils.hash(newPlain)));
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean markEmailVerified(String teacherId) {
        try {
            collection.updateOne(
                Filters.eq("teacherId", teacherId),
                Updates.set("emailVerified", true));
            return true;
        } catch (Exception e) { return false; }
    }

    /** Returns all student IDs that appear in this teacher's attendance log. */
    public static List<String> getStudentIdsForSubject(
            String teacherId, String subject) {
        Set<String> ids = new LinkedHashSet<>();
        try {
            Document d = collection.find(
                Filters.eq("teacherId", teacherId)).first();
            if (d == null) return new ArrayList<>();
            Teacher t = fromDoc(d);
            for (TeacherAttendanceRecord r : getAttendanceLog(teacherId))
                if (subject.equals(r.subject)) ids.add(r.studentId);
        } catch (Exception e) { /* ignore */ }
        return new ArrayList<>(ids);
    }

    /** Fetches the teacher's full attendance log from the DB. */
    @SuppressWarnings("unchecked")
    public static List<TeacherAttendanceRecord> getAttendanceLog(String teacherId) {
        try {
            Document d = collection.find(
                Filters.eq("teacherId", teacherId)).first();
            if (d == null) return new ArrayList<>();
            return parseAttLog((List<Document>) d.get("attendanceLog"));
        } catch (Exception e) { return new ArrayList<>(); }
    }

    /** Fetches the teacher's full marks log from the DB. */
    @SuppressWarnings("unchecked")
    public static List<TeacherMarksRecord> getMarksLog(String teacherId) {
        try {
            Document d = collection.find(
                Filters.eq("teacherId", teacherId)).first();
            if (d == null) return new ArrayList<>();
            return parseMarksLog((List<Document>) d.get("marksLog"));
        } catch (Exception e) { return new ArrayList<>(); }
    }

    // ── Serialisation: Teacher → Document ────────────────────────────────────

    private static Document toDoc(Teacher t) {
        // subjectClassMap
        Document scm = new Document();
        for (Map.Entry<String, List<String>> e : t.subjectClassMap.entrySet())
            scm.append(e.getKey(), e.getValue());

        // Use the Teacher's live attendanceLog and marksLog fields (populated on load)

        return new Document()
            .append("teacherId",   t.teacherId)
            .append("name",        t.name)
            .append("email",       t.email)
            .append("department",  t.department)
            .append("designation", t.designation)
            .append("collegeName", t.collegeName)
            .append("password",    t.password)
            .append("subjectClassMap", scm)
            .append("attendanceLog",   serAttLog(t.attendanceLog))
            .append("marksLog",        serMarksLog(t.marksLog))
            .append("assignments",     serAssignments(t.getAssignments()))
            .append("announcements",   serAnnouncements(t.getAnnouncements()));
    }

    private static List<Document> serAttLog(List<TeacherAttendanceRecord> list) {
        List<Document> docs = new ArrayList<>();
        if (list == null) return docs;
        for (TeacherAttendanceRecord r : list)
            docs.add(new Document()
                .append("studentId",    r.studentId)
                .append("studentName",  r.studentName)
                .append("subject",      r.subject)
                .append("classSection", r.classSection)
                .append("date",         r.date != null ? r.date.toString() : null)
                .append("present",      r.present)
                .append("remarks",      r.remarks));
        return docs;
    }

    private static List<Document> serMarksLog(List<TeacherMarksRecord> list) {
        List<Document> docs = new ArrayList<>();
        if (list == null) return docs;
        for (TeacherMarksRecord r : list)
            docs.add(new Document()
                .append("studentId",    r.studentId)
                .append("studentName",  r.studentName)
                .append("subject",      r.subject)
                .append("classSection", r.classSection)
                .append("examType",     r.examType)
                .append("marksObtained",r.marksObtained)
                .append("maxMarks",     r.maxMarks)
                .append("examDate",     r.examDate != null ? r.examDate.toString() : null)
                .append("remarks",      r.remarks));
        return docs;
    }

    private static List<Document> serAssignments(List<TeacherAssignment> list) {
        List<Document> docs = new ArrayList<>();
        if (list == null) return docs;
        for (TeacherAssignment a : list) {
            Document subDoc = new Document()
                .append("id",             a.id)
                .append("title",          a.title)
                .append("description",    a.description)
                .append("subject",        a.subject)
                .append("classSection",   a.classSection)
                .append("assignmentType", a.assignmentType)
                .append("maxMarks",       a.maxMarks)
                .append("assignedDate",   a.assignedDate != null ? a.assignedDate.toString() : null)
                .append("dueDate",        a.dueDate != null ? a.dueDate.toString() : null)
                .append("createdDate",    a.createdDate != null ? a.createdDate.toString() : null)
                .append("submissionMap",  mapToDoc(a.submissionMap))
                .append("marksMap",       dblMapToDoc(a.marksMap))
                .append("feedbackMap",    mapToDoc(a.feedbackMap));
            docs.add(subDoc);
        }
        return docs;
    }

    private static List<Document> serAnnouncements(List<TeacherAnnouncement> list) {
        List<Document> docs = new ArrayList<>();
        if (list == null) return docs;
        for (TeacherAnnouncement an : list)
            docs.add(new Document()
                .append("id",           an.id)
                .append("title",        an.title)
                .append("message",      an.message)
                .append("subject",      an.subject)
                .append("classSection", an.classSection)
                .append("priority",     an.priority)
                .append("postedDate",   an.postedDate != null ? an.postedDate.toString() : null)
                .append("expiryDate",   an.expiryDate != null ? an.expiryDate.toString() : null));
        return docs;
    }

    // ── Deserialisation: Document → Teacher ──────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Teacher fromDoc(Document d) {
        Teacher t = new Teacher();
        t.teacherId   = d.getString("teacherId");
        t.name        = d.getString("name");
        t.email       = d.getString("email");
        t.department  = d.getString("department");
        t.designation = d.getString("designation");
        t.collegeName = def(d, "collegeName", "Unknown College");
        t.password    = d.getString("password");

        Document scm = (Document) d.get("subjectClassMap");
        if (scm != null)
            for (String key : scm.keySet())
                t.subjectClassMap.put(key, (List<String>) scm.get(key));

        t.attendanceLog = parseAttLog((List<Document>) d.get("attendanceLog"));
        t.marksLog      = parseMarksLog((List<Document>) d.get("marksLog"));
        t.assignments   = parseAssignments((List<Document>) d.get("assignments"));
        t.announcements = parseAnnouncements((List<Document>) d.get("announcements"));
        return t;
    }

    private static List<TeacherAttendanceRecord> parseAttLog(List<Document> docs) {
        List<TeacherAttendanceRecord> list = new ArrayList<>();
        if (docs == null) return list;
        for (Document doc : docs) {
            TeacherAttendanceRecord r = new TeacherAttendanceRecord();
            r.studentId    = doc.getString("studentId");
            r.studentName  = doc.getString("studentName");
            r.subject      = doc.getString("subject");
            r.classSection = doc.getString("classSection");
            r.date         = parseDate(doc.getString("date"));
            r.present      = Boolean.TRUE.equals(doc.getBoolean("present"));
            r.remarks      = doc.getString("remarks");
            list.add(r);
        }
        return list;
    }

    private static List<TeacherMarksRecord> parseMarksLog(List<Document> docs) {
        List<TeacherMarksRecord> list = new ArrayList<>();
        if (docs == null) return list;
        for (Document doc : docs) {
            TeacherMarksRecord r = new TeacherMarksRecord();
            r.studentId     = doc.getString("studentId");
            r.studentName   = doc.getString("studentName");
            r.subject       = doc.getString("subject");
            r.classSection  = doc.getString("classSection");
            r.examType      = doc.getString("examType");
            r.marksObtained = num(doc, "marksObtained", 0.0);
            r.maxMarks      = num(doc, "maxMarks", 100.0);
            r.examDate      = parseDate(doc.getString("examDate"));
            r.remarks       = doc.getString("remarks");
            list.add(r);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<TeacherAssignment> parseAssignments(List<Document> docs) {
        List<TeacherAssignment> list = new ArrayList<>();
        if (docs == null) return list;
        for (Document doc : docs) {
            TeacherAssignment a = new TeacherAssignment();
            a.id             = def(doc, "id", a.id);
            a.title          = doc.getString("title");
            a.description    = doc.getString("description");
            a.subject        = doc.getString("subject");
            a.classSection   = doc.getString("classSection");
            a.assignmentType = doc.getString("assignmentType");
            a.maxMarks       = num(doc, "maxMarks", 100.0);
            a.assignedDate   = parseDate(doc.getString("assignedDate"));
            a.dueDate        = parseDate(doc.getString("dueDate"));
            a.createdDate    = parseDate(doc.getString("createdDate"));
            a.submissionMap  = docToStrBoolMap((Document) doc.get("submissionMap"));
            a.marksMap       = docToDblMap((Document) doc.get("marksMap"));
            a.feedbackMap    = docToStrStrMap((Document) doc.get("feedbackMap"));
            list.add(a);
        }
        return list;
    }

    private static List<TeacherAnnouncement> parseAnnouncements(List<Document> docs) {
        List<TeacherAnnouncement> list = new ArrayList<>();
        if (docs == null) return list;
        for (Document doc : docs) {
            TeacherAnnouncement an = new TeacherAnnouncement();
            an.id           = def(doc, "id", an.id);
            an.title        = doc.getString("title");
            an.message      = doc.getString("message");
            an.subject      = doc.getString("subject");
            an.classSection = doc.getString("classSection");
            an.priority     = def(doc, "priority", "Medium");
            an.postedDate   = parseDate(doc.getString("postedDate"));
            an.expiryDate   = parseDate(doc.getString("expiryDate"));
            list.add(an);
        }
        return list;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static String def(Document d, String k, String fb) {
        Object v = d.get(k); return v != null ? v.toString() : fb;
    }
    private static double num(Document d, String k, double fb) {
        Object v = d.get(k); return v instanceof Number ? ((Number)v).doubleValue() : fb;
    }
    private static LocalDate parseDate(String s) {
        try { return (s != null && !s.isEmpty()) ? LocalDate.parse(s) : null; }
        catch (Exception e) { return null; }
    }
    private static Document mapToDoc(Map<String, ?> m) {
        Document doc = new Document();
        if (m != null) m.forEach((k, v) -> doc.append(k, v));
        return doc;
    }
    private static Document dblMapToDoc(Map<String, Double> m) {
        Document doc = new Document();
        if (m != null) m.forEach(doc::append);
        return doc;
    }
    private static Map<String, Boolean> docToStrBoolMap(Document doc) {
        Map<String, Boolean> map = new HashMap<>();
        if (doc != null) for (String k : doc.keySet()) {
            Object v = doc.get(k);
            map.put(k, Boolean.TRUE.equals(v));
        }
        return map;
    }
    private static Map<String, Double> docToDblMap(Document doc) {
        Map<String, Double> map = new HashMap<>();
        if (doc != null) for (String k : doc.keySet()) {
            Object v = doc.get(k);
            if (v instanceof Number) map.put(k, ((Number)v).doubleValue());
        }
        return map;
    }
    private static Map<String, String> docToStrStrMap(Document doc) {
        Map<String, String> map = new HashMap<>();
        if (doc != null) for (String k : doc.keySet()) {
            Object v = doc.get(k);
            if (v != null) map.put(k, v.toString());
        }
        return map;
    }

    // === College-scoped queries ===============================================

    /**
     * Returns every distinct collegeName stored in the teachers collection,
     * sorted alphabetically, excluding blank / "Unknown College" entries.
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
            System.err.println("[TeacherDataManager] getAllCollegeNames: " + e.getMessage());
        }
        return new java.util.ArrayList<>(names);
    }

    /**
     * Returns all teachers whose collegeName exactly matches (case-insensitive)
     * the supplied college name.
     */
    public static java.util.List<Teacher> findAllTeachersByCollege(String college) {
        java.util.List<Teacher> result = new java.util.ArrayList<>();
        if (college == null || college.trim().isEmpty()) return result;
        try {
            for (Document d : collection.find(
                    com.mongodb.client.model.Filters.regex("collegeName",
                        "^" + java.util.regex.Pattern.quote(college.trim()) + "$",
                        "i"))) {
                result.add(fromDoc(d));
            }
        } catch (Exception e) {
            System.err.println("[TeacherDataManager] findAllTeachersByCollege: " + e.getMessage());
        }
        return result;
    }

    /**
     * Returns a merged, de-duplicated list of college names from both
     * the students and teachers collections — used to populate dropdowns
     * in registration forms.
     */
    public static java.util.List<String> getAllCollegeNamesCombined() {
        java.util.TreeSet<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(getAllCollegeNames());
        names.addAll(com.scis.db.DataManager.getAllCollegeNames());
        return new java.util.ArrayList<>(names);
    }
}
