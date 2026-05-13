package payroll.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import payroll.Employee;
import payroll.data.PayrollDatabase;
import payroll.model.PayType;
import payroll.model.PayrollResult;
import payroll.security.UserAccount;
import payroll.service.PayrollService;
import payroll.service.TimeEntryService;

public class EmployeeFrame extends JFrame {
    private final PayrollDatabase database;
    private final UserAccount account;
    private final Employee employee;
    private final TimeEntryService timeEntryService;
    private final PayrollService payrollService;

    private final JTextField weekStartField = new JTextField("2026-05-04");
    private final JTextField paycheckWeekStartField = new JTextField("2026-05-04");
    private final JTextField[] hourFields = new JTextField[7];
    private final JTextField ptoField = new JTextField("0");
    private final JTextArea paycheckArea = new JTextArea();

    public EmployeeFrame(PayrollDatabase database, UserAccount account, Employee employee) {
        super("ABC Payroll System - Employee");
        this.database = database;
        this.account = account;
        this.employee = employee;
        this.timeEntryService = new TimeEntryService(database);
        this.payrollService = new PayrollService(database);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);
        buildScreen();
        loadTimeEntry();
    }

    private void buildScreen() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Hours / PTO", buildTimePanel());
        tabs.addTab("Paycheck", buildPaycheckPanel());
        tabs.addTab("Account Info", buildInfoPanel());
        add(tabs);
    }

    private JPanel buildTimePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(0, 2, 4, 4));
        String[] dayNames = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };

        form.add(new JLabel("Employee"));
        form.add(new JLabel(employee.toString()));
        form.add(new JLabel("Week Start"));
        form.add(weekStartField);

        for (int index = 0; index < hourFields.length; index++) {
            hourFields[index] = new JTextField("0");
            form.add(new JLabel(dayNames[index] + " Hours"));
            form.add(hourFields[index]);
        }

        form.add(new JLabel("PTO Hours"));
        form.add(ptoField);

        JButton loadButton = new JButton("Load Week");
        JButton saveButton = new JButton("Save Time Entry");
        JPanel buttons = new JPanel();
        buttons.add(loadButton);
        buttons.add(saveButton);

        loadButton.addActionListener(event -> loadTimeEntry());
        saveButton.addActionListener(event -> saveTimeEntry());

        if (employee.getPayType() == PayType.SALARY) {
            for (JTextField hourField : hourFields) {
                hourField.setEnabled(false);
            }
        }

        panel.add(form, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildPaycheckPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel controls = new JPanel();
        JButton previewButton = new JButton("Preview Paycheck");
        controls.add(new JLabel("Week Start"));
        controls.add(paycheckWeekStartField);
        controls.add(previewButton);
        paycheckArea.setEditable(false);

        previewButton.addActionListener(event -> previewPaycheck());

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(paycheckArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setText("""
                Logged in as: %s
                Employee: %s
                Department: %s
                Job: %s
                Pay Type: %s
                Medical: %s
                Dependents: %d

                Employee users can save hours, add PTO, and preview paycheck calculations.
                Time entries cannot be changed after HR calculates payroll for that week.
                """.formatted(
                account.getDisplayName(),
                employee.getFullName(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.getPayType(),
                employee.getMedicalCoverage(),
                employee.getDependents()));
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        return panel;
    }

    private void loadTimeEntry() {
        try {
            LocalDate weekStart = timeEntryService.normalizeToMonday(LocalDate.parse(weekStartField.getText().trim()));
            weekStartField.setText(weekStart.toString());
            double[] hours = timeEntryService.loadHours(employee, weekStart);

            for (int index = 0; index < hourFields.length; index++) {
                if (hourFields[index] != null) {
                    hourFields[index].setText(String.valueOf(hours[index]));
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Load Time", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveTimeEntry() {
        try {
            LocalDate weekStart = timeEntryService.normalizeToMonday(LocalDate.parse(weekStartField.getText().trim()));
            double[] hours = new double[7];

            for (int index = 0; index < hours.length; index++) {
                hours[index] = employee.getPayType() == PayType.SALARY ? 0.0 : Double.parseDouble(hourFields[index].getText().trim());
            }

            double pto = Double.parseDouble(ptoField.getText().trim());
            timeEntryService.saveWeek(employee, weekStart, hours, pto, false);
            JOptionPane.showMessageDialog(this, "Time entry saved.");
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Save Time", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewPaycheck() {
        try {
            LocalDate weekStart = timeEntryService.normalizeToMonday(LocalDate.parse(paycheckWeekStartField.getText().trim()));
            paycheckWeekStartField.setText(weekStart.toString());
            PayrollResult result = payrollService.previewEmployeePaycheck(employee, weekStart, weekStart.plusDays(6));
            paycheckArea.setText("""
                    Paycheck Preview
                    Employee: %s
                    Gross Pay: %s
                    Medical Deduction: %s
                    Dependent Stipend: %s
                    Taxable Pay: %s

                    State Tax: %s
                    Federal Tax Employee: %s
                    Social Security Employee: %s
                    Medicare Employee: %s

                    Federal Tax Employer: %s
                    Social Security Employer: %s
                    Medicare Employer: %s

                    Net Pay: %s
                    """.formatted(
                    result.getEmployeeName(),
                    Money.format(result.getGrossPay()),
                    Money.format(result.getMedicalDeduction()),
                    Money.format(result.getDependentStipend()),
                    Money.format(result.getTaxablePay()),
                    Money.format(result.getStateTaxEmployee()),
                    Money.format(result.getFederalTaxEmployee()),
                    Money.format(result.getSocialSecurityEmployee()),
                    Money.format(result.getMedicareEmployee()),
                    Money.format(result.getFederalTaxEmployer()),
                    Money.format(result.getSocialSecurityEmployer()),
                    Money.format(result.getMedicareEmployer()),
                    Money.format(result.getNetPay())));
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Paycheck", JOptionPane.ERROR_MESSAGE);
        }
    }
}
