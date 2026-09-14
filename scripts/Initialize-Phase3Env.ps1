param([ValidateRange(1024, 65535)][int]$Port = 8083)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envPath) { throw '.env already exists; keep its database credentials and edit it manually if needed.' }
function New-LocalSecret {
    $bytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}
$contents = [IO.File]::ReadAllText((Join-Path $projectRoot '.env.example'))
foreach ($key in @('DB_PASSWORD', 'DB_ROOT_PASSWORD', 'JWT_ACCESS_SECRET', 'JWT_REFRESH_SECRET')) {
    $contents = [regex]::Replace($contents, "(?m)^$key=.*$", "$key=$(New-LocalSecret)")
}
$contents = $contents.Replace('http://localhost', "http://localhost:$Port")
$contents += "`nPHASE3_HTTP_PORT=$Port`n"
# CreateNew also prevents a concurrent invocation from replacing existing credentials.
$stream = [IO.File]::Open($envPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
try {
    $bytes = (New-Object System.Text.UTF8Encoding($false)).GetBytes($contents)
    $stream.Write($bytes, 0, $bytes.Length)
} finally { $stream.Dispose() }
Write-Host 'Created ignored .env with independent random local secrets. No secrets were printed.'
