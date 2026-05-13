# Payroll Project Design

This is the design plan I used for my payroll system final project. I kept the program simple to understand overall, but it still covers the main requirements from the project requirements doc.

## Program Summary

The program is a Java Swing desktop app. It uses a SQLite database to store employees, users, time entries, payroll runs, and paycheck results.

The main jobs of the program are:

- Let an admin log in.
- Let an employee log in.
- Add, edit, delete, and search employees.
- Enter hours and PTO.
- Calculate gross pay, taxes, and net pay.
- Lock time entries after payroll is calculated.
- Create a payroll report and payroll CSV file.

## Main Screens

- Login screen
- Admin screen
- Employee screen
- Employee management tab
- Time entry tab
- Payroll calculation tab
- Reports tab
- Application information tab

## Database

The project uses SQLite. The database file is:

```text
data/payroll.db
```

Tables used:

- `employees`
- `users`
- `time_entries`
- `payroll_runs`
- `paychecks`

The database starts with 12 fake employee records. These employees are for testing and demonstration, and obviously not to be used.

## Security

The project has a login screen for admins and employees.

## Admin login:

**User ID:** HR0001
**Password:** ABCpayroll2026!

Employee logins use the employee company email. The starting password comes from the email name and date of birth.

Passwords are stored as hashes instead of plain text. One of our class assignments asked for MD5, so the project includes an MD5 hash demo. The normal login database also uses a salt before hashing the password.

The database code uses prepared statements so user input is not placed directly into SQL commands.

## Payroll Rules

- Salary employees are paid annual salary divided by 52.
- Hourly employees enter hours for each day of the week.
- Hours over 8 in one day are paid at time and a half.
- Saturday hours are paid at time and a half.
- Medical is pretax.
- Single medical coverage is $50.
- Family medical coverage is $100.
- Each dependent adds a $45 stipend.
- Indiana state tax is 3.15%.
- Federal employee tax is 7.65%.
- Federal employer tax is 7.65%.
- Social Security employee tax is 6.2%.
- Social Security employer tax is 6.2%.
- Medicare employee tax is 1.45%.
- Medicare employer tax is 1.45%.

## Data Structures

The project uses a few simple data structures:

- Arrays for the 7 daily hour boxes.
- Lists for employee records, time entries, and payroll results.
- Maps for matching time entries to dates.
- SQLite tables for saved data.

## Algorithms

### Payroll Calculation

The payroll calculation goes through each active employee, gets their time entries, calculates gross pay, subtracts pretax medical, adds dependent stipend, calculates taxes, and then finally saves the net pay result.

### Binary Search

The employee search includes both iterative and recursive binary search. I included both because recursion and binary search were a part of the course.

Basic steps:

1. Sort employees by employee ID.
2. Look at the middle employee.
3. If the middle employee is the one being searched for, return it.
4. If the search ID is lower, search the left side.
5. If the search ID is higher, search the right side.

## Big-O Notes

- Binary search is `O(log n)` after the list is sorted.
- Sorting the list first is `O(n log n)`.
- Loading all employees is `O(n)`.
- Payroll calculation is close to `O(n)` for a normal weekly payroll because each employee only has 7 days of time entries.
- Login lookup is treated like a fast database lookup for one user.

## Eight Week Plan

Week 1: Review the payroll requirements and started the first project files. I set up the Java project, decided to use Swing for the screens, and wrote down the main payroll rules that need to be calculated later.

Week 2: Worked on the first algorithm assignments and used them to plan the project. Started the basic employee class, thought about runtime/Big-O, and decided where lists, searches, and validation would be used.

Week 3: Added more algorithm practice from class, including sorting/searching ideas. Began organizing the project into smaller classes instead of putting everything into one file.

Week 4: Built the first real payroll screens and started saving the employee information. Added the employee fields from the project handout and created the first version of the database tables.

Week 5: Improved the payroll code based on the course project checkpoint. Added employee time entry, PTO, salary/hourly pay rules, and the basic tax calculation methods.

Week 6: Added data structure work from class. Used lists for employees and payroll results, arrays for the weekly hour boxes, and database tables for saved records. Kept improving the payroll calculation and reports.

Week 7: Finished the security and algorithms assignments. Added the MD5 hashing demo, a salted password storage, prepared SQL statements, recursive and iterative binary search, and the main testing log.

Week 8: Finished the final project submission. I cleaned up and wrote more of the README, UML, user guide, design notes, testing notes, reports, and GitHub folder. Ran the app and tests one last time before submitting.

## Testing

The project has a simple Java test runner:

**src/payroll/tests/PayrollSystemTest.java**

The testing log is:

**tests/TestCases.md**

The tests check logins, password hashing, seeded employees, binary search, payroll calculations, report creation, and payroll locking.
