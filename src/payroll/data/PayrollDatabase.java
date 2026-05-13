package payroll.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import payroll.Employee;
import payroll.model.EmployeeStatus;
import payroll.model.Gender;
import payroll.model.MedicalCoverage;
import payroll.model.PayType;
import payroll.model.PayrollResult;
import payroll.model.TimeEntry;
import payroll.security.CredentialGenerator;
import payroll.security.PasswordHasher;
import payroll.security.UserAccount;
import payroll.security.UserType;

public class PayrollDatabase {
    private final Path databasePath;

    public PayrollDatabase(Path databasePath) {
        this.databasePath = databasePath;
    }

    public void initialize() {
        try {
            if (databasePath.getParent() != null) {
                Files.createDirectories(databasePath.getParent());
            }

            Class.forName("org.sqlite.JDBC");

            try (Connection connection = getConnection()) {
                createTables(connection);
                migrateSecurityColumns(connection);
                seedDataIfNeeded(connection);
                migrateUnsaltedPasswordHashes(connection);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Database could not be initialized.", exception);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS employees (
                        employee_id TEXT PRIMARY KEY,
                        department TEXT NOT NULL,
                        job_title TEXT NOT NULL,
                        first_name TEXT NOT NULL,
                        last_name TEXT NOT NULL,
                        surname TEXT NOT NULL,
                        status TEXT NOT NULL,
                        company_email TEXT NOT NULL UNIQUE,
                        date_of_birth TEXT NOT NULL,
                        gender TEXT NOT NULL,
                        pay_type TEXT NOT NULL,
                        address_line1 TEXT NOT NULL,
                        address_line2 TEXT,
                        city TEXT NOT NULL,
                        state TEXT NOT NULL,
                        zip TEXT NOT NULL,
                        picture_path TEXT,
                        date_hire TEXT NOT NULL,
                        base_salary REAL NOT NULL,
                        hourly_rate REAL NOT NULL,
                        medical_coverage TEXT NOT NULL,
                        dependents INTEGER NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id TEXT PRIMARY KEY,
                        user_type TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        password_salt TEXT NOT NULL DEFAULT '',
                        employee_id TEXT,
                        display_name TEXT NOT NULL,
                        FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS time_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        employee_id TEXT NOT NULL,
                        work_date TEXT NOT NULL,
                        hours_worked REAL NOT NULL,
                        pto_hours REAL NOT NULL,
                        locked INTEGER NOT NULL DEFAULT 0,
                        UNIQUE(employee_id, work_date),
                        FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS payroll_runs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        period_start TEXT NOT NULL,
                        period_end TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        status TEXT NOT NULL,
                        UNIQUE(period_start, period_end)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS paychecks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        payroll_run_id INTEGER NOT NULL,
                        employee_id TEXT NOT NULL,
                        gross_pay REAL NOT NULL,
                        medical_deduction REAL NOT NULL,
                        dependent_stipend REAL NOT NULL,
                        taxable_pay REAL NOT NULL,
                        state_tax_employee REAL NOT NULL,
                        federal_tax_employee REAL NOT NULL,
                        social_security_employee REAL NOT NULL,
                        medicare_employee REAL NOT NULL,
                        federal_tax_employer REAL NOT NULL,
                        social_security_employer REAL NOT NULL,
                        medicare_employer REAL NOT NULL,
                        net_pay REAL NOT NULL,
                        UNIQUE(payroll_run_id, employee_id),
                        FOREIGN KEY(payroll_run_id) REFERENCES payroll_runs(id) ON DELETE CASCADE,
                        FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    private void migrateSecurityColumns(Connection connection) throws SQLException {
        if (!columnExists(connection, "users", "password_salt")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE users ADD COLUMN password_salt TEXT NOT NULL DEFAULT ''");
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + tableName + ")");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void migrateUnsaltedPasswordHashes(Connection connection) throws SQLException {
        List<UserPasswordMigration> migrations = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT user_id, user_type, employee_id
                FROM users
                WHERE password_salt IS NULL OR password_salt = ''
                """);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String userId = resultSet.getString("user_id");
                String userType = resultSet.getString("user_type");
                String employeeId = resultSet.getString("employee_id");
                String password = initialPasswordForUser(connection, userId, userType, employeeId);

                if (password != null) {
                    migrations.add(new UserPasswordMigration(userId, password));
                }
            }
        }

        for (UserPasswordMigration migration : migrations) {
            String salt = PasswordHasher.generateSalt();
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE users
                    SET password_salt = ?, password_hash = ?
                    WHERE user_id = ?
                    """)) {
                update.setString(1, salt);
                update.setString(2, PasswordHasher.hashMd5WithSalt(migration.plainTextPassword, salt));
                update.setString(3, migration.userId);
                update.executeUpdate();
            }
        }
    }

    private String initialPasswordForUser(Connection connection, String userId, String userType, String employeeId) throws SQLException {
        if (UserType.ADMIN.name().equals(userType) && "HR0001".equalsIgnoreCase(userId)) {
            return "ABCpayroll2026!";
        }

        if (!UserType.EMPLOYEE.name().equals(userType) || employeeId == null) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT company_email, date_of_birth
                FROM employees
                WHERE employee_id = ?
                """)) {
            statement.setString(1, employeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                CredentialGenerator credentialGenerator = new CredentialGenerator();
                return credentialGenerator.generateInitialEmployeePassword(
                        resultSet.getString("company_email"),
                        LocalDate.parse(resultSet.getString("date_of_birth")));
            }
        }
    }

    private void seedDataIfNeeded(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM employees")) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
        }

        insertAdminUser(connection);

        List<Employee> employees = List.of(
                employee("E1001", "Human Resources", "HR Coordinator", "Maya", "Rodriguez", "Rodriguez", EmployeeStatus.ACTIVE, "maya.rodriguez@abccompany.com", "1988-03-14", Gender.FEMALE, PayType.SALARY, "410 Main St", "Apt 2", "Indianapolis", "IN", "46204", "", "2018-02-12", 62000.00, 0.00, MedicalCoverage.FAMILY, 2),
                employee("E1002", "Finance", "Payroll Clerk", "Noah", "Carter", "Carter", EmployeeStatus.ACTIVE, "noah.carter@abccompany.com", "1991-11-20", Gender.MALE, PayType.HOURLY, "811 Market Ave", "", "Indianapolis", "IN", "46205", "", "2020-06-01", 0.00, 28.50, MedicalCoverage.SINGLE, 0),
                employee("E1003", "Operations", "Warehouse Lead", "Ava", "Thompson", "Thompson", EmployeeStatus.ACTIVE, "ava.thompson@abccompany.com", "1985-09-07", Gender.FEMALE, PayType.HOURLY, "55 Lake Rd", "", "Fishers", "IN", "46037", "", "2017-08-21", 0.00, 31.25, MedicalCoverage.FAMILY, 3),
                employee("E1004", "Sales", "Account Manager", "Liam", "Brooks", "Brooks", EmployeeStatus.ACTIVE, "liam.brooks@abccompany.com", "1993-04-18", Gender.MALE, PayType.SALARY, "733 Oak St", "", "Carmel", "IN", "46032", "", "2019-09-03", 70500.00, 0.00, MedicalCoverage.SINGLE, 1),
                employee("E1005", "IT", "Support Specialist", "Emma", "Wilson", "Wilson", EmployeeStatus.ACTIVE, "emma.wilson@abccompany.com", "1995-01-25", Gender.FEMALE, PayType.HOURLY, "19 Pine Ct", "", "Greenwood", "IN", "46142", "", "2021-01-11", 0.00, 26.75, MedicalCoverage.SINGLE, 0),
                employee("E1006", "Operations", "Machine Operator", "Ethan", "Davis", "Davis", EmployeeStatus.ACTIVE, "ethan.davis@abccompany.com", "1990-06-05", Gender.MALE, PayType.HOURLY, "912 River Dr", "", "Noblesville", "IN", "46060", "", "2016-03-15", 0.00, 24.00, MedicalCoverage.FAMILY, 2),
                employee("E1007", "Customer Service", "Service Rep", "Olivia", "Miller", "Miller", EmployeeStatus.ACTIVE, "olivia.miller@abccompany.com", "1998-12-30", Gender.FEMALE, PayType.HOURLY, "305 Center St", "", "Indianapolis", "IN", "46220", "", "2022-05-17", 0.00, 22.25, MedicalCoverage.SINGLE, 1),
                employee("E1008", "Management", "Office Manager", "James", "Anderson", "Anderson", EmployeeStatus.ACTIVE, "james.anderson@abccompany.com", "1982-07-16", Gender.MALE, PayType.SALARY, "220 North Ave", "", "Zionsville", "IN", "46077", "", "2014-10-06", 83000.00, 0.00, MedicalCoverage.FAMILY, 4),
                employee("E1009", "Finance", "Staff Accountant", "Sophia", "Martinez", "Martinez", EmployeeStatus.ACTIVE, "sophia.martinez@abccompany.com", "1992-10-09", Gender.FEMALE, PayType.SALARY, "604 Elm St", "", "Indianapolis", "IN", "46202", "", "2020-02-24", 68000.00, 0.00, MedicalCoverage.SINGLE, 0),
                employee("E1010", "Operations", "Picker Packer", "Benjamin", "Taylor", "Taylor", EmployeeStatus.ACTIVE, "benjamin.taylor@abccompany.com", "1997-05-12", Gender.MALE, PayType.HOURLY, "1420 Maple Way", "", "Plainfield", "IN", "46168", "", "2023-07-10", 0.00, 21.50, MedicalCoverage.SINGLE, 0),
                employee("E1011", "HR", "Recruiter", "Isabella", "Moore", "Moore", EmployeeStatus.ACTIVE, "isabella.moore@abccompany.com", "1989-08-22", Gender.FEMALE, PayType.SALARY, "88 College Ave", "", "Indianapolis", "IN", "46208", "", "2018-11-19", 64500.00, 0.00, MedicalCoverage.FAMILY, 2),
                employee("E1012", "Sales", "Sales Assistant", "Lucas", "White", "White", EmployeeStatus.TERMINATED, "lucas.white@abccompany.com", "1994-02-02", Gender.MALE, PayType.HOURLY, "771 South St", "", "Avon", "IN", "46123", "", "2021-04-05", 0.00, 20.00, MedicalCoverage.SINGLE, 0));

        for (Employee employee : employees) {
            saveEmployee(connection, employee);
            insertEmployeeUser(connection, employee);
        }

        seedTimeEntries(connection);
    }

    private Employee employee(
            String employeeId,
            String department,
            String jobTitle,
            String firstName,
            String lastName,
            String surname,
            EmployeeStatus status,
            String companyEmail,
            String dateOfBirth,
            Gender gender,
            PayType payType,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String zip,
            String picturePath,
            String dateHire,
            double baseSalary,
            double hourlyRate,
            MedicalCoverage medicalCoverage,
            int dependents) {
        return new Employee(
                employeeId,
                department,
                jobTitle,
                firstName,
                lastName,
                surname,
                status,
                companyEmail,
                LocalDate.parse(dateOfBirth),
                gender,
                payType,
                addressLine1,
                addressLine2,
                city,
                state,
                zip,
                picturePath,
                LocalDate.parse(dateHire),
                baseSalary,
                hourlyRate,
                medicalCoverage,
                dependents);
    }

    private void insertAdminUser(Connection connection) throws SQLException {
        String salt = PasswordHasher.generateSalt();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO users(user_id, user_type, password_hash, password_salt, employee_id, display_name)
                VALUES (?, ?, ?, ?, NULL, ?)
                """)) {
            statement.setString(1, "HR0001");
            statement.setString(2, UserType.ADMIN.name());
            statement.setString(3, PasswordHasher.hashMd5WithSalt("ABCpayroll2026!", salt));
            statement.setString(4, salt);
            statement.setString(5, "ABC HR Admin");
            statement.executeUpdate();
        }
    }

    private void insertEmployeeUser(Connection connection, Employee employee) throws SQLException {
        CredentialGenerator credentialGenerator = new CredentialGenerator();
        String userId = credentialGenerator.generateEmployeeUserId(employee.getCompanyEmail());
        String password = credentialGenerator.generateInitialEmployeePassword(employee.getCompanyEmail(), employee.getDateOfBirth());
        String salt = PasswordHasher.generateSalt();

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO users(user_id, user_type, password_hash, password_salt, employee_id, display_name)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, userId);
            statement.setString(2, UserType.EMPLOYEE.name());
            statement.setString(3, PasswordHasher.hashMd5WithSalt(password, salt));
            statement.setString(4, salt);
            statement.setString(5, employee.getEmployeeId());
            statement.setString(6, employee.getFullName());
            statement.executeUpdate();
        }
    }

    private void seedTimeEntries(Connection connection) throws SQLException {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        saveHourlyWeek(connection, "E1002", weekStart, new double[] { 8, 8, 9, 8, 8, 4, 0 });
        saveHourlyWeek(connection, "E1003", weekStart, new double[] { 10, 8, 8, 8, 7, 5, 0 });
        saveHourlyWeek(connection, "E1005", weekStart, new double[] { 8, 8, 8, 8, 8, 0, 0 });
        saveHourlyWeek(connection, "E1006", weekStart, new double[] { 8, 9, 8, 8, 8, 6, 0 });
        saveHourlyWeek(connection, "E1007", weekStart, new double[] { 7, 7.5, 8, 8.5, 8, 0, 0 });
        saveHourlyWeek(connection, "E1010", weekStart, new double[] { 8, 8, 8, 8, 8, 3, 0 });
        saveHourlyWeek(connection, "E1012", weekStart, new double[] { 0, 0, 0, 0, 0, 0, 0 });
        saveSalaryPto(connection, "E1001", weekStart.plusDays(2), 4);
        saveSalaryPto(connection, "E1008", weekStart.plusDays(4), 8);
    }

    private void saveHourlyWeek(Connection connection, String employeeId, LocalDate weekStart, double[] hours) throws SQLException {
        for (int index = 0; index < hours.length; index++) {
            upsertTimeEntry(connection, employeeId, weekStart.plusDays(index), hours[index], 0, false, false);
        }
    }

    private void saveSalaryPto(Connection connection, String employeeId, LocalDate workDate, double ptoHours) throws SQLException {
        upsertTimeEntry(connection, employeeId, workDate, 0, ptoHours, false, false);
    }

    public Optional<UserAccount> findUserAccount(String userId) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT user_id, user_type, password_hash, password_salt, employee_id, display_name
                        FROM users
                        WHERE lower(user_id) = lower(?)
                        """)) {
            statement.setString(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UserAccount(
                        resultSet.getString("user_id"),
                        UserType.valueOf(resultSet.getString("user_type")),
                        resultSet.getString("password_hash"),
                        resultSet.getString("password_salt"),
                        resultSet.getString("display_name"),
                        resultSet.getString("employee_id")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read user account.", exception);
        }
    }

    public List<Employee> findAllEmployees() {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM employees ORDER BY employee_id");
                ResultSet resultSet = statement.executeQuery()) {
            List<Employee> employees = new ArrayList<>();

            while (resultSet.next()) {
                employees.add(mapEmployee(resultSet));
            }

            return employees;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read employees.", exception);
        }
    }

    public Optional<Employee> findEmployeeById(String employeeId) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM employees WHERE employee_id = ?")) {
            statement.setString(1, employeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapEmployee(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read employee.", exception);
        }
    }

    public void saveEmployee(Employee employee) {
        try (Connection connection = getConnection()) {
            saveEmployee(connection, employee);
            insertEmployeeUser(connection, employee);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save employee.", exception);
        }
    }

    private void saveEmployee(Connection connection, Employee employee) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO employees(
                    employee_id, department, job_title, first_name, last_name, surname, status,
                    company_email, date_of_birth, gender, pay_type, address_line1, address_line2,
                    city, state, zip, picture_path, date_hire, base_salary, hourly_rate,
                    medical_coverage, dependents)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(employee_id) DO UPDATE SET
                    department = excluded.department,
                    job_title = excluded.job_title,
                    first_name = excluded.first_name,
                    last_name = excluded.last_name,
                    surname = excluded.surname,
                    status = excluded.status,
                    company_email = excluded.company_email,
                    date_of_birth = excluded.date_of_birth,
                    gender = excluded.gender,
                    pay_type = excluded.pay_type,
                    address_line1 = excluded.address_line1,
                    address_line2 = excluded.address_line2,
                    city = excluded.city,
                    state = excluded.state,
                    zip = excluded.zip,
                    picture_path = excluded.picture_path,
                    date_hire = excluded.date_hire,
                    base_salary = excluded.base_salary,
                    hourly_rate = excluded.hourly_rate,
                    medical_coverage = excluded.medical_coverage,
                    dependents = excluded.dependents
                """)) {
            setEmployeeParameters(statement, employee);
            statement.executeUpdate();
        }
    }

    private void setEmployeeParameters(PreparedStatement statement, Employee employee) throws SQLException {
        statement.setString(1, employee.getEmployeeId());
        statement.setString(2, employee.getDepartment());
        statement.setString(3, employee.getJobTitle());
        statement.setString(4, employee.getFirstName());
        statement.setString(5, employee.getLastName());
        statement.setString(6, employee.getSurname());
        statement.setString(7, employee.getStatus().name());
        statement.setString(8, employee.getCompanyEmail());
        statement.setString(9, employee.getDateOfBirth().toString());
        statement.setString(10, employee.getGender().name());
        statement.setString(11, employee.getPayType().name());
        statement.setString(12, employee.getAddressLine1());
        statement.setString(13, employee.getAddressLine2());
        statement.setString(14, employee.getCity());
        statement.setString(15, employee.getState());
        statement.setString(16, employee.getZip());
        statement.setString(17, employee.getPicturePath());
        statement.setString(18, employee.getDateHire().toString());
        statement.setDouble(19, employee.getBaseSalary());
        statement.setDouble(20, employee.getHourlyRate());
        statement.setString(21, employee.getMedicalCoverage().name());
        statement.setInt(22, employee.getDependents());
    }

    public void deleteEmployee(String employeeId) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM employees WHERE employee_id = ?")) {
            statement.setString(1, employeeId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete employee.", exception);
        }
    }

    public List<TimeEntry> findTimeEntries(String employeeId, LocalDate startDate, LocalDate endDate) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT * FROM time_entries
                        WHERE employee_id = ? AND work_date BETWEEN ? AND ?
                        ORDER BY work_date
                        """)) {
            statement.setString(1, employeeId);
            statement.setString(2, startDate.toString());
            statement.setString(3, endDate.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                List<TimeEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    entries.add(mapTimeEntry(resultSet));
                }
                return entries;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read time entries.", exception);
        }
    }

    public boolean hasLockedEntries(String employeeId, LocalDate startDate, LocalDate endDate) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT COUNT(*)
                        FROM time_entries
                        WHERE employee_id = ? AND work_date BETWEEN ? AND ? AND locked = 1
                        """)) {
            statement.setString(1, employeeId);
            statement.setString(2, startDate.toString());
            statement.setString(3, endDate.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to check time entry lock status.", exception);
        }
    }

    public void saveTimeEntry(String employeeId, LocalDate workDate, double hoursWorked, double ptoHours) {
        saveTimeEntry(employeeId, workDate, hoursWorked, ptoHours, false);
    }

    public void saveTimeEntry(String employeeId, LocalDate workDate, double hoursWorked, double ptoHours, boolean overrideLocked) {
        try (Connection connection = getConnection()) {
            upsertTimeEntry(connection, employeeId, workDate, hoursWorked, ptoHours, false, overrideLocked);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save time entry.", exception);
        }
    }

    private void upsertTimeEntry(
            Connection connection,
            String employeeId,
            LocalDate workDate,
            double hoursWorked,
            double ptoHours,
            boolean locked,
            boolean overrideLocked) throws SQLException {
        String sql = overrideLocked
                ? """
                    INSERT INTO time_entries(employee_id, work_date, hours_worked, pto_hours, locked)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(employee_id, work_date) DO UPDATE SET
                        hours_worked = excluded.hours_worked,
                        pto_hours = excluded.pto_hours
                    """
                : """
                INSERT INTO time_entries(employee_id, work_date, hours_worked, pto_hours, locked)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(employee_id, work_date) DO UPDATE SET
                    hours_worked = excluded.hours_worked,
                    pto_hours = excluded.pto_hours
                WHERE time_entries.locked = 0
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeId);
            statement.setString(2, workDate.toString());
            statement.setDouble(3, hoursWorked);
            statement.setDouble(4, ptoHours);
            statement.setInt(5, locked ? 1 : 0);
            statement.executeUpdate();
        }
    }

    public int createPayrollRun(LocalDate periodStart, LocalDate periodEnd) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT OR IGNORE INTO payroll_runs(period_start, period_end, created_at, status)
                    VALUES (?, ?, datetime('now'), ?)
                    """)) {
                insert.setString(1, periodStart.toString());
                insert.setString(2, periodEnd.toString());
                insert.setString(3, "CALCULATED");
                insert.executeUpdate();
            }

            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT id FROM payroll_runs WHERE period_start = ? AND period_end = ?
                    """)) {
                select.setString(1, periodStart.toString());
                select.setString(2, periodEnd.toString());
                try (ResultSet resultSet = select.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("id");
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create payroll run.", exception);
        }

        throw new IllegalStateException("Payroll run was not created.");
    }

    public void savePaycheck(int payrollRunId, PayrollResult result) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO paychecks(
                            payroll_run_id, employee_id, gross_pay, medical_deduction,
                            dependent_stipend, taxable_pay, state_tax_employee,
                            federal_tax_employee, social_security_employee, medicare_employee,
                            federal_tax_employer, social_security_employer, medicare_employer, net_pay)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(payroll_run_id, employee_id) DO UPDATE SET
                            gross_pay = excluded.gross_pay,
                            medical_deduction = excluded.medical_deduction,
                            dependent_stipend = excluded.dependent_stipend,
                            taxable_pay = excluded.taxable_pay,
                            state_tax_employee = excluded.state_tax_employee,
                            federal_tax_employee = excluded.federal_tax_employee,
                            social_security_employee = excluded.social_security_employee,
                            medicare_employee = excluded.medicare_employee,
                            federal_tax_employer = excluded.federal_tax_employer,
                            social_security_employer = excluded.social_security_employer,
                            medicare_employer = excluded.medicare_employer,
                            net_pay = excluded.net_pay
                        """)) {
            statement.setInt(1, payrollRunId);
            statement.setString(2, result.getEmployeeId());
            statement.setDouble(3, result.getGrossPay());
            statement.setDouble(4, result.getMedicalDeduction());
            statement.setDouble(5, result.getDependentStipend());
            statement.setDouble(6, result.getTaxablePay());
            statement.setDouble(7, result.getStateTaxEmployee());
            statement.setDouble(8, result.getFederalTaxEmployee());
            statement.setDouble(9, result.getSocialSecurityEmployee());
            statement.setDouble(10, result.getMedicareEmployee());
            statement.setDouble(11, result.getFederalTaxEmployer());
            statement.setDouble(12, result.getSocialSecurityEmployer());
            statement.setDouble(13, result.getMedicareEmployer());
            statement.setDouble(14, result.getNetPay());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save paycheck.", exception);
        }
    }

    public void lockEntriesForPeriod(LocalDate periodStart, LocalDate periodEnd) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        UPDATE time_entries SET locked = 1
                        WHERE work_date BETWEEN ? AND ?
                        """)) {
            statement.setString(1, periodStart.toString());
            statement.setString(2, periodEnd.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to lock payroll time entries.", exception);
        }
    }

    public List<PayrollResult> findPaychecksForRun(int payrollRunId) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT p.*, e.first_name || ' ' || e.last_name AS employee_name
                        FROM paychecks p
                        JOIN employees e ON e.employee_id = p.employee_id
                        WHERE p.payroll_run_id = ?
                        ORDER BY p.employee_id
                        """)) {
            statement.setInt(1, payrollRunId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<PayrollResult> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapPayrollResult(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read paychecks.", exception);
        }
    }

    public Optional<PayrollResult> findLatestPaycheck(String employeeId) {
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT p.*, e.first_name || ' ' || e.last_name AS employee_name
                        FROM paychecks p
                        JOIN employees e ON e.employee_id = p.employee_id
                        JOIN payroll_runs r ON r.id = p.payroll_run_id
                        WHERE p.employee_id = ?
                        ORDER BY r.period_end DESC, p.id DESC
                        LIMIT 1
                        """)) {
            statement.setString(1, employeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapPayrollResult(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read latest paycheck.", exception);
        }
    }

    private Employee mapEmployee(ResultSet resultSet) throws SQLException {
        return new Employee(
                resultSet.getString("employee_id"),
                resultSet.getString("department"),
                resultSet.getString("job_title"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("surname"),
                EmployeeStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("company_email"),
                LocalDate.parse(resultSet.getString("date_of_birth")),
                Gender.valueOf(resultSet.getString("gender")),
                PayType.valueOf(resultSet.getString("pay_type")),
                resultSet.getString("address_line1"),
                resultSet.getString("address_line2"),
                resultSet.getString("city"),
                resultSet.getString("state"),
                resultSet.getString("zip"),
                resultSet.getString("picture_path"),
                LocalDate.parse(resultSet.getString("date_hire")),
                resultSet.getDouble("base_salary"),
                resultSet.getDouble("hourly_rate"),
                MedicalCoverage.valueOf(resultSet.getString("medical_coverage")),
                resultSet.getInt("dependents"));
    }

    private TimeEntry mapTimeEntry(ResultSet resultSet) throws SQLException {
        return new TimeEntry(
                resultSet.getInt("id"),
                resultSet.getString("employee_id"),
                LocalDate.parse(resultSet.getString("work_date")),
                resultSet.getDouble("hours_worked"),
                resultSet.getDouble("pto_hours"),
                resultSet.getInt("locked") == 1);
    }

    private PayrollResult mapPayrollResult(ResultSet resultSet) throws SQLException {
        return new PayrollResult(
                resultSet.getString("employee_id"),
                resultSet.getString("employee_name"),
                resultSet.getDouble("gross_pay"),
                resultSet.getDouble("medical_deduction"),
                resultSet.getDouble("dependent_stipend"),
                resultSet.getDouble("taxable_pay"),
                resultSet.getDouble("state_tax_employee"),
                resultSet.getDouble("federal_tax_employee"),
                resultSet.getDouble("social_security_employee"),
                resultSet.getDouble("medicare_employee"),
                resultSet.getDouble("federal_tax_employer"),
                resultSet.getDouble("social_security_employer"),
                resultSet.getDouble("medicare_employer"),
                resultSet.getDouble("net_pay"));
    }

    private static class UserPasswordMigration {
        private final String userId;
        private final String plainTextPassword;

        private UserPasswordMigration(String userId, String plainTextPassword) {
            this.userId = userId;
            this.plainTextPassword = plainTextPassword;
        }
    }
}
