package payroll.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import payroll.Employee;

public class BinarySearchUtil {
    public Optional<Employee> iterativeSearchById(List<Employee> employees, String targetId) {
        List<Employee> sortedEmployees = sortByEmployeeId(employees);
        int low = 0;
        int high = sortedEmployees.size() - 1;

        while (low <= high) {
            int middle = low + (high - low) / 2;
            Employee middleEmployee = sortedEmployees.get(middle);
            int compare = middleEmployee.getEmployeeId().compareToIgnoreCase(targetId);

            if (compare == 0) {
                return Optional.of(middleEmployee);
            }

            if (compare < 0) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }

        return Optional.empty();
    }

    public Optional<Employee> recursiveSearchById(List<Employee> employees, String targetId) {
        List<Employee> sortedEmployees = sortByEmployeeId(employees);
        return recursiveSearchById(sortedEmployees, targetId, 0, sortedEmployees.size() - 1);
    }

    private Optional<Employee> recursiveSearchById(List<Employee> employees, String targetId, int low, int high) {
        if (low > high) {
            return Optional.empty();
        }

        int middle = low + (high - low) / 2;
        Employee middleEmployee = employees.get(middle);
        int compare = middleEmployee.getEmployeeId().compareToIgnoreCase(targetId);

        if (compare == 0) {
            return Optional.of(middleEmployee);
        }

        if (compare < 0) {
            return recursiveSearchById(employees, targetId, middle + 1, high);
        }

        return recursiveSearchById(employees, targetId, low, middle - 1);
    }

    private List<Employee> sortByEmployeeId(List<Employee> employees) {
        List<Employee> sortedEmployees = new ArrayList<>(employees);
        sortedEmployees.sort(Comparator.comparing(Employee::getEmployeeId, String.CASE_INSENSITIVE_ORDER));
        return sortedEmployees;
    }
}
