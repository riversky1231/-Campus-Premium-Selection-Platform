$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$stateFile = Join-Path $projectRoot 'backend-run/processes.json'
if (-not (Test-Path -LiteralPath $stateFile)) { Write-Host '没有本脚本启动的进程记录。'; exit }
$records = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
foreach ($record in $records) {
    $process = Get-Process -Id $record.Id -ErrorAction SilentlyContinue
    if (-not $process) { continue }
    $command = Get-CimInstance Win32_Process -Filter "ProcessId=$($record.Id)"
    if ($command.CommandLine -and $command.CommandLine.Contains($record.Jar) -and $process.StartTime.ToUniversalTime().Ticks -eq [long]$record.StartedTicks) {
        Stop-Process -Id $record.Id
        Write-Host "已停止 $($record.Name)"
    } else { Write-Warning "PID $($record.Id) 与记录不匹配，保留该进程。" }
}
