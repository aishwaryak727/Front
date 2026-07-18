# Launches all TeleConnect services from their built jars.
# Build first with:  .\mvnw.cmd clean install
# Then run:          .\run-all.ps1        (logs go to .\logs\<service>.log)
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$env:JAVA_TOOL_OPTIONS = ""
New-Item -ItemType Directory -Force "$root\logs" | Out-Null

$services = @(
    @{ Name = "Eureka";      Port = 8761; Jar = "eurekaserver\target\eurekaserver-0.0.1-SNAPSHOT.jar" },
    @{ Name = "IAM";        Port = 8081; Jar = "IAM\target\IAM-1.0.0.jar" },
    @{ Name = "Subscriber"; Port = 8086; Jar = "subscriber\target\subscriber-1.0.0.jar" },
    @{ Name = "Plan";       Port = 8083; Jar = "Plan\target\plan-service-1.0.0.jar" },
    @{ Name = "Usage";      Port = 8084; Jar = "Usage\target\usagetracking-1.0.0.jar" },
    @{ Name = "Billing";      Port = 8085; Jar = "billing-service\target\billing-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "Notification"; Port = 8087; Jar = "notification\target\notification-0.0.1-SNAPSHOT.jar" },
    @{ Name = "Fault";        Port = 8090; Jar = "Fault-service\target\fault-1.0.0.jar" },
    @{ Name = "Analytics";    Port = 8089; Jar = "analytics-service\target\analytics-service-1.0.0.jar" },
    @{ Name = "Gateway";      Port = 9090; Jar = "gateway\target\gateway-1.0.0.jar" }
)

foreach ($s in $services) {
    $jar = Join-Path $root $s.Jar
    if (-not (Test-Path $jar)) {
        Write-Host "[$($s.Name)] jar not found ($($s.Jar)). Run '.\mvnw.cmd clean install' first." -ForegroundColor Red
        continue
    }
    $logFile = "$root\logs\$($s.Name).log"
    $errFile = "$root\logs\$($s.Name).err.log"
    Start-Process -FilePath "cmd" -ArgumentList @("/c", "java -jar `"$jar`" 1>> `"$logFile`" 2>> `"$errFile`"") `
        -WindowStyle Hidden
    Write-Host "[$($s.Name)] starting on port $($s.Port) -> logs\$($s.Name).log" -ForegroundColor Green
}

Write-Host ""
Write-Host "All services launching. They take ~25-30s to be ready." -ForegroundColor Cyan
Write-Host "Stop them all with:  .\stop-all.ps1"
