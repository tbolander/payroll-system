package payroll.ui;

public class Money {
    public static String format(double value) {
        return String.format("$%.2f", value);
    }
}
