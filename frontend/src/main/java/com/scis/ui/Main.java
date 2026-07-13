package com.scis.ui;

import com.scis.model.Student;
import com.scis.teacher.model.Teacher;
import com.scis.ui.teacher.*;
import com.scis.auth.LoginThrottle;
import com.scis.teacher.db.TeacherDataManager;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

public class Main extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     mainPanel  = new JPanel(cardLayout);
    private String activeSection = "Dashboard";
    private JPanel dashboardRoot;
    private Object currentUser;

    public Main() {
        setTitle(Theme.currentRole == Theme.Role.STUDENT ? "SCIS — Student Portal" : "SCIS — Teacher Portal");
        setSize(Theme.currentRole == Theme.Role.STUDENT ? 900 : 1480, Theme.currentRole == Theme.Role.STUDENT ? 800 : 940);
        setMinimumSize(new Dimension(800, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        applySystemLAF();

        mainPanel.add(LoginScreen.build(new LoginScreen.LoginCallback() {
            @Override public void onStudentLogin(Student student) { onLogin(student); }
            @Override public void onTeacherLogin(Teacher teacher) { onLogin(teacher); }
            @Override public void onBack() { onBackToChooser(); }
        }), "login");
        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    private void onLogin(Object user) {
        this.currentUser = user;
        rebuildDashboard(user, "Dashboard");
    }
    
    private void onBackToChooser() {
        dispose();
        Main.showRoleChooser();
    }

    private void rebuildDashboard(Object user, String section) {
        if (user == null) return;
        activeSection = section;
        if (dashboardRoot != null) mainPanel.remove(dashboardRoot);
        
        if (Theme.currentRole == Theme.Role.STUDENT) {
            dashboardRoot = buildStudentDashboardRoot((Student) user);
        } else {
            dashboardRoot = buildTeacherDashboardRoot((Teacher) user);
        }
        UIFactory.applyThemeTree(dashboardRoot);
        
        mainPanel.add(dashboardRoot, "dashboard");
        cardLayout.show(mainPanel, "dashboard");
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private JPanel buildStudentDashboardRoot(Student student) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.bg());

        root.add(
            HeaderPanel.build(
                student,
                () -> { if (JOptionPane.showConfirmDialog(this, "Logout?", "Confirm",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    cardLayout.show(mainPanel, "login"); },
                () -> { Theme.darkMode = !Theme.darkMode;
                    
                    Theme.syncUIManager();
                    rebuildDashboard(student, activeSection); }),
            BorderLayout.NORTH);

        CardLayout contentCL = new CardLayout();
        JPanel content = new JPanel(contentCL);
        content.setBackground(Theme.bg());

        Runnable onDataChange = () -> rebuildDashboard(student, activeSection);

        content.add(DashboardTab.build(student),                "Dashboard");
        content.add(AttendanceTab.build(student, onDataChange), "Attendance");
        content.add(MarksTab.build(student, onDataChange),      "Marks");
        content.add(TasksTab.build(student, onDataChange),      "Tasks");
        content.add(PerformanceTab.build(student),              "Performance");
        content.add(MLTab.build(student),                       "ML Analytics");
        content.add(MedicalLeaveTab.build(student, onDataChange),"Medical Leave");
        content.add(SearchTab.build(student),                   "Search & Filter");
        content.add(ReportsTab.build(student),                  "Reports");

        contentCL.show(content, activeSection);
        root.add(content, BorderLayout.CENTER);

        JPanel sidebar = SidebarPanel.build(student, activeSection, navItem -> {
            activeSection = navItem;
            contentCL.show(content, navItem);
            rebuildDashboard(student, navItem);
        });
        root.add(sidebar, BorderLayout.WEST);
        return root;
    }


    private static final String[] NAV_ITEMS = {
        "Dashboard", "Attendance", "Marks & Grades",
        "Assignments", "Students", "Medical Leave", "Announcements", "Subjects"
    };

    private JPanel buildTeacherDashboardRoot(Teacher teacher) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.bg());

        // Header
        root.add(buildHeader(teacher), BorderLayout.NORTH);

        // Content
        CardLayout contentCL = new CardLayout();
        JPanel content = new JPanel(contentCL);
        content.setBackground(Theme.bg());

        Runnable onChange = () -> rebuildDashboard(teacher, activeSection);

        content.add(TeacherDashboardTab.build(teacher),                   "Dashboard");
        content.add(TeacherAttendanceTab.build(teacher, onChange),        "Attendance");
        content.add(TeacherMarksTab.build(teacher, onChange),             "Marks & Grades");
        content.add(TeacherAssignmentsTab.build(teacher, onChange),       "Assignments");
        content.add(TeacherStudentsTab.build(teacher),                    "Students");
        content.add(TeacherMedicalLeaveTab.build(teacher, onChange),      "Medical Leave");
        content.add(TeacherAnnouncementsTab.build(teacher, onChange),     "Announcements");
        content.add(TeacherSubjectsTab.build(teacher, onChange),          "Subjects");

        contentCL.show(content, activeSection);
        root.add(content, BorderLayout.CENTER);

        // Sidebar
        root.add(buildSidebar(teacher, contentCL, content), BorderLayout.WEST);
        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader(Teacher teacher) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.surface());
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.border()),
            BorderFactory.createEmptyBorder(12,20,12,20)));

        JLabel appLbl = new JLabel("SCIS — Teacher Portal");
        appLbl.setFont(new Font("Segoe UI",Font.BOLD,20)); appLbl.setForeground(Theme.TEACHER_TEAL);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0));
        right.setBackground(Theme.surface());

        JLabel userLbl = new JLabel(teacher.name + "  |  " + teacher.designation
            + " · " + teacher.department);
        userLbl.setFont(new Font("Segoe UI",Font.PLAIN,12)); userLbl.setForeground(Theme.muted());

        JToggleButton darkBtn = new JToggleButton(Theme.darkMode ? "Light Mode" : "Dark Mode");
        darkBtn.setSelected(Theme.darkMode);
        darkBtn.setFont(new Font("Segoe UI",Font.BOLD,11));
        darkBtn.setBackground(Theme.darkMode ? new Color(30,60,50) : new Color(236,253,245));
        darkBtn.setForeground(Theme.text()); darkBtn.setFocusPainted(false);
        darkBtn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.border(),1,true),
            BorderFactory.createEmptyBorder(5,12,5,12)));
        darkBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        darkBtn.addActionListener(e -> {
            Theme.darkMode = !Theme.darkMode;
            
            Theme.syncUIManager();
            rebuildDashboard(teacher, activeSection);
        });

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI",Font.BOLD,12));
        logoutBtn.setBackground(new Color(239,68,68)); logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false); logoutBtn.setBorderPainted(false);
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(6,14,6,14));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,"Logout?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                cardLayout.show(mainPanel,"login");
        });

        right.add(userLbl); right.add(darkBtn); right.add(logoutBtn);
        header.add(appLbl, BorderLayout.WEST);
        header.add(right,  BorderLayout.EAST);
        return header;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private JPanel buildSidebar(Teacher teacher,
                                 CardLayout contentCL, JPanel content) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.sidebar());
        sidebar.setPreferredSize(new Dimension(220,0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0,0,0,1,new Color(20,50,45)));

        sidebar.add(Box.createRigidArea(new Dimension(0,20)));

        // Avatar
        JPanel avPanel = new JPanel();
        avPanel.setLayout(new BoxLayout(avPanel,BoxLayout.Y_AXIS));
        avPanel.setBackground(Theme.sidebar());
        avPanel.setMaximumSize(new Dimension(220,100));
        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(13,148,136,200)); g2.fillOval(4,0,52,52);
                g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI",Font.BOLD,26));
                FontMetrics fm=g2.getFontMetrics();
                String init=teacher.name.length()>0?String.valueOf(teacher.name.charAt(0)).toUpperCase():"T";
                g2.drawString(init,4+(52-fm.stringWidth(init))/2,(52+fm.getAscent())/2-2);
            }
        };
        av.setOpaque(false); av.setPreferredSize(new Dimension(60,52));
        av.setMaximumSize(new Dimension(60,52)); av.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel nameLbl=new JLabel(teacher.name.length()>16?teacher.name.substring(0,16)+"…":teacher.name);
        nameLbl.setFont(new Font("Segoe UI",Font.BOLD,13)); nameLbl.setForeground(Color.WHITE);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel desigLbl=new JLabel(teacher.designation);
        desigLbl.setFont(new Font("Segoe UI",Font.PLAIN,11)); desigLbl.setForeground(new Color(110,170,155));
        desigLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        avPanel.add(av); avPanel.add(Box.createRigidArea(new Dimension(0,4)));
        avPanel.add(nameLbl); avPanel.add(Box.createRigidArea(new Dimension(0,2)));
        avPanel.add(desigLbl);
        sidebar.add(avPanel);
        sidebar.add(Box.createRigidArea(new Dimension(0,20)));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(190,1)); sep.setForeground(new Color(30,60,55));
        sidebar.add(sep);
        sidebar.add(Box.createRigidArea(new Dimension(0,8)));

        String[] icons = {"grid","calendar","chart","check","people","heart","bell","book"};
        for (int i=0; i<NAV_ITEMS.length; i++) {
            final String item=NAV_ITEMS[i]; final String icon=icons[i];
            final boolean active=item.equals(activeSection);
            JButton btn = new JButton(item) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2=(Graphics2D)g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground()); g2.fillRect(0,0,getWidth(),getHeight());
                    drawSidebarIcon(g2,icon,18,(getHeight()-16)/2,16,
                        active?Color.WHITE:new Color(110,170,155));
                    g2.setColor(getForeground()); g2.setFont(getFont());
                    FontMetrics fm=g2.getFontMetrics();
                    g2.drawString(getText(),44,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                }
            };
            btn.setFont(new Font("Segoe UI",active?Font.BOLD:Font.PLAIN,13));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(220,44)); btn.setPreferredSize(new Dimension(220,44));
            btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false);
            btn.setBorder(BorderFactory.createEmptyBorder()); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBackground(active?Theme.TEACHER_TEAL:Theme.sidebar());
            btn.setForeground(active?Color.WHITE:new Color(110,170,155));
            btn.addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){if(!btn.getBackground().equals(Theme.TEACHER_TEAL))btn.setBackground(new Color(20,60,55));}
                public void mouseExited(MouseEvent e){if(!btn.getBackground().equals(Theme.TEACHER_TEAL))btn.setBackground(Theme.sidebar());}
            });
            btn.addActionListener(e -> {
                activeSection=item; contentCL.show(content,item);
                rebuildDashboard(teacher,item);
            });
            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0,2)));
        }
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    /** Minimal vector icon set for teacher sidebar. */
    private void drawSidebarIcon(Graphics2D g2, String type,
                                  int x, int y, int s, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch (type) {
            case "grid":
                int sq=s/2-1;
                g2.fillRoundRect(x,y,sq,sq,2,2); g2.fillRoundRect(x+sq+2,y,sq,sq,2,2);
                g2.fillRoundRect(x,y+sq+2,sq,sq,2,2); g2.fillRoundRect(x+sq+2,y+sq+2,sq,sq,2,2); break;
            case "calendar":
                g2.drawRoundRect(x,y+2,s-1,s-3,3,3); g2.drawLine(x,y+6,x+s-1,y+6);
                g2.fillRect(x+3,y,2,5); g2.fillRect(x+s-5,y,2,5);
                g2.fillRect(x+3,y+9,3,3); g2.fillRect(x+8,y+9,3,3); break;
            case "chart":
                g2.fillRect(x,y+s-5,4,5); g2.fillRect(x+5,y+s-9,4,9);
                g2.fillRect(x+10,y+s-6,4,6); g2.fillRect(x+s-4,y,4,s); break;
            case "check":
                g2.drawRoundRect(x,y,s-1,s-1,3,3);
                g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.drawLine(x+3,y+s/2,x+s/2-1,y+s-4); g2.drawLine(x+s/2-1,y+s-4,x+s-3,y+3); break;
            case "people":
                g2.fillOval(x+2,y,6,6); g2.drawArc(x,y+7,10,8,0,180);
                g2.fillOval(x+s-8,y,6,6); g2.drawArc(x+s-10,y+7,10,8,0,180); break;
            case "heart": // medical/leave
                int hcx=x+s/2, hcy=y+4, hr=4;
                g2.fillArc(hcx-hr,hcy-hr,hr*2,hr*2,0,180);
                g2.fillArc(hcx,hcy-hr,hr*2,hr*2,0,180);
                int[] hxp={hcx-hr,hcx+hr*2,hcx+s/2-2,hcx+s/2+2};
                // simple filled polygon for heart bottom
                g2.fillOval(x+1, y+2, s-2, s-4); // approximate filled heart
                break;
            case "bell":
                g2.drawArc(x+2,y,s-4,s-4,0,180);
                g2.drawLine(x+2,y+s/2,x+2,y+s-3); g2.drawLine(x+s-3,y+s/2,x+s-3,y+s-3);
                g2.drawLine(x+2,y+s-3,x+s-3,y+s-3); g2.drawArc(x+s/2-2,y+s-3,5,4,180,180); break;
            case "book":
                g2.drawRoundRect(x+1,y,s-3,s-1,3,3);
                g2.drawLine(x+4,y+4,x+s-5,y+4); g2.drawLine(x+4,y+7,x+s-5,y+7);
                g2.drawLine(x+4,y+10,x+s-8,y+10); break;
            default: g2.fillRoundRect(x+2,y+2,s-4,s-4,4,4);
        }
        g2.setStroke(new BasicStroke());
    }

    private static void addRow(JPanel card, GridBagConstraints g,
                                int row, Insets insets, Component comp) {
        g.gridy=row; g.insets=insets; card.add(comp,g);
    }

    /**
     * Applies login-field styling using TeacherTheme colours (not the global Theme),
     * so the teacher login form always looks correct regardless of the global dark-mode flag.
     */
    private static void styleTeacherLoginField(JComponent tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setPreferredSize(new Dimension(340, 44));
        tf.setMaximumSize(new Dimension(340, 44));
        tf.setAlignmentX(Component.CENTER_ALIGNMENT);
        tf.setBackground(Theme.surface());
        tf.setForeground(Theme.text());
        tf.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(Theme.border(), 1, true),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        if (tf instanceof JTextField)
            ((JTextField) tf).setCaretColor(Theme.TEACHER_TEAL);
        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(Theme.TEACHER_TEAL, 2, true),
                    BorderFactory.createEmptyBorder(7, 13, 7, 13)));
                tf.setBackground(Theme.card());
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(Theme.border(), 1, true),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
                tf.setBackground(Theme.surface());
            }
        });
    }


    private static void applySystemLAF() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ENTRY POINT  —  Role-chooser full window
    // ═════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.setProperty("swing.useSystemFontSettings", "false");
        applySystemLAF();
        SwingUtilities.invokeLater(Main::showRoleChooser);
    }

    /**
     * Opens a full-size role-chooser JFrame.
     * Avoids the broken-dialog sizing issues entirely.
     */
    public static void showRoleChooser() {

        JFrame frame = new JFrame("SCIS — Smart Campus Intelligence System");
        frame.setSize(860, 540);
        frame.setMinimumSize(new Dimension(720, 460));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);

        // ── Full background ───────────────────────────────────────────────────
        JPanel bg = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Deep navy gradient
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(8, 12, 35),
                    getWidth(), getHeight(), new Color(18, 30, 70)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Decorative glows
                g2.setColor(new Color(59, 130, 246, 25));
                g2.fillOval(-100, -100, 380, 380);
                g2.setColor(new Color(13, 148, 136, 18));
                g2.fillOval(getWidth() - 220, getHeight() - 220, 380, 380);
                g2.setColor(new Color(139, 92, 246, 12));
                g2.fillOval(getWidth() / 2 - 120, getHeight() - 160, 280, 280);
            }
        };

        // ── Centre card ───────────────────────────────────────────────────────
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Subtle shadow
                for (int i = 8; i > 0; i--) {
                    g2.setColor(new Color(0, 0, 0, i * 5));
                    g2.fillRoundRect(i, i + 3,
                        getWidth() - i * 2, getHeight() - i * 2, 20, 20);
                }
                // Card face
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 56, 48, 56));
        card.setPreferredSize(new Dimension(560, 380));
        card.setMaximumSize(new Dimension(560, 380));

        // ── Logo circle ───────────────────────────────────────────────────────
        JPanel logoPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Outer glow ring
                g2.setColor(new Color(59, 130, 246, 40));
                g2.fillOval(2, 2, 68, 68);
                // Main circle
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(59, 130, 246),
                    72, 72, new Color(37, 99, 235)));
                g2.fillOval(8, 8, 56, 56);
                // Letter — always white on the blue circle
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("S",
                    8 + (56 - fm.stringWidth("S")) / 2,
                    8 + (56 + fm.getAscent() - fm.getDescent()) / 2 - 1);
            }
        };
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(72, 72));
        logoPanel.setMaximumSize(new Dimension(72, 72));
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Titles ────────────────────────────────────────────────────────────
        JLabel titleLbl = new JLabel("SCIS", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLbl.setForeground(Color.WHITE); // role chooser bg is always dark navy
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLbl = new JLabel(
            "Smart Campus Intelligence System", SwingConstants.CENTER);
        subtitleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLbl.setForeground(new Color(200, 215, 240)); // soft blue-white on dark bg
        subtitleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Divider ───────────────────────────────────────────────────────────
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(100, 130, 180)); // visible on dark navy bg
                g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            }
        };
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(500, 14));
        divider.setPreferredSize(new Dimension(500, 14));
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Portal prompt ─────────────────────────────────────────────────────
        JLabel promptLbl = new JLabel("Select your portal to continue",
            SwingConstants.CENTER);
        promptLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        promptLbl.setForeground(new Color(200, 215, 240)); // soft blue-white on dark bg
        promptLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Portal buttons ────────────────────────────────────────────────────
        JButton studentBtn = buildPortalButton(
            "Student Portal",
            "Login or register as a student",
            new Color(59, 130, 246),
            new Color(37, 99, 235));

        JButton teacherBtn = buildPortalButton(
            "Teacher Portal",
            "Login or register as a faculty member",
            new Color(13, 148, 136),
            new Color(15, 118, 110));

        // ── Button row — each button takes exactly half width ─────────────────
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 16, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(500, 76));
        btnRow.setPreferredSize(new Dimension(500, 76));
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRow.add(studentBtn);
        btnRow.add(teacherBtn);

        // ── Assemble card ─────────────────────────────────────────────────────
        card.add(logoPanel);
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(titleLbl);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(subtitleLbl);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(divider);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(promptLbl);
        card.add(Box.createRigidArea(new Dimension(0, 22)));
        card.add(btnRow);

        bg.add(card);
        frame.add(bg);

        // ── Actions ───────────────────────────────────────────────────────────
        studentBtn.addActionListener(e -> {
            Theme.currentRole = Theme.Role.STUDENT;
            Theme.syncUIManager();
            frame.dispose();
            new Main().setVisible(true);
        });
        teacherBtn.addActionListener(e -> {
            Theme.currentRole = Theme.Role.TEACHER;
            Theme.syncUIManager();
            frame.dispose();
            new Main().setVisible(true);
        });

        frame.setVisible(true);
    }

    /**
     * Builds a full-height portal button with a title and subtitle line.
     * Uses a GridLayout inside the row so both buttons are always equal width.
     */
    private static JButton buildPortalButton(String title, String subtitle,
                                              Color colorTop, Color colorBot) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Hover: slightly lighter
                Color top = getModel().isRollover()
                    ? colorTop.brighter() : colorTop;
                Color bot = getModel().isRollover()
                    ? colorBot.brighter() : colorBot;
                g2.setPaint(new GradientPaint(0, 0, top,
                    getWidth(), getHeight(), bot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Title text — always white on the colored button
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                FontMetrics fmT = g2.getFontMetrics();
                // Centre both lines together: shift title up half a line
                int totalH = fmT.getHeight() + 4 + 13; // title + gap + subtitle approx height
                int startY = (getHeight() - totalH) / 2;
                g2.drawString(title,
                    (getWidth() - fmT.stringWidth(title)) / 2,
                    startY + fmT.getAscent());

                // Subtitle text — semi-transparent white always readable on any button colour
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(new Color(255, 255, 255, 190));
                FontMetrics fmS = g2.getFontMetrics();
                g2.drawString(subtitle,
                    (getWidth() - fmS.stringWidth(subtitle)) / 2,
                    startY + fmT.getHeight() + 4 + fmS.getAscent());
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(240, 72));
        btn.setMinimumSize(new Dimension(180, 64));
        return btn;
    }

}
