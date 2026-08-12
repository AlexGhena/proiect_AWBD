# Starts all three backend microservices (userService, bankingService, transactionService)
# Each runs in its own window with the `dev` profile, so Postgres (db `awbd`) must already be running.

$root = $PSScriptRoot
$services = @("userService", "bankingService", "transactionService")

foreach ($svc in $services) {
    $path = Join-Path $root $svc
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$path'; .\mvnw.cmd spring-boot:run" -WindowStyle Normal
}

Write-Host "Started userService (8081), bankingService (8082), transactionService (8083) in separate windows."
