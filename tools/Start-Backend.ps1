param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$statePath = Join-Path $projectRoot 'backend-run'
New-Item -ItemType Directory -Path $statePath -Force | Out-Null
foreach ($port in @(8080,8081,8082,8083)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { throw "端口 $port 已被占用，请确认后再启动。" }
}
if (-not $SkipBuild) {
    & mvn -f backend/pom.xml package
    if ($LASTEXITCODE -ne 0) { throw '构建或测试失败，未启动任何服务。' }
}
if ([string]::IsNullOrWhiteSpace($env:INTERNAL_TOKEN)) {
    $buffer = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($buffer) } finally { $rng.Dispose() }
    $env:INTERNAL_TOKEN = [Convert]::ToBase64String($buffer)
}
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:MONITOR_ENABLED = 'false'
$env:ADMIN_TOKEN = ''
$javaPath = (Get-Command java).Source
$modules = @(
    @{Name='user';Port=8081;Jar='backend/user-service/target/user-service-1.0.0.jar'},
    @{Name='product';Port=8082;Jar='target/product-service-1.0.0.jar'},
    @{Name='order';Port=8083;Jar='backend/order-service/target/order-service-1.0.0.jar'},
    @{Name='gateway';Port=8080;Jar='backend/gateway/target/gateway-1.0.0.jar'}
)
foreach ($module in $modules) { if (-not (Test-Path -LiteralPath $module.Jar)) { throw "缺少 $($module.Jar)，请先构建。" } }
$processes = @()
foreach ($module in $modules) {
    $jarPath = (Resolve-Path -LiteralPath $module.Jar).Path
    $started = Start-Process -FilePath $javaPath -ArgumentList @('-jar',('"{0}"' -f $jarPath)) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $statePath "$($module.Name).out.log") -RedirectStandardError (Join-Path $statePath "$($module.Name).err.log")
    $processes += @{Name=$module.Name;Id=$started.Id;Jar=$jarPath;StartedTicks=$started.StartTime.ToUniversalTime().Ticks}
    $processes | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $statePath 'processes.json') -Encoding UTF8
}
$processes | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $statePath 'processes.json') -Encoding UTF8
Write-Host '已启动4个进程，日志位于backend-run。请等待各服务健康后运行 tools/Smoke-Test.ps1。'
Write-Host '网关 http://localhost:8080；演示学生 student / StudentDemo123!；管理员 admin / AdminDemo123!'
