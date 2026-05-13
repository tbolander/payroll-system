package payroll.model;

public enum MedicalCoverage {
    SINGLE(50.00),
    FAMILY(100.00);

    private final double weeklyCost;

    MedicalCoverage(double weeklyCost) {
        this.weeklyCost = weeklyCost;
    }

    public double getWeeklyCost() {
        return weeklyCost;
    }
}
