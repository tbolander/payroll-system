package payroll.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import payroll.Employee;
import payroll.model.PayType;
import payroll.model.PayrollResult;
import payroll.model.TimeEntry;

public class PayrollCalculator {
    public static final double STATE_TAX_RATE = 0.0315;
    public static final double FEDERAL_TAX_RATE_EMPLOYEE = 0.0765;
    public static final double FEDERAL_TAX_RATE_EMPLOYER = 0.0765;
    public static final double SOCIAL_SECURITY_RATE_EMPLOYEE = 0.062;
    public static final double SOCIAL_SECURITY_RATE_EMPLOYER = 0.062;
    public static final double MEDICARE_RATE_EMPLOYEE = 0.0145;
    public static final double MEDICARE_RATE_EMPLOYER = 0.0145;
    public static final double DEPENDENT_STIPEND = 45.00;

    public PayrollResult calculate(Employee employee, List<TimeEntry> entries, LocalDate periodStart, LocalDate periodEnd) {
        double grossPay = employee.getPayType() == PayType.SALARY
                ? calculateSalaryGross(employee)
                : calculateHourlyGross(employee, entries);

        double medicalDeduction = employee.getMedicalCoverage().getWeeklyCost();
        double dependentStipend = employee.getDependents() * DEPENDENT_STIPEND;
        double taxablePay = Math.max(0, grossPay + dependentStipend - medicalDeduction);

        double stateTax = round(taxablePay * STATE_TAX_RATE);
        double federalEmployee = round(taxablePay * FEDERAL_TAX_RATE_EMPLOYEE);
        double socialEmployee = round(taxablePay * SOCIAL_SECURITY_RATE_EMPLOYEE);
        double medicareEmployee = round(taxablePay * MEDICARE_RATE_EMPLOYEE);
        double federalEmployer = round(taxablePay * FEDERAL_TAX_RATE_EMPLOYER);
        double socialEmployer = round(taxablePay * SOCIAL_SECURITY_RATE_EMPLOYER);
        double medicareEmployer = round(taxablePay * MEDICARE_RATE_EMPLOYER);

        double netPay = round(grossPay + dependentStipend - medicalDeduction
                - stateTax - federalEmployee - socialEmployee - medicareEmployee);

        return new PayrollResult(
                employee.getEmployeeId(),
                employee.getFullName(),
                round(grossPay),
                round(medicalDeduction),
                round(dependentStipend),
                round(taxablePay),
                stateTax,
                federalEmployee,
                socialEmployee,
                medicareEmployee,
                federalEmployer,
                socialEmployer,
                medicareEmployer,
                netPay);
    }

    private double calculateSalaryGross(Employee employee) {
        return employee.getBaseSalary() / 52.0;
    }

    private double calculateHourlyGross(Employee employee, List<TimeEntry> entries) {
        Map<LocalDate, TimeEntry> entriesByDate = new HashMap<>();
        for (TimeEntry entry : entries) {
            entriesByDate.put(entry.getWorkDate(), entry);
        }

        double totalPay = 0;
        for (TimeEntry entry : entriesByDate.values()) {
            double hours = entry.getHoursWorked();

            if (entry.getWorkDate().getDayOfWeek() == DayOfWeek.SATURDAY) {
                totalPay += hours * employee.getHourlyRate() * 1.5;
            } else {
                double regularHours = Math.min(hours, 8.0);
                double overtimeHours = Math.max(0, hours - 8.0);
                totalPay += (regularHours * employee.getHourlyRate())
                        + (overtimeHours * employee.getHourlyRate() * 1.5);
            }
        }

        return totalPay;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
