# Stops all TeleConnect services + the API gateway by the ports they listen on.
$ports = 9090, 8081, 8086, 8083, 8084, 8085, 8087, 8090, 8089
$pids = $ports | ForEach-Object {
    (Get-NetTCPConnection -LocalPort $_ -State Listen -ErrorAction SilentlyContinue).OwningProcess
} | Sort-Object -Unique

if (-not $pids) { Write-Host "No services listening on $($ports -join ', ')."; return }

foreach ($procid in $pids) {
    try { Stop-Process -Id $procid -Force -ErrorAction Stop; Write-Host "Stopped PID $procid" }
    catch { Write-Host "Could not stop PID $procid" -ForegroundColor Yellow }
}
