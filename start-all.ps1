# Starts Postgres (Windows service), then the full stack: userService, bankingService,
# transactionService, and the Angular frontend. Each backend service runs in its own
# window with the `dev` profile.

$pgService = Get-Service -Name "postgresql-x64-18" -ErrorAction SilentlyContinue
if ($pgService -and $pgService.Status -ne "Running") {
    Write-Host "Starting Postgres service..."
    Start-Service -Name "postgresql-x64-18"
} elseif ($pgService) {
    Write-Host "Postgres already running."
} else {
    Write-Host "Warning: postgresql-x64-18 service not found; make sure Postgres is running manually."
}

$root = $PSScriptRoot
$services = @("userService", "bankingService", "transactionService")

foreach ($svc in $services) {
    $path = Join-Path $root $svc
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$path'; .\mvnw.cmd spring-boot:run" -WindowStyle Normal
}

$frontendPath = Join-Path $root "frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendPath'; npm start" -WindowStyle Normal

Write-Host "Started userService (8081), bankingService (8082), transactionService (8083), and frontend (4200) in separate windows."
