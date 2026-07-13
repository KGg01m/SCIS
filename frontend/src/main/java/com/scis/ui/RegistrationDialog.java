package com.scis.ui;

import com.scis.auth.EmailService;
import com.scis.db.DataManager;
import com.scis.model.Student;
import com.scis.teacher.db.TeacherDataManager;
import com.scis.teacher.model.Teacher;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class RegistrationDialog {

    private static final String ENTER_NEW = "-- Enter a new institution --";
    private static final String[] COMMON_SUBJECTS = {
        "Mathematics", "Physics", "Chemistry", "Biology",
        "Computer Science", "Data Structures", "Algorithms",
        "Database Management", "Operating Systems", "Computer Networks",
        "Software Engineering", "Artificial Intelligence", "Machine Learning",
        "Electronics", "Digital Circuits", "Signals & Systems",
        "Thermodynamics", "Fluid Mechanics", "Strength of Materials",
        "Structural Analysis", "Surveying", "Engineering Drawing",
        "Power Systems", "Control Systems", "Electrical Machines",
        "Engineering Mathematics", "Engineering Physics",
        "English Communication", "Economics", "Management"
    };

    private RegistrationDialog() {}

    public static void show(Component parent) {
        if (Theme.currentRole == Theme.Role.TEACHER) {
            Step1Result info = showStep1(parent, true);
            if (info == null) return;
            List<SubjectEntry> subjects = showStep2(parent, info);
            
            Teacher t = new Teacher(info.id, info.name, info.email, info.dept, info.desig,
                info.college.isEmpty() ? "Unknown College" : info.college, info.password);
            for (SubjectEntry se : subjects) t.addSubject(se.subject, se.section);

            try {
                TeacherDataManager.EmailCheckResult emailCheck = TeacherDataManager.checkEmailExists(t.email, t.teacherId);
                if (emailCheck.exists) {
                    JOptionPane.showMessageDialog(parent, "Email address is already registered.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Unable to verify email uniqueness. Please try again.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }

            try { EmailService.sendVerificationOtp(info.email); } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Could not send verification email. Registering without email verification.", "Warning", JOptionPane.WARNING_MESSAGE);
                finishTeacherSave(parent, t, false); return;
            }
            OtpVerificationDialog.show(parent, info.email, () -> EmailService.sendVerificationOtp(info.email), verified -> {
                if (Boolean.TRUE.equals(verified)) finishTeacherSave(parent, t, true);
                else JOptionPane.showMessageDialog(parent, "Registration cancelled — email not verified.", "Cancelled", JOptionPane.WARNING_MESSAGE);
            });
        } else {
            Step1Result info = showStep1(parent, false);
            if (info == null) return;

            try {
                DataManager.EmailCheckResult ec = DataManager.checkEmailExists(info.email, info.id);
                if (ec.exists) {
                    JOptionPane.showMessageDialog(parent, "Email address is already registered.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Unable to verify email uniqueness. Please try again.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }

            try { EmailService.sendVerificationOtp(info.email); } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Email send failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            OtpVerificationDialog.show(parent, info.email, () -> EmailService.sendVerificationOtp(info.email), verified -> {
                if (Boolean.TRUE.equals(verified)) finishStudentSave(parent, info, true);
                else JOptionPane.showMessageDialog(parent, "Registration cancelled — email not verified.", "Cancelled", JOptionPane.WARNING_MESSAGE);
            });
        }
    }

    private static Step1Result showStep1(Component parent, boolean isTeacher) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), 
            isTeacher ? "Teacher Registration — Step 1 of 2" : "Student Registration", 
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setResizable(true);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
        panel.setBackground(Theme.card());

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST; g.weightx = 1.0;

        JLabel title = new JLabel(isTeacher ? "Create Teacher Account" : "Create your account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22)); title.setForeground(Theme.text());
        JLabel subtitle = new JLabel(isTeacher ? "Step 1: Personal & Login Details" : "Fill in the fields below to register with SCIS.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13)); subtitle.setForeground(Theme.muted());

        JTextField idField = styledField(); JTextField nameField = styledField(); JTextField emailField = styledField();

        JComboBox<String> collegeCombo = new JComboBox<>();
        collegeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13)); collegeCombo.setBackground(Theme.surface());
        collegeCombo.setForeground(Theme.text()); collegeCombo.setMaximumSize(new Dimension(450, 38));
        collegeCombo.setPreferredSize(new Dimension(450, 38));

        JTextField collegeCustomField = styledField();
        collegeCustomField.setToolTipText("Type the full, exact name of your institution");
        collegeCustomField.setVisible(false);

        JLabel collegeStatusLbl = new JLabel(" ");
        collegeStatusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        SwingUtilities.invokeLater(() -> {
            collegeCombo.addItem("Select your institution...");
            try {
                List<String> names = TeacherDataManager.getAllCollegeNamesCombined();
                for (String n : names) collegeCombo.addItem(n);
            } catch (Exception ex) {}
            collegeCombo.addItem(ENTER_NEW);
        });

        collegeCombo.addActionListener(e -> {
            boolean isNew = ENTER_NEW.equals(collegeCombo.getSelectedItem());
            collegeCustomField.setVisible(isNew);
            if (!isNew) collegeStatusLbl.setText(" ");
            panel.revalidate(); panel.repaint();
        });

        collegeCustomField.getDocument().addDocumentListener(new DocumentAdapter() {
            public void insertUpdate(DocumentEvent e) { liveValidate(); }
            public void removeUpdate(DocumentEvent e) { liveValidate(); }
            private void liveValidate() {
                String v = collegeCustomField.getText().trim();
                if (v.isEmpty()) { collegeStatusLbl.setText(" "); return; }
                if (v.length() < 3) {
                    collegeStatusLbl.setText("✗ Name must be at least 3 characters"); collegeStatusLbl.setForeground(Theme.RED);
                } else if (!v.matches("[A-Za-z0-9 .,'&()\\-]+")) {
                    collegeStatusLbl.setText("✗ Invalid characters in institution name"); collegeStatusLbl.setForeground(Theme.RED);
                } else {
                    collegeStatusLbl.setText("✓ New institution will be registered"); collegeStatusLbl.setForeground(Theme.EMERALD);
                }
            }
        });

        JComboBox<String> deptCombo = new JComboBox<>(isTeacher ? new String[]{"Computer Science","Electronics","Mechanical","Civil","Electrical","Information Technology","Mathematics","Physics","Chemistry","Other"} : new String[]{"Computer Science","Electronics","Mechanical","Civil","Electrical","Information Technology"});
        JComboBox<String> semCombo = new JComboBox<>(new String[]{"1","2","3","4","5","6","7","8"});
        JComboBox<String> desigCombo = new JComboBox<>(new String[]{"Professor","Associate Professor","Assistant Professor","Lecturer","Lab Instructor","HOD","Other"});
        JPasswordField passField = styledPassField(); JPasswordField conField = styledPassField();

        for (JComponent c : new JComponent[]{idField, nameField, emailField, deptCombo, semCombo, desigCombo, passField, conField}) {
            c.setMaximumSize(new Dimension(450, 38)); c.setPreferredSize(new Dimension(450, 38));
        }

        JLabel emailOkLbl = new JLabel(" "); emailOkLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        emailField.getDocument().addDocumentListener(new DocumentAdapter() {
            public void insertUpdate(DocumentEvent e) { check(); }
            public void removeUpdate(DocumentEvent e) { check(); }
            private void check() {
                String em = emailField.getText().trim();
                if (em.isEmpty()) { emailOkLbl.setText(" "); return; }
                if (em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { emailOkLbl.setText("✓ Valid email format"); emailOkLbl.setForeground(Theme.EMERALD); }
                else { emailOkLbl.setText("✗ Invalid email format"); emailOkLbl.setForeground(Theme.RED); }
            }
        });

        PasswordStrengthWidget strengthWidget = new PasswordStrengthWidget();
        strengthWidget.attach(passField); strengthWidget.setMaximumSize(new Dimension(450, 60)); strengthWidget.setPreferredSize(new Dimension(450, 60));

        JLabel matchLbl = new JLabel(" "); matchLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        Runnable checkMatch = () -> {
            String pw=new String(passField.getPassword()), con=new String(conField.getPassword());
            if (con.isEmpty()) { matchLbl.setText(" "); return; }
            if (pw.equals(con)) { matchLbl.setText("✓ Passwords match"); matchLbl.setForeground(Theme.EMERALD); }
            else { matchLbl.setText("✗ Passwords do not match"); matchLbl.setForeground(Theme.RED); }
        };
        passField.getDocument().addDocumentListener(new DocumentAdapter() { public void insertUpdate(DocumentEvent e){checkMatch.run();} public void removeUpdate(DocumentEvent e){checkMatch.run();} });
        conField.getDocument().addDocumentListener(new DocumentAdapter() { public void insertUpdate(DocumentEvent e){checkMatch.run();} public void removeUpdate(DocumentEvent e){checkMatch.run();} });

        JLabel statusLbl = new JLabel(" "); statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12)); statusLbl.setForeground(Theme.RED);
        JButton regBtn = new JButton(isTeacher ? "Next: Choose Subjects →" : "Register & Verify Email");
        regBtn.setFont(new Font("Segoe UI", Font.BOLD, 14)); regBtn.setForeground(Color.WHITE); regBtn.setBackground(Theme.getPrimary());
        regBtn.setFocusPainted(false); regBtn.setBorderPainted(false); regBtn.setPreferredSize(new Dimension(450, 46)); regBtn.setMaximumSize(new Dimension(450, 46)); regBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        int row = 0;
        g.gridy = row++; g.insets = new Insets(0,0,4,0);  panel.add(title, g);
        g.gridy = row++; g.insets = new Insets(0,0,20,0); panel.add(subtitle, g);

        row = addGridRow(panel, g, row, isTeacher ? "Teacher / Employee ID *" : "Student ID *", idField, null);
        row = addGridRow(panel, g, row, "Full Name *", nameField, null);
        row = addGridRow(panel, g, row, "Email Address *", emailField, emailOkLbl);

        g.gridy=row++; g.insets=new Insets(0,0,3,0); panel.add(fieldLabel("College / Institution *"), g);
        g.gridy=row++; g.insets=new Insets(0,0,2,0); panel.add(collegeCombo, g);
        g.gridy=row++; g.insets=new Insets(0,0,2,0); panel.add(collegeCustomField, g);
        g.gridy=row++; g.insets=new Insets(0,0,10,0); panel.add(collegeStatusLbl, g);

        row = addGridRow(panel, g, row, "Department *", deptCombo, null);
        if (isTeacher) row = addGridRow(panel, g, row, "Designation *", desigCombo, null);
        else row = addGridRow(panel, g, row, "Semester *", semCombo, null);

        row = addGridRow(panel, g, row, "Password *", passField, null);
        g.gridy = row++; g.insets = new Insets(0,0,12,0); panel.add(strengthWidget, g);
        row = addGridRow(panel, g, row, "Confirm Password *", conField, matchLbl);

        g.gridy = row++; g.insets = new Insets(0,0,14,0); panel.add(statusLbl, g);
        g.gridy = row;   g.insets = new Insets(0,0,0,0);  panel.add(regBtn, g);

        final Step1Result[] result = {null};

        regBtn.addActionListener(e -> {
            statusLbl.setForeground(Theme.RED);
            String id=idField.getText().trim(), nm=nameField.getText().trim(), em=emailField.getText().trim();
            String dept=(String)deptCombo.getSelectedItem(), pw=new String(passField.getPassword()), con2=new String(conField.getPassword());
            int sem = isTeacher ? 0 : Integer.parseInt((String)semCombo.getSelectedItem());
            String desig = isTeacher ? (String)desigCombo.getSelectedItem() : "";

            String selectedCombo = (String)collegeCombo.getSelectedItem();
            String college;
            if (ENTER_NEW.equals(selectedCombo)) {
                college = collegeCustomField.getText().trim();
                if (college.isEmpty()) { statusLbl.setText("Enter the name of your institution."); return; }
                if (college.length()<3) { statusLbl.setText("Institution name must be at least 3 characters."); return; }
                if (!college.matches("[A-Za-z0-9 .,'&()\\-]+")) { statusLbl.setText("Institution name contains invalid characters."); return; }
            } else if (selectedCombo==null || selectedCombo.startsWith("Select your")) {
                statusLbl.setText("Please select or enter your institution."); return;
            } else college = selectedCombo;

            if (id.isEmpty() || nm.isEmpty() || em.isEmpty() || pw.isEmpty()) { statusLbl.setText("All starred (*) fields are required."); return; }
            if (id.length()<3) { statusLbl.setText("ID must be at least 3 chars."); return; }
            if (!nm.matches("^[a-zA-Z\\s\\-'.]+$")) { statusLbl.setText("Full Name can only contain letters, spaces, hyphens, apostrophes, and dots."); return; }
            if (!em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { statusLbl.setText("Enter a valid email address."); return; }
            if (!strengthWidget.isSufficient()) { statusLbl.setText("Password is too weak."); return; }
            if (!pw.equals(con2)) { statusLbl.setText("Passwords do not match."); return; }
            
            if (isTeacher) {
                if (TeacherDataManager.teacherExists(id)) { statusLbl.setText("Teacher ID already registered."); return; }
            } else {
                if (DataManager.studentExists(id)) { statusLbl.setText("Student ID is already registered."); return; }
            }

            result[0] = new Step1Result(id, nm, em, college, dept, sem, desig, pw);
            dlg.dispose();
        });

        JScrollPane scroll = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16); scroll.setBorder(BorderFactory.createEmptyBorder());
        dlg.add(scroll); dlg.pack(); dlg.setSize(520, Math.min(dlg.getHeight(), 700));
        dlg.setLocationRelativeTo(parent); dlg.setVisible(true);

        return result[0];
    }

    private static List<SubjectEntry> showStep2(Component parent, Step1Result info) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), "Teacher Registration — Step 2 of 2", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setResizable(true);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32)); panel.setBackground(Theme.card());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0; g.fill=GridBagConstraints.HORIZONTAL; g.anchor=GridBagConstraints.NORTHWEST; g.weightx=1.0;

        JLabel title = new JLabel("Select Your Subjects"); title.setFont(new Font("Segoe UI", Font.BOLD, 20)); title.setForeground(Theme.getPrimary());
        JLabel subtitle = new JLabel("Choose subjects you teach."); subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12)); subtitle.setForeground(Theme.muted());
        
        int row = 0; g.gridy=row++; g.insets = new Insets(0,0,4,0); panel.add(title, g); g.gridy=row++; g.insets = new Insets(0,0,18,0); panel.add(subtitle, g);

        JPanel checkGrid = new JPanel(new GridLayout(0, 2, 8, 6)); checkGrid.setBackground(Theme.card());
        List<JCheckBox> boxes = new ArrayList<>();
        for (String s : COMMON_SUBJECTS) {
            JCheckBox cb = new JCheckBox(s); cb.setFont(new Font("Segoe UI", Font.PLAIN, 12)); cb.setBackground(Theme.card());
            boxes.add(cb); checkGrid.add(cb);
        }
        JScrollPane checkScroll = new JScrollPane(checkGrid); checkScroll.setPreferredSize(new Dimension(450, 260)); checkScroll.setMaximumSize(new Dimension(450, 260)); checkScroll.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(203, 213, 225), 1, true), BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        JTextField customSubjectField = UIFactory.styledTextField("Enter custom subject name..."); JTextField customSectionField = UIFactory.styledTextField("Section (optional)");
        JButton addCustomBtn = new JButton("+ Add"); addCustomBtn.setFont(new Font("Segoe UI", Font.BOLD, 12)); addCustomBtn.setBackground(Theme.PURPLE); addCustomBtn.setForeground(Color.WHITE); addCustomBtn.setFocusPainted(false); addCustomBtn.setBorderPainted(false); addCustomBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        DefaultListModel<String> selectedModel = new DefaultListModel<>(); JList<String> selectedList = new JList<>(selectedModel); selectedList.setFont(new Font("Segoe UI", Font.PLAIN, 12)); selectedList.setFixedCellHeight(26);
        JScrollPane selectedScroll = new JScrollPane(selectedList); selectedScroll.setPreferredSize(new Dimension(450, 120)); selectedScroll.setMaximumSize(new Dimension(450, 120)); selectedScroll.setBorder(BorderFactory.createTitledBorder("Selected Subjects"));

        JButton removeSelectedBtn = new JButton("Remove Selected"); removeSelectedBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11)); removeSelectedBtn.setBackground(Theme.RED); removeSelectedBtn.setForeground(Color.WHITE); removeSelectedBtn.setFocusPainted(false); removeSelectedBtn.setBorderPainted(false); removeSelectedBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JTextField sectionForChecked = UIFactory.styledTextField("Section for checked subjects");
        JButton addCheckedBtn = new JButton("Add Checked Subjects"); addCheckedBtn.setFont(new Font("Segoe UI", Font.BOLD, 13)); addCheckedBtn.setBackground(Theme.getPrimary()); addCheckedBtn.setForeground(Color.WHITE); addCheckedBtn.setFocusPainted(false); addCheckedBtn.setBorderPainted(false); addCheckedBtn.setCursor(new Cursor(Cursor.HAND_CURSOR)); addCheckedBtn.setPreferredSize(new Dimension(450, 40)); addCheckedBtn.setMaximumSize(new Dimension(450, 40));

        final List<SubjectEntry> entries = new ArrayList<>();
        addCheckedBtn.addActionListener(e -> {
            String sec = sectionForChecked.getText().trim();
            for (JCheckBox cb : boxes) {
                if (cb.isSelected()) {
                    String subj = cb.getText(); boolean already = false;
                    for (SubjectEntry se : entries) if (se.subject.equals(subj)) already = true;
                    if (!already) { entries.add(new SubjectEntry(subj, sec)); selectedModel.addElement(sec.isEmpty() ? subj : subj + "  [" + sec + "]"); }
                    cb.setSelected(false);
                }
            }
        });

        addCustomBtn.addActionListener(e -> {
            String subj = customSubjectField.getText().trim(); if (subj.isEmpty()) return;
            String sec = customSectionField.getText().trim(); entries.add(new SubjectEntry(subj, sec)); selectedModel.addElement(sec.isEmpty() ? subj : subj + "  [" + sec + "]");
            customSubjectField.setText(""); customSectionField.setText("");
        });

        removeSelectedBtn.addActionListener(e -> {
            int idx = selectedList.getSelectedIndex();
            if (idx >= 0 && idx < entries.size()) { entries.remove(idx); selectedModel.remove(idx); }
        });

        JButton doneBtn = new JButton("Complete Registration"); doneBtn.setFont(new Font("Segoe UI", Font.BOLD, 14)); doneBtn.setForeground(Color.WHITE); doneBtn.setBackground(Theme.EMERALD); doneBtn.setFocusPainted(false); doneBtn.setBorderPainted(false); doneBtn.setPreferredSize(new Dimension(450, 46)); doneBtn.setMaximumSize(new Dimension(450, 46)); doneBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JButton skipBtn = new JButton("Skip — Add Subjects Later"); skipBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12)); skipBtn.setForeground(Theme.muted()); skipBtn.setBackground(Theme.card()); skipBtn.setBorderPainted(false); skipBtn.setFocusPainted(false); skipBtn.setContentAreaFilled(false); skipBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        doneBtn.addActionListener(e -> dlg.dispose()); skipBtn.addActionListener(e -> { entries.clear(); dlg.dispose(); });

        JPanel customRow = new JPanel(new BorderLayout(8, 0)); customRow.setBackground(Theme.card()); customRow.setMaximumSize(new Dimension(450, 40)); customRow.setPreferredSize(new Dimension(450, 40)); customRow.add(customSubjectField, BorderLayout.CENTER); customRow.add(customSectionField, BorderLayout.EAST); customRow.add(addCustomBtn, BorderLayout.WEST);

        g.gridy = row++; g.insets=new Insets(0,0,6,0); panel.add(new JLabel("1. Choose from common subjects:"), g); g.gridy = row++; g.insets=new Insets(0,0,6,0); panel.add(checkScroll, g); g.gridy = row++; g.insets=new Insets(0,0,4,0); panel.add(new JLabel("Section for checked subjects (optional):"), g); g.gridy = row++; g.insets=new Insets(0,0,6,0); panel.add(sectionForChecked, g); g.gridy = row++; g.insets=new Insets(0,0,12,0); panel.add(addCheckedBtn, g); g.gridy = row++; g.insets=new Insets(0,0,4,0); panel.add(new JLabel("2. Or add a custom subject:"), g); g.gridy = row++; g.insets=new Insets(0,0,12,0); panel.add(customRow, g); g.gridy = row++; g.insets=new Insets(0,0,4,0); panel.add(selectedScroll, g); JPanel removeBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); removeBtnRow.setBackground(Theme.card()); removeBtnRow.add(removeSelectedBtn); g.gridy = row++; g.insets=new Insets(0,0,16,0); panel.add(removeBtnRow, g); g.gridy = row++; g.insets=new Insets(0,0,6,0); panel.add(doneBtn, g); g.gridy = row++; g.insets=new Insets(0,0,0,0); panel.add(skipBtn, g);

        JScrollPane scroll = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); scroll.getVerticalScrollBar().setUnitIncrement(16); scroll.setBorder(BorderFactory.createEmptyBorder()); dlg.add(scroll); dlg.pack(); dlg.setSize(520, Math.min(dlg.getHeight(), 600)); dlg.setLocationRelativeTo(parent); dlg.setVisible(true);
        return entries;
    }

    private static void finishStudentSave(Component parent, Step1Result info, boolean emailVerified) {
        Student student = new Student(info.id, info.name, info.email, info.dept, info.sem, info.password, info.college);
        DataManager.saveStudent(student);
        if (emailVerified) DataManager.markEmailVerified(info.id);
        JOptionPane.showMessageDialog(parent, "<html><b>Registration successful!</b><br>" + (emailVerified ? "Email verified. " : "") + "You can now sign in with your Student ID.<br><small>Institution: " + info.college + "</small></html>", "Welcome to SCIS", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void finishTeacherSave(Component parent, Teacher t, boolean verified) {
        TeacherDataManager.saveTeacher(t);
        if (verified) TeacherDataManager.markEmailVerified(t.teacherId);
        JOptionPane.showMessageDialog(parent, "<html><b>Teacher account created!</b><br>" + (verified ? "Email verified. " : "") + "Sign in with your Teacher ID.<br>Subjects: " + t.subjectClassMap.size() + " registered.</html>", "Welcome", JOptionPane.INFORMATION_MESSAGE);
    }

    private static JLabel fieldLabel(String text) { JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.PLAIN, 12)); l.setForeground(Theme.text()); return l; }
    private static int addGridRow(JPanel panel, GridBagConstraints g, int row, String labelText, JComponent field, JLabel extraLabel) { g.gridy = row; g.insets = new Insets(0,0,3,0); panel.add(fieldLabel(labelText), g); g.gridy = row + 1; g.insets = new Insets(0,0,2,0); panel.add(field, g); g.gridy = row + 2; g.insets = new Insets(0,0,10,0); panel.add(extraLabel != null ? extraLabel : Box.createRigidArea(new Dimension(0,1)), g); return row + 3; }
    private static JTextField styledField() { JTextField tf = new JTextField(22); tf.setFont(new Font("Segoe UI", Font.PLAIN, 13)); tf.setForeground(Theme.text()); tf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(203,213,225), 1, true), BorderFactory.createEmptyBorder(6,10,6,10))); tf.setBackground(Theme.surface()); return tf; }
    private static JPasswordField styledPassField() { JPasswordField pf = new JPasswordField(22); pf.setFont(new Font("Segoe UI", Font.PLAIN, 13)); pf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(203,213,225), 1, true), BorderFactory.createEmptyBorder(6,10,6,10))); pf.setBackground(Theme.surface()); return pf; }

    private static class Step1Result {
        final String id, name, email, college, dept, desig, password; final int sem;
        Step1Result(String id, String n, String e, String c, String d, int s, String ds, String pw) { this.id=id; name=n; email=e; college=c; dept=d; sem=s; desig=ds; password=pw; }
    }
    static class SubjectEntry {
        final String subject, section;
        SubjectEntry(String s, String sec) { subject=s; section=sec==null?"":sec; }
    }
}
