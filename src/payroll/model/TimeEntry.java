package payroll.model;

import java.time.LocalDate;

public class TimeEntry {
    private int id;
    private String employeeId;
    private LocalDate workDate;
    private double hoursWorked;
    private double ptoHours;
    private boolean locked;

    public TimeEntry(int id, String employeeId, LocalDate workDate, double hoursWorked, double ptoHours, boolean locked) {
        this.id = id;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.hoursWorked = hoursWorked;
        this.ptoHours = ptoHours;
        this.locked = locked;
    }

    public int getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public double getPtoHours() {
        return ptoHours;
    }

    public boolean isLocked() {
        return locked;
    }
}
