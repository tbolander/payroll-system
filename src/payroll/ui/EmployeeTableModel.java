package payroll.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import payroll.Employee;

public class EmployeeTableModel extends AbstractTableModel {
    private final String[] columns = { "ID", "Name", "Department", "Job", "Status", "Pay Type", "Email" };
    private List<Employee> employees = new ArrayList<>();

    public void setEmployees(List<Employee> employees) {
        this.employees = new ArrayList<>(employees);
        fireTableDataChanged();
    }

    public Employee getEmployeeAt(int rowIndex) {
        return employees.get(rowIndex);
    }

    public int findRowByEmployeeId(String employeeId) {
        for (int index = 0; index < employees.size(); index++) {
            if (employees.get(index).getEmployeeId().equalsIgnoreCase(employeeId)) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return employees.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Employee employee = employees.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> employee.getEmployeeId();
            case 1 -> employee.getFullName();
            case 2 -> employee.getDepartment();
            case 3 -> employee.getJobTitle();
            case 4 -> employee.getStatus();
            case 5 -> employee.getPayType();
            case 6 -> employee.getCompanyEmail();
            default -> "";
        };
    }
}
