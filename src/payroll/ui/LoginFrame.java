package payroll.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import payroll.Employee;
import payroll.data.PayrollDatabase;
import payroll.security.AuthService;
import payroll.security.UserAccount;
import payroll.security.UserType;

public class LoginFrame extends JFrame {
    private final PayrollDatabase database;
    private final AuthService authService;
    private final JTextField userIdField = new JTextField("HR0001");
    private final JPasswordField passwordField = new JPasswordField();
    private final JComboBox<UserType> userTypeBox = new JComboBox<>(UserType.values());

    public LoginFrame(PayrollDatabase database) {
        super("ABC Payroll System - Login");
        this.database = database;
        this.authService = new AuthService(database);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 230);
        setLocationRelativeTo(null);
        buildScreen();
    }

    private void buildScreen() {
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("User ID"));
        form.add(userIdField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.add(new JLabel("User Type"));
        form.add(userTypeBox);

        JButton loginButton = new JButton("Login");
        JButton exitButton = new JButton("Exit Program");
        form.add(loginButton);
        form.add(exitButton);

        add(new JLabel("ABC Payroll System", JLabel.CENTER), BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);

        loginButton.addActionListener(event -> login());
        exitButton.addActionListener(event -> System.exit(0));
    }

    private void login() {
        String userId = userIdField.getText().trim();
        String password = new String(passwordField.getPassword());
        UserType userType = (UserType) userTypeBox.getSelectedItem();

        Optional<UserAccount> account = authService.authenticate(userId, password, userType);
        if (account.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Login failed.", "Login", JOptionPane.ERROR_MESSAGE);
            return;
        }

        dispose();

        if (account.get().getUserType() == UserType.ADMIN) {
            new AdminFrame(database, account.get()).setVisible(true);
            return;
        }

        Optional<Employee> employee = database.findEmployeeById(account.get().getEmployeeId());
        if (employee.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Employee profile was not found.", "Login", JOptionPane.ERROR_MESSAGE);
            new LoginFrame(database).setVisible(true);
            return;
        }

        new EmployeeFrame(database, account.get(), employee.get()).setVisible(true);
    }
}
