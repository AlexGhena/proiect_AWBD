# Stops any previously started userService/bankingService/transactionService/frontend
# processes (identified by the ports they listen on), then starts Postgres (Windows
# service) if needed, then the full stack again. Postgres itself is never stopped.

function Stop-AwbdPort([int]$port) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($conn in $conns) {
        # Walk up from the listening process (a forked java/node child) to the
        # Start-Process powershell window that owns it, so the whole tree - and
        # the window itself - goes away, not just the leaf process.
        $topId = $conn.OwningProcess
        $current = Get-CimInstance Win32_Process -Filter "ProcessId=$topId" -ErrorAction SilentlyContinue
        while ($current -and $current.ParentProcessId) {
            $parent = Get-CimInstance Win32_Process -Filter "ProcessId=$($current.ParentProcessId)" -ErrorAction SilentlyContinue
            if (-not $parent) { break }
            $topId = $parent.ProcessId
            if ($parent.Name -eq "powershell.exe") { break }
            $current = $parent
        }
        Write-Host "  Stopping process on port $port (PID $topId) and its child processes..."
        taskkill /PID $topId /T /F 2>$null | Out-Null
    }
}

Write-Host "Stopping previously started services..."
foreach ($port in 8081, 8082, 8083, 4200) {
    Stop-AwbdPort -port $port
}

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
    $title = "AWBD-$svc"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = '$title'; cd '$path'; .\mvnw.cmd spring-boot:run" -WindowStyle Normal
}

$frontendPath = Join-Path $root "frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = 'AWBD-frontend'; cd '$frontendPath'; npm start" -WindowStyle Normal

Write-Host "Started userService (8081), bankingService (8082), transactionService (8083), and frontend (4200) in separate windows."
