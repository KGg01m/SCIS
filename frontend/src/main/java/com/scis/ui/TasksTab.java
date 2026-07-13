package com.scis.ui;

import com.scis.db.DataManager;
import com.scis.model.Student;
import com.scis.model.StudentTask;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TasksTab — builds the Tasks management panel with a task list and
 * a side reminder panel.
 */
public final class TasksTab {

    private TasksTab() {}

    public static JPanel build(Student student, Runnable onDataChange) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Form card ─────────────────────────────────────────────────────────
        JPanel formCard = UIFactory.buildCard("Add New Task");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.card());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField titleField = UIFactory.styledTextField("Task title...");
        JComboBox<String> typeCombo     = new JComboBox<>(StudentTask.TASK_TYPES);
        JComboBox<String> subjCombo     = SubjectComboHelper.create(panel, student, () -> onDataChange.run());
        JTextField dueDateField = UIFactory.styledTextField(LocalDate.now().plusDays(7).toString());
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        JTextField descField  = UIFactory.styledTextField("Optional description...");
        priorityCombo.setSelectedIndex(1);
        UIFactory.styleCombo(typeCombo);
        UIFactory.styleCombo(priorityCombo);

        gbc.gridx = 0; gbc.gridy = 0; form.add(UIFactory.formLbl("Title:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; form.add(titleField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        form.add(UIFactory.formLbl("Type:"),    gbc); gbc.gridx = 1; form.add(typeCombo, gbc);
        gbc.gridx = 2; form.add(UIFactory.formLbl("Subject:"), gbc);
        gbc.gridx = 3; form.add(subjCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(UIFactory.formLbl("Due Date:"), gbc); gbc.gridx = 1; form.add(dueDateField, gbc);
        gbc.gridx = 2; form.add(UIFactory.formLbl("Priority:"), gbc);
        gbc.gridx = 3; form.add(priorityCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(UIFactory.formLbl("Description:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; form.add(descField, gbc);

        JButton addBtn    = UIFactory.actionButton("Add Task", Theme.PURPLE);
        JButton submitBtn = UIFactory.actionButton("Submit",   Theme.GREEN);
        JButton editBtn   = UIFactory.actionButton("Edit",     Theme.ORANGE);
        JButton delBtn    = UIFactory.actionButton("Delete",   Theme.RED);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Theme.card());
        btnRow.add(addBtn); btnRow.add(submitBtn);
        btnRow.add(editBtn); btnRow.add(delBtn);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; form.add(btnRow, gbc);
        formCard.add(form, BorderLayout.CENTER);

        // ── Task table ────────────────────────────────────────────────────────
        String[] tcols = {"Title", "Type", "Subject", "Due Date",
                           "Priority", "Status", "Description"};
        DefaultTableModel taskModel = new DefaultTableModel(tcols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable taskTable = UIFactory.styledTable(taskModel);
        taskTable.setDefaultRenderer(Object.class, taskStatusRenderer());
        refreshTable(student, taskModel);

        // ── Reminder panel ────────────────────────────────────────────────────
        JPanel reminderPanel = buildReminderPanel(student);
        JScrollPane reminderScroll = new JScrollPane(reminderPanel);
        reminderScroll.setBorder(null);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            UIFactory.buildTableCard(taskTable, "Tasks"), reminderScroll);
        split.setResizeWeight(0.72);
        split.setDividerSize(6);

        // ── Button actions ────────────────────────────────────────────────────
        addBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) { UIFactory.toast(panel, "Enter a task title!"); return; }
            try {
                LocalDate dd = LocalDate.parse(dueDateField.getText().trim());
                String subj  = (String) subjCombo.getSelectedItem();
                if (!SubjectComboHelper.isValidSelection(subj)) subj = "General";
                int pri = priorityCombo.getSelectedIndex() + 1;
                student.addTask(new StudentTask(title,
                    (String) typeCombo.getSelectedItem(), subj,
                    dd, descField.getText().trim(), pri));
                DataManager.saveStudent(student);
                titleField.setText(""); descField.setText("");
                refreshTable(student, taskModel);
                refreshReminderPanel(student, reminderPanel);
                onDataChange.run();
                UIFactory.toast(panel, "Task added!");
            } catch (Exception ex) {
                UIFactory.toast(panel, "Invalid date! Use YYYY-MM-DD");
            }
        });

        submitBtn.addActionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select a task!"); return; }
            String tTitle = (String) taskModel.getValueAt(row, 0);
            String tType  = (String) taskModel.getValueAt(row, 1);
            for (StudentTask t : student.getTasks())
                if (t.title.equals(tTitle) && t.taskType.equals(tType) && !t.submitted) {
                    t.submitted = true; t.submittedDate = LocalDate.now(); break;
                }
            DataManager.saveStudent(student);
            refreshTable(student, taskModel);
            refreshReminderPanel(student, reminderPanel);
            onDataChange.run();
            UIFactory.toast(panel, "Marked as submitted!");
        });

        delBtn.addActionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select a task!"); return; }
            String tTitle = (String) taskModel.getValueAt(row, 0);
            String tType  = (String) taskModel.getValueAt(row, 1);
            if (JOptionPane.showConfirmDialog(panel,
                    "Delete task: " + tTitle + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                student.getTasks().removeIf(
                    t -> t.title.equals(tTitle) && t.taskType.equals(tType));
                DataManager.saveStudent(student);
                refreshTable(student, taskModel);
                refreshReminderPanel(student, reminderPanel);
                onDataChange.run();
            }
        });

        editBtn.addActionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row == -1) { UIFactory.toast(panel, "Select a task!"); return; }
            String tTitle = (String) taskModel.getValueAt(row, 0);
            String tType  = (String) taskModel.getValueAt(row, 1);
            StudentTask found = null;
            for (StudentTask t : student.getTasks())
                if (t.title.equals(tTitle) && t.taskType.equals(tType)) {
                    found = t; break;
                }
            if (found != null)
                showEditDialog(panel, student, found, taskModel, reminderPanel, onDataChange);
        });

        panel.add(formCard, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // ── Reminder panel ────────────────────────────────────────────────────────

    private static JPanel buildReminderPanel(Student student) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.card());
        panel.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(Theme.RED, 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel title = new JLabel("Upcoming Reminders");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(Theme.text());
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        fillReminderPanel(student, panel);
        return panel;
    }

    private static void refreshReminderPanel(Student student, JPanel panel) {
        panel.removeAll();
        fillReminderPanel(student, panel);
        panel.revalidate();
        panel.repaint();
    }

    private static void fillReminderPanel(Student student, JPanel panel) {
        List<StudentTask> overdue   = student.getOverdueTasks();
        List<StudentTask> upcoming  = student.getUpcomingTasks();

        if (!overdue.isEmpty()) {
            JLabel ol = new JLabel("OVERDUE (" + overdue.size() + ")");
            ol.setFont(new Font("Segoe UI", Font.BOLD, 12));
            ol.setForeground(Theme.RED);
            ol.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(ol);
            panel.add(Box.createRigidArea(new Dimension(0, 4)));
            for (StudentTask t : overdue) {
                Color bg = Theme.darkMode
                    ? new Color(69, 10, 10) : new Color(255, 241, 242);
                panel.add(taskCard(t, bg));
                panel.add(Box.createRigidArea(new Dimension(0, 4)));
            }
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        JLabel ul = new JLabel("UPCOMING (" + upcoming.size() + ")");
        ul.setFont(new Font("Segoe UI", Font.BOLD, 12));
        ul.setForeground(Theme.TEAL);
        ul.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(ul);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        if (upcoming.isEmpty()) {
            JLabel none = new JLabel("No upcoming tasks!");
            none.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            none.setForeground(Theme.muted());
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(none);
        } else {
            for (StudentTask t : upcoming) {
                Color bg = t.getDaysUntilDue() <= 2
                    ? (Theme.darkMode ? new Color(69, 43, 5) : new Color(255, 251, 235))
                    : (Theme.darkMode ? new Color(5, 30, 60)  : new Color(240, 249, 255));
                panel.add(taskCard(t, bg));
                panel.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }
        panel.add(Box.createVerticalGlue());
    }

    private static JPanel taskCard(StudentTask t, Color bg) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(bg);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(Theme.border()),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        card.setMaximumSize(new Dimension(330, 65));
        JLabel tl = new JLabel("[" + t.taskType + "] " + t.title);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tl.setForeground(Theme.text());
        long d = t.getDaysUntilDue();
        String dayStr = t.submitted ? "✓ Submitted"
            : d < 0 ? "Overdue by " + Math.abs(d) + "d"
            : d + "d left";
        JLabel sl = new JLabel(t.subject + " | Due: "
            + (t.dueDate != null ? t.dueDate.toString() : "N/A")
            + " | " + dayStr);
        sl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sl.setForeground(Theme.muted());
        card.add(tl); card.add(sl);
        return card;
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    private static void refreshTable(Student student, DefaultTableModel model) {
        model.setRowCount(0);
        List<StudentTask> tasks = new ArrayList<>(student.getTasks());
        tasks.sort(Comparator.comparingInt((StudentTask t) -> t.priority)
            .thenComparing(t -> t.dueDate == null ? LocalDate.MAX : t.dueDate));
        for (StudentTask t : tasks)
            model.addRow(new Object[]{
                t.title, t.taskType, t.subject,
                t.dueDate != null ? t.dueDate.toString() : "N/A",
                t.getPriorityLabel(), t.getStatusLabel(),
                t.description != null ? t.description : ""
            });
    }

    private static DefaultTableCellRenderer taskStatusRenderer() {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(
                    t, v, sel, foc, r, c);
                if (!sel) {
                    String st = (String) t.getModel().getValueAt(r, 5);
                    if ("Submitted".equals(st))
                        comp.setBackground(Theme.darkMode
                            ? new Color(6, 32, 18) : new Color(240, 255, 245));
                    else if ("Overdue".equals(st))
                        comp.setBackground(Theme.darkMode
                            ? new Color(69, 10, 10) : new Color(255, 241, 242));
                    else if ("Due Soon".equals(st))
                        comp.setBackground(Theme.darkMode
                            ? new Color(69, 43, 5) : new Color(255, 251, 235));
                    else
                        comp.setBackground(Theme.card());
                    comp.setForeground(Theme.text());
                }
                return comp;
            }
        };
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────

    private static void showEditDialog(Component parent, Student student,
                                        StudentTask ft, DefaultTableModel model,
                                        JPanel reminderPanel,
                                        Runnable onDataChange) {
        JDialog dlg = new JDialog(
            SwingUtilities.getWindowAncestor(parent),
            "Edit Task",
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(460, 300);
        dlg.setLocationRelativeTo(parent);

        JPanel p = new JPanel(new GridLayout(5, 2, 10, 8));
        p.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        JTextField tf  = new JTextField(ft.title);
        JComboBox<String> tyc = new JComboBox<>(StudentTask.TASK_TYPES);
        tyc.setSelectedItem(ft.taskType);
        JTextField ddf = new JTextField(
            ft.dueDate != null ? ft.dueDate.toString() : "");
        JTextField df  = new JTextField(
            ft.description != null ? ft.description : "");
        JComboBox<String> pc =
            new JComboBox<>(new String[]{"High", "Medium", "Low"});
        pc.setSelectedIndex(ft.priority - 1);
        p.add(new JLabel("Title:"));       p.add(tf);
        p.add(new JLabel("Type:"));        p.add(tyc);
        p.add(new JLabel("Due Date:"));    p.add(ddf);
        p.add(new JLabel("Priority:"));    p.add(pc);
        p.add(new JLabel("Description:")); p.add(df);

        JButton sv = UIFactory.actionButton("Save",   Theme.GREEN);
        JButton cn = UIFactory.actionButton("Cancel", new Color(100, 100, 100));
        JPanel bp = new JPanel(new FlowLayout()); bp.add(sv); bp.add(cn);
        JPanel mp = new JPanel(new BorderLayout());
        mp.add(p, BorderLayout.CENTER); mp.add(bp, BorderLayout.SOUTH);

        sv.addActionListener(e2 -> {
            try {
                ft.title       = tf.getText().trim();
                ft.taskType    = (String) tyc.getSelectedItem();
                ft.dueDate     = LocalDate.parse(ddf.getText().trim());
                ft.priority    = pc.getSelectedIndex() + 1;
                ft.description = df.getText().trim();
                DataManager.saveStudent(student);
                refreshTable(student, model);
                refreshReminderPanel(student, reminderPanel);
                dlg.dispose();
                onDataChange.run();
            } catch (Exception ex) {
                UIFactory.toast(dlg, "Invalid date! Use YYYY-MM-DD");
            }
        });
        cn.addActionListener(e2 -> dlg.dispose());
        dlg.add(mp);
        dlg.setVisible(true);
    }
}
