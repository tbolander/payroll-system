package payroll.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import payroll.Employee;
import payroll.model.PayType;

public class EmployeeValidator {
    public List<String> validate(Employee employee) {
        List<String> errors = new ArrayList<>();

        require(errors, employee.getEmployeeId(), "Employee ID is required.");
        require(errors, employee.getDepartment(), "Department is required.");
        require(errors, employee.getJobTitle(), "Job title is required.");
        require(errors, employee.getFirstName(), "First name is required.");
        require(errors, employee.getLastName(), "Last name is required.");
        require(errors, employee.getSurname(), "Surname is required.");
        require(errors, employee.getCompanyEmail(), "Company email is required.");
        require(errors, employee.getAddressLine1(), "Address line 1 is required.");
        require(errors, employee.getCity(), "City is required.");
        require(errors, employee.getState(), "State is required.");
        require(errors, employee.getZip(), "Zip code is required.");

        if (employee.getCompanyEmail() != null && !employee.getCompanyEmail().contains("@")) {
            errors.add("Company email must include @.");
        }

        if (employee.getDateOfBirth() == null) {
            errors.add("Date of birth is required.");
        } else if (Period.between(employee.getDateOfBirth(), LocalDate.now()).getYears() < 18) {
            errors.add("Employee must be at least 18 years old.");
        }

        if (employee.getDateHire() == null) {
            errors.add("Date hire is required.");
        }

        if (employee.getDependents() < 0) {
            errors.add("Dependents cannot be negative.");
        }

        if (employee.getPayType() == PayType.SALARY && employee.getBaseSalary() <= 0) {
            errors.add("Salary employees must have a base salary greater than 0.");
        }

        if (employee.getPayType() == PayType.HOURLY && employee.getHourlyRate() <= 0) {
            errors.add("Hourly employees must have an hourly rate greater than 0.");
        }

        return errors;
    }

    private void require(List<String> errors, String value, String message) {
        if (value == null || value.isBlank()) {
            errors.add(message);
        }
    }
}
