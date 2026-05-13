$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

& "$PSScriptRoot\compile.ps1"
java --enable-native-access=ALL-UNNAMED -cp "build;lib\*" payroll.tests.PayrollSystemTest
