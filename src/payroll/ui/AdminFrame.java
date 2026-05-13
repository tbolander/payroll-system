package payroll.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import payroll.Employee;
import payroll.data.PayrollDatabase;
import payroll.model.EmployeeStatus;
import payroll.model.Gender;
import payroll.model.MedicalCoverage;
import payroll.model.PayType;
import payroll.model.PayrollResult;
import payroll.model.TimeEntry;
import payroll.security.UserAccount;
import payroll.service.BinarySearchUtil;
import payroll.service.EmployeeValidator;
import payroll.service.PayrollService;
import payroll.service.TimeEntryService;

public class AdminFrame extends JFrame {
    private static final String VERSION = "1.0.0";

    private final PayrollDatabase database;
    private final UserAccount account;
    private final EmployeeValidator validator = new EmployeeValidator();
    private final BinarySearchUtil searchUtil = new BinarySearchUtil();
    private final TimeEntryService timeEntryService;
    private final PayrollService payrollService;

    private final EmployeeTableModel employeeTableModel = new EmployeeTableModel();
    private final JTable employeeTable = new JTable(employeeTableModel);
    private final JComboBox<Employee> timeEmployeeBox = new JComboBox<>();

    private final JTextField employeeIdField = new JTextField();
    private final JTextField departmentField = new JTextField();
    private final JTextField jobTitleField = new JTextField();
    private final JTextField firstNameField = new JTextField();
    private final JTextField lastNameField = new JTextField();
    private final JTextField surnameField = new JTextField();
    private final JComboBox<EmployeeStatus> statusBox = new JComboBox<>(EmployeeStatus.values());
    private final JTextField dateOfBirthField = new JTextField("1990-01-01");
    private final JComboBox<Gender> genderBox = new JComboBox<>(Gender.values());
    private final JComboBox<PayType> payTypeBox = new JComboBox<>(PayType.values());
    private final JTextField emailField = new JTextField();
    private final JTextField address1Field = new JTextField();
    private final JTextField address2Field = new JTextField();
    private final JTextField cityField = new JTextField();
    private final JTextField stateField = new JTextField("IN");
    private final JTextField zipField = new JTextField();
    private final JTextField picturePathField = new JTextField();
    private final JTextField dateHireField = new JTextField(LocalDate.now().toString());
    private final JTextField baseSalaryField = new JTextField("0.00");
    private final JTextField hourlyRateField = new JTextField("0.00");
    private final JComboBox<MedicalCoverage> medicalBox = new JComboBox<>(MedicalCoverage.values());
    private final JTextField dependentsField = new JTextField("0");

    private final JTextField timeWeekStartField = new JTextField("2026-05-04");
    private final JTextField[] hourFields = new JTextField[7];
    private final JTextField ptoField = new JTextField("0");

    private final JTextField payrollStartField = new JTextField("2026-05-04");
    private final JTextField payrollEndField = new JTextField("2026-05-10");
    private final PayrollResultTableModel payrollResultTableModel = new PayrollResultTableModel();
    private final JTextArea payrollReportArea = new JTextArea();
    private final JTextArea reportsArea = new JTextArea();

    public AdminFrame(PayrollDatabase database, UserAccount account) {
        super("ABC Payroll System - Admin");
        this.database = database;
        this.account = account;
        this.timeEntryService = new TimeEntryService(database);
        this.payrollService = new PayrollService(database);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setLocationRelativeTo(null);
        buildScreen();
        refreshEmployees();
    }

    private void buildScreen() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Employees", buildEmployeePanel());
        tabs.addTab("Time Entry", buildTimeEntryPanel());
        tabs.addTab("Calculate Payroll", buildPayrollPanel());
        tabs.addTab("Reports", buildReportsPanel());
        tabs.addTab("Application Info", buildInfoPanel());
        add(tabs);
    }

    private JPanel buildEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && employeeTable.getSelectedRow() >= 0) {
                showEmployee(employeeTableModel.getEmployeeAt(employeeTable.getSelectedRow()));
            }
        });

        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Search ID");
        JLabel searchResultLabel = new JLabel("Binary search ready.");
        searchPanel.add(new JLabel("Employee ID"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        searchPanel.add(searchResultLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> {
            List<Employee> employees = database.findAllEmployees();
            Optional<Employee> iterative = searchUtil.iterativeSearchById(employees, searchField.getText().trim());
            Optional<Employee> recursive = searchUtil.recursiveSearchById(employees, searchField.getText().trim());

            if (iterative.isPresent() && recursive.isPresent()) {
                int row = employeeTableModel.findRowByEmployeeId(iterative.get().getEmployeeId());
                employeeTable.getSelectionModel().setSelectionInterval(row, row);
                searchResultLabel.setText("Found with iterative and recursive binary search: " + iterative.get().getFullName());
            } else {
                searchResultLabel.setText("Employee was not found.");
            }
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(employeeTable), BorderLayout.CENTER);
        panel.add(buildEmployeeForm(), BorderLayout.EAST);
        return panel;
    }

    private JPanel buildEmployeeForm() {
        JPanel wrapper = new JPanel(new BorderLayout(6, 6));
        JPanel form = new JPanel(new GridLayout(0, 2, 4, 4));

        addField(form, "Employee ID", employeeIdField);
        addField(form, "Department", departmentField);
        addField(form, "Job Title", jobTitleField);
        addField(form, "First Name", firstNameField);
        addField(form, "Last Name", lastNameField);
        addField(form, "Sur Name", surnameField);
        addField(form, "Status", statusBox);
        addField(form, "Date of Birth", dateOfBirthField);
        addField(form, "Gender", genderBox);
        addField(form, "Pay Type", payTypeBox);
        addField(form, "Company Email", emailField);
        addField(form, "Address Line 1", address1Field);
        addField(form, "Address Line 2", address2Field);
        addField(form, "City", cityField);
        addField(form, "State", stateField);
        addField(form, "Zip", zipField);
        addField(form, "Picture Path", picturePathField);
        addField(form, "Date Hire", dateHireField);
        addField(form, "Base Salary", baseSalaryField);
        addField(form, "Hourly Rate", hourlyRateField);
        addField(form, "Medical", medicalBox);
        addField(form, "Dependents", dependentsField);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 4));
        JButton newButton = new JButton("New");
        JButton saveButton = new JButton("Save");
        JButton deleteButton = new JButton("Delete");
        buttons.add(newButton);
        buttons.add(saveButton);
        buttons.add(deleteButton);

        newButton.addActionListener(event -> clearEmployeeForm());
        saveButton.addActionListener(event -> saveEmployee());
        deleteButton.addActionListener(event -> deleteEmployee());

        wrapper.add(new JLabel("Employee Demographics / Salary"), BorderLayout.NORTH);
        wrapper.add(form, BorderLayout.CENTER);
        wrapper.add(buttons, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildTimeEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(0, 2, 4, 4));
        String[] dayNames = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };

        addField(form, "Employee", timeEmployeeBox);
        addField(form, "Week Start", timeWeekStartField);
        for (int index = 0; index < hourFields.length; index++) {
            hourFields[index] = new JTextField("0");
            addField(form, dayNames[index] + " Hours", hourFields[index]);
        }
        addField(form, "PTO Hours", ptoField);

        JPanel buttons = new JPanel();
        JButton loadButton = new JButton("Load Week");
        JButton saveButton = new JButton("Save Adjustment");
        buttons.add(loadButton);
        buttons.add(saveButton);

        loadButton.addActionListener(event -> loadTimeEntry());
        saveButton.addActionListener(event -> saveTimeEntry());

        JTextArea note = new JTextArea("""
                Salary employees are automatically paid 8 hours Monday through Friday.
                Hourly employees can enter daily hours. Saturday is paid at time and a half.
                Admin adjustments are allowed from this screen.
                """);
        note.setEditable(false);

        panel.add(form, BorderLayout.NORTH);
        panel.add(note, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildPayrollPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new GridLayout(2, 4, 6, 6));
        JButton previewButton = new JButton("Preview Payroll");
        JButton calculateButton = new JButton("Calculate and Lock");

        controls.add(new JLabel("Period Start"));
        controls.add(payrollStartField);
        controls.add(new JLabel("Period End"));
        controls.add(payrollEndField);
        controls.add(previewButton);
        controls.add(calculateButton);

        JTable payrollTable = new JTable(payrollResultTableModel);
        payrollReportArea.setEditable(false);

        previewButton.addActionListener(event -> previewPayroll());
        calculateButton.addActionListener(event -> calculatePayroll());

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(payrollTable), BorderLayout.CENTER);
        panel.add(new JScrollPane(payrollReportArea), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JButton refreshButton = new JButton("Refresh Reports");
        reportsArea.setEditable(false);
        refreshButton.addActionListener(event -> refreshReports());
        panel.add(refreshButton, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportsArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setText("""
                ABC Payroll System
                Version: %s

                Logged in as: %s

                Security:
                - Admin login is HR0001 with a hashed password.
                - Employee login IDs use company email addresses.
                - Employee initial passwords use email name + date of birth.
                - Password hashes are stored in SQLite using MD5 for the course hashing requirement.

                Payroll:
                - Medical deductions are pretax.
                - Dependents add a $45 weekly stipend each.
                - Time entries lock after payroll calculation.
                """.formatted(VERSION, account.getDisplayName()));
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        return panel;
    }

    private void addField(JPanel form, String label, java.awt.Component field) {
        form.add(new JLabel(label));
        form.add(field);
    }

    private void refreshEmployees() {
        List<Employee> employees = database.findAllEmployees();
        employeeTableModel.setEmployees(employees);
        timeEmployeeBox.removeAllItems();
        for (Employee employee : employees) {
            timeEmployeeBox.addItem(employee);
        }
    }

    private void showEmployee(Employee employee) {
        employeeIdField.setText(employee.getEmployeeId());
        departmentField.setText(employee.getDepartment());
        jobTitleField.setText(employee.getJobTitle());
        firstNameField.setText(employee.getFirstName());
        lastNameField.setText(employee.getLastName());
        surnameField.setText(employee.getSurname());
        statusBox.setSelectedItem(employee.getStatus());
        dateOfBirthField.setText(employee.getDateOfBirth().toString());
        genderBox.setSelectedItem(employee.getGender());
        payTypeBox.setSelectedItem(employee.getPayType());
        emailField.setText(employee.getCompanyEmail());
        address1Field.setText(employee.getAddressLine1());
        address2Field.setText(employee.getAddressLine2());
        cityField.setText(employee.getCity());
        stateField.setText(employee.getState());
        zipField.setText(employee.getZip());
        picturePathField.setText(employee.getPicturePath());
        dateHireField.setText(employee.getDateHire().toString());
        baseSalaryField.setText(String.valueOf(employee.getBaseSalary()));
        hourlyRateField.setText(String.valueOf(employee.getHourlyRate()));
        medicalBox.setSelectedItem(employee.getMedicalCoverage());
        dependentsField.setText(String.valueOf(employee.getDependents()));
    }

    private void clearEmployeeForm() {
        employeeIdField.setText("");
        departmentField.setText("");
        jobTitleField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        surnameField.setText("");
        statusBox.setSelectedItem(EmployeeStatus.ACTIVE);
        dateOfBirthField.setText("1990-01-01");
        genderBox.setSelectedItem(Gender.FEMALE);
        payTypeBox.setSelectedItem(PayType.HOURLY);
        emailField.setText("");
        address1Field.setText("");
        address2Field.setText("");
        cityField.setText("");
        stateField.setText("IN");
        zipField.setText("");
        picturePathField.setText("");
        dateHireField.setText(LocalDate.now().toString());
        baseSalaryField.setText("0.00");
        hourlyRateField.setText("0.00");
        medicalBox.setSelectedItem(MedicalCoverage.SINGLE);
        dependentsField.setText("0");
    }

    private void saveEmployee() {
        try {
            Employee employee = buildEmployeeFromForm();
            List<String> errors = validator.validate(employee);
            if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(this, String.join("\n", errors), "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            database.saveEmployee(employee);
            refreshEmployees();
            JOptionPane.showMessageDialog(this, "Employee saved.");
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Save Employee", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Employee buildEmployeeFromForm() {
        return new Employee(
                employeeIdField.getText().trim(),
                departmentField.getText().trim(),
                jobTitleField.getText().trim(),
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                surnameField.getText().trim(),
                (EmployeeStatus) statusBox.getSelectedItem(),
                emailField.getText().trim(),
                LocalDate.parse(dateOfBirthField.getText().trim()),
                (Gender) genderBox.getSelectedItem(),
                (PayType) payTypeBox.getSelectedItem(),
                address1Field.getText().trim(),
                address2Field.getText().trim(),
                cityField.getText().trim(),
                stateField.getText().trim(),
                zipField.getText().trim(),
                picturePathField.getText().trim(),
                LocalDate.parse(dateHireField.getText().trim()),
                Double.parseDouble(baseSalaryField.getText().trim()),
                Double.parseDouble(hourlyRateField.getText().trim()),
                (MedicalCoverage) medicalBox.getSelectedItem(),
                Integer.parseInt(dependentsField.getText().trim()));
    }

    private void deleteEmployee() {
        String employeeId = employeeIdField.getText().trim();
        if (employeeId.isBlank()) {
            return;
        }

        int answer = JOptionPane.showConfirmDialog(this, "Delete employee " + employeeId + "?");
        if (answer == JOptionPane.YES_OPTION) {
            database.deleteEmployee(employeeId);
            clearEmployeeForm();
            refreshEmployees();
        }
    }

    private void loadTimeEntry() {
        Employee employee = (Employee) timeEmployeeBox.getSelectedItem();
        if (employee == null) {
            return;
        }

        LocalDate weekStart = timeEntryService.normalizeToMonday(LocalDate.parse(timeWeekStartField.getText().trim()));
        timeWeekStartField.setText(weekStart.toString());

        double[] hours = timeEntryService.loadHours(employee, weekStart);
        for (int index = 0; index < hourFields.length; index++) {
            hourFields[index].setText(String.valueOf(hours[index]));
        }

        double pto = 0.0;
        for (TimeEntry entry : database.findTimeEntries(employee.getEmployeeId(), weekStart, weekStart.plusDays(6))) {
            pto += entry.getPtoHours();
        }
        ptoField.setText(String.valueOf(pto));
    }

    private void saveTimeEntry() {
        try {
            Employee employee = (Employee) timeEmployeeBox.getSelectedItem();
            if (employee == null) {
                return;
            }

            LocalDate weekStart = timeEntryService.normalizeToMonday(LocalDate.parse(timeWeekStartField.getText().trim()));
            double[] hours = new double[7];
            for (int index = 0; index < hours.length; index++) {
                hours[index] = Double.parseDouble(hourFields[index].getText().trim());
            }
            double pto = Double.parseDouble(ptoField.getText().trim());

            timeEntryService.saveWeek(employee, weekStart, hours, pto, true);
            JOptionPane.showMessageDialog(this, "Time entry saved.");
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Time Entry", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewPayroll() {
        LocalDate start = LocalDate.parse(payrollStartField.getText().trim());
        LocalDate end = LocalDate.parse(payrollEndField.getText().trim());
        List<PayrollResult> results = payrollService.previewPayroll(start, end);
        payrollResultTableModel.setResults(results);
        payrollReportArea.setText(payrollService.formatReport(results, start, end));
    }

    private void calculatePayroll() {
        LocalDate start = LocalDate.parse(payrollStartField.getText().trim());
        LocalDate end = LocalDate.parse(payrollEndField.getText().trim());
        List<PayrollResult> results = payrollService.calculateAndLockPayroll(start, end);
        payrollResultTableModel.setResults(results);
        payrollReportArea.setText(payrollService.formatReport(results, start, end));
        refreshReports();
        JOptionPane.showMessageDialog(this, "Payroll calculated, time entries locked, and report files created.");
    }

    private void refreshReports() {
        Path reportsDirectory = Path.of("reports");
        StringBuilder text = new StringBuilder();

        try {
            if (!Files.exists(reportsDirectory)) {
                reportsArea.setText("No reports have been created yet.");
                return;
            }

            Files.list(reportsDirectory).sorted().forEach(path -> {
                text.append(path.getFileName()).append("\n");
                if (path.toString().endsWith(".txt")) {
                    try {
                        text.append(Files.readString(path)).append("\n");
                    } catch (IOException exception) {
                        text.append("Could not read report.\n");
                    }
                }
                text.append("\n");
            });
            reportsArea.setText(text.toString());
        } catch (IOException exception) {
            reportsArea.setText("Unable to read reports.");
        }
    }
}
