package com.scis.ui;

import com.scis.auth.LoginThrottle;
import com.scis.db.DataManager;
import com.scis.model.Student;
import com.scis.teacher.db.TeacherDataManager;
import com.scis.teacher.model.Teacher;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * LoginScreen — login card with security hardening:
 * <ul>
 *   <li>Account lockout after {@value LoginThrottle#MAX_ATTEMPTS} failures</li>
 *   <li>Remaining-attempts warning shown on each failure</li>
 *   <li>Show / hide password toggle</li>
 *   <li>"Forgot password?" → {@link ForgotPasswordDialog}</li>
 *   <li>Card shake animation on wrong password</li>
 * </ul>
 */
public final class LoginScreen {

    public interface LoginCallback {
        void onStudentLogin(Student student);
        void onTeacherLogin(Teacher teacher);
        void onBack();
    }

    private LoginScreen() {}

    public static JPanel build(LoginCallback callback) {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (Theme.currentRole == Theme.Role.STUDENT) {
                    g2.setPaint(new GradientPaint(0,0,new Color(10,15,40),getWidth(),getHeight(),new Color(20,40,90)));
                    g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(new Color(59,130,246,30)); g2.fillOval(-80,-80,300,300);
                    g2.setColor(new Color(168,85,247,20)); g2.fillOval(getWidth()-180,getHeight()-180,320,320);
                } else {
                    g2.setPaint(new GradientPaint(0,0,new Color(8,30,28),getWidth(),getHeight(),new Color(15,60,55)));
                    g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(new Color(13,148,136,30)); g2.fillOval(-80,-80,300,300);
                    g2.setColor(new Color(16,185,129,20)); g2.fillOval(getWidth()-180,getHeight()-180,320,320);
                }
            }
        };

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Theme.card());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.currentRole == Theme.Role.STUDENT ? new Color(220,230,250) : new Color(167,243,208),1,true),
            BorderFactory.createEmptyBorder(44,50,40,50)));
        card.setPreferredSize(new Dimension(440,620));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0; g.fill=GridBagConstraints.HORIZONTAL; g.anchor=GridBagConstraints.CENTER;
        g.weightx=1.0;

        // Logo
        JPanel iconWrap = new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));
        iconWrap.setBackground(Theme.card());
        iconWrap.setMaximumSize(new Dimension(340, 64));
        iconWrap.setPreferredSize(new Dimension(340, 64));
        iconWrap.add(buildLogoIcon());

        // Titles
        JLabel appName = new JLabel("SCIS" + (Theme.currentRole == Theme.Role.TEACHER ? " — Teacher Portal" : ""), SwingConstants.CENTER);
        appName.setFont(new Font("Segoe UI",Font.BOLD, Theme.currentRole == Theme.Role.TEACHER ? 24 : 32)); appName.setForeground(Theme.text());
        appName.setMaximumSize(new Dimension(340,44));
        JLabel subtitle = new JLabel("Smart Campus Intelligence System", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI",Font.PLAIN,13)); subtitle.setForeground(Theme.muted());
        subtitle.setMaximumSize(new Dimension(340,24));

        // Divider
        JPanel divider = buildDivider();

        // Fields
        String idLabelText = Theme.currentRole == Theme.Role.STUDENT ? "Student ID" : "Teacher ID";
        JLabel idLabel = UIFactory.loginFieldLabel(idLabelText);
        JTextField idField = new JTextField(); UIFactory.styleLoginField(idField);
        JLabel pwLabel = UIFactory.loginFieldLabel("Password");
        JPasswordField pwField = new JPasswordField(); UIFactory.styleLoginField(pwField);
        JPanel pwRow = buildPasswordRow(pwField);

        // Forgot password link
        JPanel forgotRow = new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        forgotRow.setBackground(Theme.card()); forgotRow.setMaximumSize(new Dimension(340, 20));
        forgotRow.setPreferredSize(new Dimension(340, 20));
        JButton forgotBtn = new JButton("Forgot password?");
        forgotBtn.setFont(new Font("Segoe UI",Font.PLAIN,11)); forgotBtn.setForeground(Theme.getPrimary());
        forgotBtn.setBackground(Theme.card()); forgotBtn.setBorderPainted(false);
        forgotBtn.setFocusPainted(false); forgotBtn.setContentAreaFilled(false);
        forgotBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotBtn.addActionListener(e -> ForgotPasswordDialog.show(panel));
        forgotRow.add(forgotBtn);

        // Status label
        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("Segoe UI",Font.PLAIN,11)); statusLbl.setForeground(Theme.RED);
        statusLbl.setHorizontalAlignment(SwingConstants.CENTER);
        statusLbl.setMaximumSize(new Dimension(340,18));

        // Login button
        JButton loginBtn = buildLoginButton();

        // Register and Back link row
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER,8,0));
        linkRow.setBackground(Theme.card());
        linkRow.setMaximumSize(new Dimension(340, 30));
        linkRow.setPreferredSize(new Dimension(340, 30));
        
        JButton backBtn = new JButton("← Back");
        backBtn.setFont(new Font("Segoe UI",Font.BOLD,12)); backBtn.setForeground(Theme.getPrimary());
        backBtn.setBackground(Theme.card()); backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false); backBtn.setContentAreaFilled(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> callback.onBack());
        
        JLabel linkLbl = new JLabel(Theme.currentRole == Theme.Role.STUDENT ? "New student?" : "New teacher?");
        linkLbl.setFont(new Font("Segoe UI",Font.PLAIN,12)); linkLbl.setForeground(Theme.muted());
        JButton regBtn = new JButton("Register here");
        regBtn.setFont(new Font("Segoe UI",Font.BOLD,12)); regBtn.setForeground(Theme.getPrimary());
        regBtn.setBackground(Theme.card()); regBtn.setBorderPainted(false);
        regBtn.setFocusPainted(false); regBtn.setContentAreaFilled(false);
        regBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        regBtn.addActionListener(e -> RegistrationDialog.show(panel));
        
        linkRow.add(backBtn);
        linkRow.add(linkLbl); 
        linkRow.add(regBtn);

        // Assemble card rows
        int r = 0;
        addRow(card,g,r++,new Insets(0,0,10,0),iconWrap);
        addRow(card,g,r++,new Insets(0,0, 4,0),appName);
        addRow(card,g,r++,new Insets(0,0,18,0),subtitle);
        addRow(card,g,r++,new Insets(0,0,18,0),divider);
        addRow(card,g,r++,new Insets(0,0, 4,0),idLabel);
        addRow(card,g,r++,new Insets(0,0,14,0),idField);
        addRow(card,g,r++,new Insets(0,0, 4,0),pwLabel);
        addRow(card,g,r++,new Insets(0,0, 4,0),pwRow);
        addRow(card,g,r++,new Insets(0,0, 8,0),forgotRow);
        addRow(card,g,r++,new Insets(0,0, 4,0),statusLbl);
        addRow(card,g,r++,new Insets(0,0,14,0),loginBtn);
        addRow(card,g,r,  new Insets(0,0, 0,0),linkRow);

        // Login action (shared by button + Enter on fields)
        ActionListener doLogin = e -> {
            String id = idField.getText().trim();
            String pw = new String(pwField.getPassword());
            if (id.isEmpty() || pw.isEmpty()) {
                statusLbl.setText("Enter your ID and password.");
                return;
            }
            if (LoginThrottle.isLockedOut(id)) {
                long mins = LoginThrottle.minutesRemaining(id);
                statusLbl.setText("Account locked. Try again in " + mins + " min.");
                loginBtn.setEnabled(false);
                return;
            }
            
            boolean success = false;
            if (Theme.currentRole == Theme.Role.STUDENT) {
                Student student = DataManager.loadStudent(id, pw);
                if (student != null) {
                    success = true;
                    LoginThrottle.recordSuccess(id);
                    statusLbl.setText(" "); loginBtn.setEnabled(true);
                    callback.onStudentLogin(student);
                }
            } else {
                Teacher teacher = TeacherDataManager.loadTeacher(id, pw);
                if (teacher != null) {
                    success = true;
                    LoginThrottle.recordSuccess(id);
                    statusLbl.setText(" "); loginBtn.setEnabled(true);
                    callback.onTeacherLogin(teacher);
                }
            }
            
            if (!success) {
                LoginThrottle.recordFailure(id);
                if (LoginThrottle.isLockedOut(id)) {
                    statusLbl.setText("Too many failures — locked for "
                        + LoginThrottle.LOCKOUT_MINUTES + " min.");
                    loginBtn.setEnabled(false);
                } else {
                    int left = LoginThrottle.attemptsRemaining(id);
                    statusLbl.setText(left <= 2
                        ? "Invalid credentials. " + left + " attempt(s) left before lockout."
                        : "Invalid credentials. Please try again.");
                }
                shakeCard(card);
            }
        };
        loginBtn.addActionListener(doLogin);
        idField.addActionListener(doLogin);
        pwField.addActionListener(doLogin);

        JPanel shadow = buildShadowWrapper(card);
        panel.add(shadow);
        return panel;
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private static JPanel buildLogoIcon() {
        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics gr) {
                super.paintComponent(gr);
                Graphics2D g2=(Graphics2D)gr;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.getPrimary()); g2.fillOval(0,0,64,64);
                g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI",Font.BOLD,28));
                FontMetrics fm=g2.getFontMetrics();
                String letter = Theme.currentRole == Theme.Role.STUDENT ? "S" : "T";
                g2.drawString(letter,(64-fm.stringWidth(letter))/2,64-(64-fm.getAscent())/2-2);
            }
        };
        logo.setOpaque(false); logo.setPreferredSize(new Dimension(64,64)); logo.setMaximumSize(new Dimension(64,64));
        return logo;
    }

    private static JPanel buildDivider() {
        JPanel d = new JPanel() {
            @Override protected void paintComponent(Graphics gr) {
                super.paintComponent(gr);
                ((Graphics2D)gr).setColor(Theme.currentRole == Theme.Role.STUDENT ? new Color(230,236,248) : new Color(167,243,208));
                gr.drawLine(0,getHeight()/2,getWidth(),getHeight()/2);
            }
        };
        d.setOpaque(false); d.setMaximumSize(new Dimension(340,12)); d.setPreferredSize(new Dimension(340,12));
        return d;
    }

    private static JPanel buildPasswordRow(JPasswordField pwField) {
        JPanel row = new JPanel(new BorderLayout(4,0));
        row.setBackground(Theme.card()); row.setMaximumSize(new Dimension(340,44));
        JToggleButton eye = new JToggleButton("Show");
        eye.setFont(new Font("Segoe UI",Font.PLAIN,10)); eye.setFocusPainted(false);
        eye.setBorderPainted(false); eye.setBackground(Theme.surface());
        eye.setForeground(Theme.muted()); eye.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eye.setPreferredSize(new Dimension(52,44));
        eye.addActionListener(e -> {
            if (eye.isSelected()) { pwField.setEchoChar((char)0); eye.setText("Hide"); }
            else { pwField.setEchoChar('•'); eye.setText("Show"); }
        });
        row.add(pwField,BorderLayout.CENTER); row.add(eye,BorderLayout.EAST);
        return row;
    }

    private static JButton buildLoginButton() {
        JButton btn = new JButton("Sign In") {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g2=(Graphics2D)gr;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color base=isEnabled()?Theme.getPrimary():new Color(148,163,184);
                Color dark=isEnabled()?(Theme.currentRole == Theme.Role.STUDENT ? Theme.BLUE_DARK : Theme.TEACHER_TEAL_DARK):new Color(148,163,184);
                g2.setPaint(new GradientPaint(0,0,base,getWidth(),getHeight(),dark));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(Color.WHITE); g2.setFont(getFont());
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setFont(new Font("Segoe UI",Font.BOLD,15)); btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(340,46)); btn.setMaximumSize(new Dimension(340,46));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }

    private static JPanel buildShadowWrapper(JPanel inner) {
        JPanel shadow = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics gr) {
                super.paintComponent(gr);
                Graphics2D g2=(Graphics2D)gr;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                for(int i=8;i>0;i--){g2.setColor(new Color(0,0,0,i*4));g2.fillRoundRect(i,i+4,getWidth()-i*2,getHeight()-i*2,18,18);}
            }
        };
        shadow.setOpaque(false); shadow.add(inner);
        return shadow;
    }

    private static void shakeCard(JPanel card) {
        Timer t = new Timer(28, null);
        int[] step={0};
        int[] offsets={0,-10,10,-8,8,-5,5,-2,2,0};
        t.addActionListener(e->{
            if(step[0]>=offsets.length){((Timer)e.getSource()).stop();return;}
            card.setLocation(card.getX()+offsets[step[0]++],card.getY());
        });
        t.start();
    }

    private static void addRow(JPanel card,GridBagConstraints g,int row,Insets insets,Component comp){
        g.gridy=row; g.insets=insets; card.add(comp,g);
    }
}
