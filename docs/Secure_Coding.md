# Secure Coding Notes

This project has a small security module for the payroll system.

## What I Used

- Java `MessageDigest` for MD5 hashing
- SQLite JDBC as the outside library
- Prepared SQL statements for database work
- A login screen for admin and employee users
- Password hashes instead of plain text passwords

## Password Hashing

The class assignment asks for MD5, so I used MD5 in `PasswordHasher.java`.

The hash demo shows the direct MD5 hash:

```
Password: ABCpayroll2026!
MD5 Hash: 582e8194eb2adbe50751c1e37913228f
```

The normal login database adds a salt before hashing. This means the database does not store the same plain MD5 value shown in the hash demo.

## Why MD5 Is Mentioned

MD5 is included because it was required for the assignment. In a payroll program that I intended to deploy, I would probably use something stronger like PBKDF2, bcrypt, or Argon2.

## Secure Coding Practices Used

- User input is checked before saving employee records.
- Login errors do not say whether the user ID or password was wrong.
- SQL uses prepared statements.
- Passwords are stored as hashes.
- The project does not use Java serialization.
- Security code is kept in the `security` package so it is easier to find.

## Hash Demo Command

To run the hashing demo:
-open `payroll-system` or the project folder in VSCode and run in a Terminal:
**powershell -ExecutionPolicy Bypass -File scripts\hash-demo.ps1**
