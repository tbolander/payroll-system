package payroll.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import payroll.Employee;
import payroll.data.PayrollDatabase;
import payroll.model.PayrollResult;
import payroll.security.AuthService;
import payroll.security.PasswordHasher;
import payroll.security.UserType;
import payroll.service.BinarySearchUtil;
import payroll.service.EmployeeValidator;
import payroll.service.PayrollCalculator;
import payroll.service.PayrollService;
import payroll.service.TimeEntryService;

public class PayrollSystemTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        Path testDatabasePath = Path.of("data", "test-payroll.db");
        Files.deleteIfExists(testDatabasePath);

        PayrollDatabase database = new PayrollDatabase(testDatabasePath);
        database.initialize();

        testSeedData(database);
        testSecurity(database);
        testBinarySearch(database);
        testValidation(database);
        testPayrollCalculations(database);
        testPayrollLocking(database);

        System.out.println();
        System.out.println("Tests passed: " + passed);
        System.out.println("Tests failed: " + failed);

        if (failed > 0) {
            throw new IllegalStateException("One or more tests failed.");
        }
    }

    private static void testSeedData(PayrollDatabase database) {
        check(database.findAllEmployees().size() == 12, "Database seeds 12 fabricated employees.");
    }

    private static void testSecurity(PayrollDatabase database) {
        AuthService authService = new AuthService(database);
        check(PasswordHasher.hashMd5("ABCpayroll2026!").equals("582e8194eb2adbe50751c1e37913228f"),
                "MD5 hash matches expected demo value.");
        check(database.findUserAccount("HR0001").orElseThrow().getPasswordSalt().length() >= 32
                        && !database.findUserAccount("HR0001").orElseThrow().getPasswordHash().equals("582e8194eb2adbe50751c1e37913228f"),
                "Database login hash uses a salt instead of storing the plain MD5 demo hash.");
        check(authService.authenticate("HR0001", "ABCpayroll2026!", UserType.ADMIN).isPresent(),
                "Admin login succeeds.");
        check(authService.authenticate("HR0001", "bad-password", UserType.ADMIN).isEmpty(),
                "Bad admin password fails.");
        check(authService.authenticate("maya.rodriguez@abccompany.com", "maya.rodriguez!19880314", UserType.EMPLOYEE).isPresent(),
                "Employee generated login succeeds.");
        check(authService.authenticate("maya.rodriguez@abccompany.com", "maya.rodriguez!19880314", UserType.ADMIN).isEmpty(),
                "Employee cannot log in as admin.");
    }

    private static void testBinarySearch(PayrollDatabase database) {
        BinarySearchUtil searchUtil = new BinarySearchUtil();
        List<Employee> employees = database.findAllEmployees();

        check(searchUtil.iterativeSearchById(employees, "E1008").isPresent(),
                "Iterative binary search finds an employee.");
        check(searchUtil.recursiveSearchById(employees, "E1008").isPresent(),
                "Recursive binary search finds an employee.");
        check(searchUtil.recursiveSearchById(employees, "E9999").isEmpty(),
                "Recursive binary search returns empty when not found.");
    }

    private static void testValidation(PayrollDatabase database) {
        Employee employee = database.findEmployeeById("E1002").orElseThrow();
        EmployeeValidator validator = new EmployeeValidator();
        check(validator.validate(employee).isEmpty(), "Seed employee passes validation.");
    }

    private static void testPayrollCalculations(PayrollDatabase database) {
        PayrollCalculator calculator = new PayrollCalculator();
        LocalDate start = LocalDate.of(2026, 5, 4);
        LocalDate end = LocalDate.of(2026, 5, 10);
        Employee hourly = database.findEmployeeById("E1002").orElseThrow();
        PayrollResult hourlyResult = calculator.calculate(hourly, database.findTimeEntries("E1002", start, end), start, end);

        checkClose(hourlyResult.getGrossPay(), 1353.75, "Hourly gross includes daily overtime and Saturday time-and-a-half.");
        checkClose(hourlyResult.getMedicalDeduction(), 50.00, "Single medical deduction is $50.");
        checkClose(hourlyResult.getTaxablePay(), 1303.75, "Medical deduction is pretax.");
        checkClose(hourlyResult.getNetPay(), 1063.21, "Hourly net pay is accurate.");

        Employee salary = database.findEmployeeById("E1001").orElseThrow();
        PayrollResult salaryResult = calculator.calculate(salary, database.findTimeEntries("E1001", start, end), start, end);
        checkClose(salaryResult.getGrossPay(), 1192.31, "Salary gross is annual salary divided by 52.");
        checkClose(salaryResult.getDependentStipend(), 90.00, "Dependent stipend is $45 per dependent.");
    }

    private static void testPayrollLocking(PayrollDatabase database) {
        PayrollService payrollService = new PayrollService(database);
        TimeEntryService timeEntryService = new TimeEntryService(database);
        LocalDate start = LocalDate.of(2026, 5, 4);
        LocalDate end = LocalDate.of(2026, 5, 10);

        List<PayrollResult> results = payrollService.calculateAndLockPayroll(start, end);
        check(results.size() == 11, "Payroll excludes terminated employees.");
        check(Files.exists(Path.of("reports", "hr_signoff_2026-05-04_to_2026-05-10.txt")),
                "HR sign-off report is created.");
        check(Files.exists(Path.of("reports", "payroll_print_file_2026-05-04_to_2026-05-10.csv")),
                "Payroll print file is created.");

        boolean lockStoppedEmployeeUpdate = false;
        try {
            Employee employee = database.findEmployeeById("E1002").orElseThrow();
            timeEntryService.saveWeek(employee, start, new double[] { 1, 1, 1, 1, 1, 1, 1 }, 0, false);
        } catch (IllegalStateException exception) {
            lockStoppedEmployeeUpdate = true;
        }
        check(lockStoppedEmployeeUpdate, "Employee time entry is locked after payroll calculation.");
    }

    private static void check(boolean condition, String message) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + message);
        } else {
            failed++;
            System.out.println("FAIL: " + message);
        }
    }

    private static void checkClose(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.01, message + " Expected " + expected + " and got " + actual + ".");
    }
}
