package payroll.model;

public class PayrollResult {
    private final String employeeId;
    private final String employeeName;
    private final double grossPay;
    private final double medicalDeduction;
    private final double dependentStipend;
    private final double taxablePay;
    private final double stateTaxEmployee;
    private final double federalTaxEmployee;
    private final double socialSecurityEmployee;
    private final double medicareEmployee;
    private final double federalTaxEmployer;
    private final double socialSecurityEmployer;
    private final double medicareEmployer;
    private final double netPay;

    public PayrollResult(
            String employeeId,
            String employeeName,
            double grossPay,
            double medicalDeduction,
            double dependentStipend,
            double taxablePay,
            double stateTaxEmployee,
            double federalTaxEmployee,
            double socialSecurityEmployee,
            double medicareEmployee,
            double federalTaxEmployer,
            double socialSecurityEmployer,
            double medicareEmployer,
            double netPay) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.grossPay = grossPay;
        this.medicalDeduction = medicalDeduction;
        this.dependentStipend = dependentStipend;
        this.taxablePay = taxablePay;
        this.stateTaxEmployee = stateTaxEmployee;
        this.federalTaxEmployee = federalTaxEmployee;
        this.socialSecurityEmployee = socialSecurityEmployee;
        this.medicareEmployee = medicareEmployee;
        this.federalTaxEmployer = federalTaxEmployer;
        this.socialSecurityEmployer = socialSecurityEmployer;
        this.medicareEmployer = medicareEmployer;
        this.netPay = netPay;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getMedicalDeduction() {
        return medicalDeduction;
    }

    public double getDependentStipend() {
        return dependentStipend;
    }

    public double getTaxablePay() {
        return taxablePay;
    }

    public double getStateTaxEmployee() {
        return stateTaxEmployee;
    }

    public double getFederalTaxEmployee() {
        return federalTaxEmployee;
    }

    public double getSocialSecurityEmployee() {
        return socialSecurityEmployee;
    }

    public double getMedicareEmployee() {
        return medicareEmployee;
    }

    public double getFederalTaxEmployer() {
        return federalTaxEmployer;
    }

    public double getSocialSecurityEmployer() {
        return socialSecurityEmployer;
    }

    public double getMedicareEmployer() {
        return medicareEmployer;
    }

    public double getTotalEmployeeTaxes() {
        return stateTaxEmployee + federalTaxEmployee + socialSecurityEmployee + medicareEmployee;
    }

    public double getTotalEmployerTaxes() {
        return federalTaxEmployer + socialSecurityEmployer + medicareEmployer;
    }

    public double getNetPay() {
        return netPay;
    }
}
