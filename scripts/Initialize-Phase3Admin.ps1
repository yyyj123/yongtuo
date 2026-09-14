$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$username = Read-Host 'Choose initial administrator username (letters, digits, dot, underscore, hyphen)'
if ($username -notmatch '^[A-Za-z0-9_.-]{1,64}$') { throw 'Invalid username.' }
$securePassword = Read-Host 'Choose password (at least 12 characters; maximum 72 UTF-8 bytes)' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$oldEncoding = $OutputEncoding
Push-Location $projectRoot
try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    if ($password.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($password) -gt 72 -or $password.Contains("`n") -or $password.Contains("`r")) {
        throw 'Password must contain at least 12 characters and no more than 72 UTF-8 bytes.'
    }
    $OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    "$username`n$password" | docker compose -p yongtuo-phase3 -f docker-compose.local.yml -f docker-compose.phase3.yml exec -T api java '-Dloader.main=com.yongtuo.site.auth.AdminBootstrap' -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher
    if ($LASTEXITCODE -ne 0) { throw 'Administrator initialization did not complete. No existing account was reset.' }
} finally {
    $password = $null
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $securePassword.Dispose()
    $OutputEncoding = $oldEncoding
    Pop-Location
}
