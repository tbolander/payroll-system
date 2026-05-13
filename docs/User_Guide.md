# Payroll System User Guide

## How to Run

### Option 1: Run the batch file

1. Simply double-click the file **payroll.bat** in the project folder. It should compile and launch automatically.

### Option 2: Open in VS Code

1. Open the project folder in VS Code
2. Open the file `src/payroll/Main.java`
3. Click the **Run** button above explorer, or press F5

## Admin Login

```text
User ID: HR0001
Password: ABCpayroll2026!
User Type: Admin
```

## Employee Login

Employee user IDs are company email addresses. The initial employee password is the email name, an exclamation point, and date of birth as `YYYYMMDD`.

Example:

User ID: maya.rodriguez@abccompany.com
Password: maya.rodriguez!19880314

User Type: Employee

## Admin Features

Employees tab:

- View all employee records.
- Add a new employee.
- Edit an existing employee.
- Delete an employee.
- Search by employee ID using iterative and recursive binary search.

Time Entry tab:

- Choose an employee.
- Load a payroll week.
- Adjust hours or PTO.
- Save changes.

Calculate Payroll tab:

- Preview payroll.
- Calculate and lock payroll.
- Create HR sign-off report.
- Create payroll print CSV file.

Reports tab:

- View generated report files from the `reports` folder.

Application Info tab:

- View version and security notes.

## Employee Features

Hours / PTO tab:

- Hourly employees enter hours for Monday through Sunday.
- Salary employees are automatically paid 8 hours Monday through Friday and only enter PTO.
- Employees cannot edit a week after payroll is locked.

Paycheck tab:

- Preview gross pay, medical deduction, dependent stipend, taxes, employer taxes, and net pay.

Account Info tab:

- View basic employee profile information.

## Payroll Rules

- Hourly employees receive overtime for hours over 8 in a day.
- Saturday hours are paid at time and a half.
- Salary employees are paid annual salary divided by 52.
- Medical deduction is pretax.
- Single medical coverage is $50 per week.
- Family medical coverage is $100 per week.
- Dependents add a $45 weekly stipend each.
- Indiana state tax is 3.15%.
- Federal employee tax is 7.65%.
- Federal employer tax is 7.65%.
- Social Security employee tax is 6.2%.
- Social Security employer tax is 6.2%.
- Medicare employee tax is 1.45%.
- Medicare employer tax is 1.45%.

## Reports

After payroll is calculated, the application creates:

reports\hr_signoff_YYYY-MM-DD_to_YYYY-MM-DD.txt
reports\payroll_print_file_YYYY-MM-DD_to_YYYY-MM-DD.csv
