package payroll.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import payroll.Employee;
import payroll.data.PayrollDatabase;
import payroll.model.EmployeeStatus;
import payroll.model.PayrollResult;

public class PayrollService {
    private final PayrollDatabase database;
    private final PayrollCalculator calculator;

    public PayrollService(PayrollDatabase database) {
        this.database = database;
        this.calculator = new PayrollCalculator();
    }

    public List<PayrollResult> previewPayroll(LocalDate periodStart, LocalDate periodEnd) {
        List<PayrollResult> results = new ArrayList<>();

        for (Employee employee : database.findAllEmployees()) {
            if (employee.getStatus() != EmployeeStatus.ACTIVE) {
                continue;
            }

            results.add(calculator.calculate(
                    employee,
                    database.findTimeEntries(employee.getEmployeeId(), periodStart, periodEnd),
                    periodStart,
                    periodEnd));
        }

        return results;
    }

    public List<PayrollResult> calculateAndLockPayroll(LocalDate periodStart, LocalDate periodEnd) {
        int runId = database.createPayrollRun(periodStart, periodEnd);
        List<PayrollResult> results = previewPayroll(periodStart, periodEnd);

        for (PayrollResult result : results) {
            database.savePaycheck(runId, result);
        }

        database.lockEntriesForPeriod(periodStart, periodEnd);
        writeReports(periodStart, periodEnd, results);
        return results;
    }

    public PayrollResult previewEmployeePaycheck(Employee employee, LocalDate periodStart, LocalDate periodEnd) {
        return calculator.calculate(
                employee,
                database.findTimeEntries(employee.getEmployeeId(), periodStart, periodEnd),
                periodStart,
                periodEnd);
    }

    public String formatReport(List<PayrollResult> results, LocalDate periodStart, LocalDate periodEnd) {
        StringBuilder report = new StringBuilder();
        report.append("ABC Company Payroll Report\n");
        report.append("Pay Period: ").append(periodStart).append(" to ").append(periodEnd).append("\n\n");
        report.append(String.format("%-8s %-22s %12s %12s %12s%n", "ID", "Employee", "Gross", "Taxes", "Net"));

        double grossTotal = 0;
        double taxTotal = 0;
        double netTotal = 0;

        for (PayrollResult result : results) {
            grossTotal += result.getGrossPay();
            taxTotal += result.getTotalEmployeeTaxes();
            netTotal += result.getNetPay();
            report.append(String.format(
                    "%-8s %-22s $%11.2f $%11.2f $%11.2f%n",
                    result.getEmployeeId(),
                    result.getEmployeeName(),
                    result.getGrossPay(),
                    result.getTotalEmployeeTaxes(),
                    result.getNetPay()));
        }

        report.append("\n");
        report.append(String.format("Gross Payroll Total: $%.2f%n", grossTotal));
        report.append(String.format("Employee Tax Total: $%.2f%n", taxTotal));
        report.append(String.format("Net Payroll Total: $%.2f%n", netTotal));
        report.append("\nHR Sign Off: ________________________________  Date: ____________\n");
        return report.toString();
    }

    private void writeReports(LocalDate periodStart, LocalDate periodEnd, List<PayrollResult> results) {
        try {
            Path reportsDirectory = Path.of("reports");
            Files.createDirectories(reportsDirectory);

            String dateTag = periodStart + "_to_" + periodEnd;
            Files.writeString(
                    reportsDirectory.resolve("hr_signoff_" + dateTag + ".txt"),
                    formatReport(results, periodStart, periodEnd));

            StringBuilder payrollFile = new StringBuilder();
            payrollFile.append("employee_id,employee_name,gross_pay,medical_deduction,dependent_stipend,taxable_pay,employee_taxes,net_pay,employer_taxes\n");
            for (PayrollResult result : results) {
                payrollFile.append(result.getEmployeeId()).append(",");
                payrollFile.append(result.getEmployeeName()).append(",");
                payrollFile.append(result.getGrossPay()).append(",");
                payrollFile.append(result.getMedicalDeduction()).append(",");
                payrollFile.append(result.getDependentStipend()).append(",");
                payrollFile.append(result.getTaxablePay()).append(",");
                payrollFile.append(result.getTotalEmployeeTaxes()).append(",");
                payrollFile.append(result.getNetPay()).append(",");
                payrollFile.append(result.getTotalEmployerTaxes()).append("\n");
            }

            Files.writeString(reportsDirectory.resolve("payroll_print_file_" + dateTag + ".csv"), payrollFile.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write payroll reports.", exception);
        }
    }
}
