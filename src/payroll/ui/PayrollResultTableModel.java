package payroll.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import payroll.model.PayrollResult;

public class PayrollResultTableModel extends AbstractTableModel {
    private final String[] columns = {
            "ID", "Employee", "Gross", "Medical", "Stipend", "Taxable", "Employee Taxes", "Net", "Employer Taxes"
    };
    private List<PayrollResult> results = new ArrayList<>();

    public void setResults(List<PayrollResult> results) {
        this.results = new ArrayList<>(results);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return results.size();
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
        PayrollResult result = results.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> result.getEmployeeId();
            case 1 -> result.getEmployeeName();
            case 2 -> Money.format(result.getGrossPay());
            case 3 -> Money.format(result.getMedicalDeduction());
            case 4 -> Money.format(result.getDependentStipend());
            case 5 -> Money.format(result.getTaxablePay());
            case 6 -> Money.format(result.getTotalEmployeeTaxes());
            case 7 -> Money.format(result.getNetPay());
            case 8 -> Money.format(result.getTotalEmployerTaxes());
            default -> "";
        };
    }
}
