# Payroll System Testing Log

| Test ID | Module | Scenario | Test Data | Expected Result | Status |
| --- | --- | --- | --- | --- | --- |
| SEC-001 | Security | Admin password hash demo | `ABCpayroll2026!` | MD5 hash displays as `582e8194eb2adbe50751c1e37913228f` | Passed |
| SEC-002 | Security | Admin login succeeds | `HR0001` / `ABCpayroll2026!` / Admin | Admin menu displays | Passed |
| SEC-003 | Security | Admin login fails with bad password | `HR0001` / wrong password / Admin | Minimal `Login failed.` message displays | Passed |
| SEC-004 | Security | Employee login succeeds | `maya.rodriguez@abccompany.com` / `maya.rodriguez!19880314` / Employee | Employee screen opens | Passed |
| SEC-005 | Security | Employee cannot log in as admin | Employee credentials / Admin type | Minimal `Login failed.` message displays | Passed |
| SEC-006 | Security | Salted database password hash | `HR0001` | Database hash does not equal the unsalted demo MD5 hash | Passed |
| DB-001 | Database | Seed database | First program run | 12 fabricated employees are created | Passed |
| EMP-001 | Employee | Validate seeded employee | `E1002` | Required fields pass validation | Passed |
| EMP-002 | Employee | Search employee iteratively | `E1008` | Employee found | Passed |
| EMP-003 | Employee | Search employee recursively | `E1008` | Employee found | Passed |
| EMP-004 | Employee | Search missing employee recursively | `E9999` | No employee returned | Passed |
| PAY-001 | Payroll | Hourly gross pay | `E1002`, week of `2026-05-04` | Gross pay is `$1353.75` | Passed |
| PAY-002 | Payroll | Medical pretax deduction | `E1002` single coverage | Taxable pay is `$1303.75` | Passed |
| PAY-003 | Payroll | Hourly net pay | `E1002` | Net pay is `$1063.21` | Passed |
| PAY-004 | Payroll | Salary gross pay | `E1001` | Annual salary divided by 52 equals `$1192.31` | Passed |
| PAY-005 | Payroll | Dependent stipend | `E1001`, 2 dependents | Stipend is `$90.00` | Passed |
| PAY-006 | Payroll | Terminated employee excluded | `E1012` terminated | Payroll result includes 11 active employees | Passed |
| REP-001 | Reports | HR sign-off report | Calculate payroll | `reports/hr_signoff_2026-05-04_to_2026-05-10.txt` created | Passed |
| REP-002 | Reports | Payroll print file | Calculate payroll | `reports/payroll_print_file_2026-05-04_to_2026-05-10.csv` created | Passed |
| LOCK-001 | Locking | Employee time lock | Payroll calculated | Employee cannot edit locked payroll week | Passed |
| APP-001 | Startup | Application startup | `scripts/run.ps1` | Swing application starts and stays running | Passed |

Automated verification command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\test.ps1
```

Latest automated result:

```text
Tests passed: 21
Tests failed: 0
```
