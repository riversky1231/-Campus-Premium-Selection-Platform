$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$target = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $target) { throw '.env 已存在，保留现有配置，请手动修改。' }
function New-Secret {
    $buffer = New-Object byte[] 36
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($buffer) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($buffer)
}
$names = @('MYSQL_PASSWORD','MYSQL_ROOT_PASSWORD','REDIS_PASSWORD','INTERNAL_TOKEN','NACOS_AUTH_TOKEN','NACOS_IDENTITY_VALUE','NACOS_PASSWORD','RABBITMQ_PASSWORD','SEATA_PASSWORD','SEATA_SECRET','MONITOR_PASSWORD','MARKET_ADMIN_PASSWORD')
$lines = foreach ($name in $names) { '{0}={1}' -f $name,(New-Secret) }
$lines += 'WX_APPID='
$lines += 'WX_SECRET='
[IO.File]::WriteAllLines($target,$lines,[Text.UTF8Encoding]::new($false))
Write-Host "已生成 $target，请自行保存其中的管理员密码。"
