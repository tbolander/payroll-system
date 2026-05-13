$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

New-Item -ItemType Directory -Force -Path "build" | Out-Null
$sources = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

if ($sources.Count -eq 0) {
    throw "No Java source files were found."
}

javac -cp "lib\*" -d "build" $sources
Write-Host "Compile complete."
