package payroll;

import java.time.LocalDate;
import payroll.model.EmployeeStatus;
import payroll.model.Gender;
import payroll.model.MedicalCoverage;
import payroll.model.PayType;

public class Employee {
    private String employeeId;
    private String department;
    private String jobTitle;
    private String firstName;
    private String lastName;
    private String surname;
    private EmployeeStatus status;
    private String companyEmail;
    private LocalDate dateOfBirth;
    private Gender gender;
    private PayType payType;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zip;
    private String picturePath;
    private LocalDate dateHire;
    private double baseSalary;
    private double hourlyRate;
    private MedicalCoverage medicalCoverage;
    private int dependents;

    public Employee(
            String employeeId,
            String department,
            String jobTitle,
            String firstName,
            String lastName,
            String surname,
            EmployeeStatus status,
            String companyEmail,
            LocalDate dateOfBirth,
            Gender gender,
            PayType payType,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String zip,
            String picturePath,
            LocalDate dateHire,
            double baseSalary,
            double hourlyRate,
            MedicalCoverage medicalCoverage,
            int dependents) {
        this.employeeId = employeeId;
        this.department = department;
        this.jobTitle = jobTitle;
        this.firstName = firstName;
        this.lastName = lastName;
        this.surname = surname;
        this.status = status;
        this.companyEmail = companyEmail;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.payType = payType;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.picturePath = picturePath;
        this.dateHire = dateHire;
        this.baseSalary = baseSalary;
        this.hourlyRate = hourlyRate;
        this.medicalCoverage = medicalCoverage;
        this.dependents = dependents;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public PayType getPayType() {
        return payType;
    }

    public void setPayType(PayType payType) {
        this.payType = payType;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    public LocalDate getDateHire() {
        return dateHire;
    }

    public void setDateHire(LocalDate dateHire) {
        this.dateHire = dateHire;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public MedicalCoverage getMedicalCoverage() {
        return medicalCoverage;
    }

    public void setMedicalCoverage(MedicalCoverage medicalCoverage) {
        this.medicalCoverage = medicalCoverage;
    }

    public int getDependents() {
        return dependents;
    }

    public void setDependents(int dependents) {
        this.dependents = dependents;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return employeeId + " - " + getFullName();
    }
}
