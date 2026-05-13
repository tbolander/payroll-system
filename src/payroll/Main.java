package payroll;

import java.nio.file.Path;
import javax.swing.SwingUtilities;
import payroll.data.PayrollDatabase;
import payroll.ui.LoginFrame;

public class Main {
    public static void main(String[] args) {
        Path databasePath = Path.of("data", "payroll.db");
        PayrollDatabase database = new PayrollDatabase(databasePath);
        database.initialize();

        SwingUtilities.invokeLater(() -> new LoginFrame(database).setVisible(true));
    }
}
