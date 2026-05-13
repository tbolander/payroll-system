package payroll.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import payroll.Employee;
import payroll.data.PayrollDatabase;
import payroll.model.PayType;
import payroll.model.TimeEntry;

public class TimeEntryService {
    private final PayrollDatabase database;

    public TimeEntryService(PayrollDatabase database) {
        this.database = database;
    }

    public void saveWeek(Employee employee, LocalDate weekStart, double[] hours, double ptoHours, boolean adminAdjustment) {
        LocalDate normalizedWeekStart = normalizeToMonday(weekStart);
        LocalDate weekEnd = normalizedWeekStart.plusDays(6);

        if (!adminAdjustment && database.hasLockedEntries(employee.getEmployeeId(), normalizedWeekStart, weekEnd)) {
            throw new IllegalStateException("Payroll is locked for this week. Contact HR for adjustments.");
        }

        if (employee.getPayType() == PayType.SALARY) {
            for (int index = 0; index < 7; index++) {
                LocalDate workDate = normalizedWeekStart.plusDays(index);
                double salaryHours = isWeekday(workDate) ? 8.0 : 0.0;
                double ptoForDate = index == 0 ? ptoHours : 0.0;
                database.saveTimeEntry(employee.getEmployeeId(), workDate, salaryHours, ptoForDate, adminAdjustment);
            }
            return;
        }

        for (int index = 0; index < 7; index++) {
            double hoursForDate = hours[index];
            if (hoursForDate < 0 || hoursForDate > 24) {
                throw new IllegalArgumentException("Daily hours must be between 0 and 24.");
            }
            database.saveTimeEntry(employee.getEmployeeId(), normalizedWeekStart.plusDays(index), hoursForDate, 0.0, adminAdjustment);
        }
    }

    public double[] loadHours(Employee employee, LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeToMonday(weekStart);
        List<TimeEntry> entries = database.findTimeEntries(
                employee.getEmployeeId(),
                normalizedWeekStart,
                normalizedWeekStart.plusDays(6));
        Map<LocalDate, TimeEntry> entriesByDate = new HashMap<>();

        for (TimeEntry entry : entries) {
            entriesByDate.put(entry.getWorkDate(), entry);
        }

        double[] hours = new double[7];
        for (int index = 0; index < 7; index++) {
            TimeEntry entry = entriesByDate.get(normalizedWeekStart.plusDays(index));
            hours[index] = entry == null ? 0.0 : entry.getHoursWorked();
        }

        return hours;
    }

    public LocalDate normalizeToMonday(LocalDate date) {
        LocalDate normalized = date;
        while (normalized.getDayOfWeek() != DayOfWeek.MONDAY) {
            normalized = normalized.minusDays(1);
        }
        return normalized;
    }

    private boolean isWeekday(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
