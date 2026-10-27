param([ValidateSet('Start','Stop')][string]$Action = 'Start', [switch]$NoBrowser)
. (Join-Path $PSScriptRoot 'desktop-common.ps1')
$root = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $root
$envFile = Join-Path $root '.env'
$lock = $null
try {
    $lock = [IO.File]::Open((Join-Path $root '.env.desktop.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    Assert-DesktopDocker
    if (-not (Test-Path -LiteralPath $envFile)) {
        if ($Action -eq 'Stop') { Write-Host 'No local deployment configuration found.'; exit 0 }
        & (Join-Path $PSScriptRoot 'Initialize-Phase3Env.ps1') -Port 8085
        if (-not (Test-Path -LiteralPath $envFile)) { throw 'Environment initialization failed.' }
        Add-Content -LiteralPath $envFile -Encoding UTF8 -Value "`nPHASE5_HTTP_PORT=8085"
    }
    $values = Read-DesktopEnv $envFile
    $port = Get-DesktopPort $values 'PHASE5_HTTP_PORT' 8085
    $script:Compose = @('compose','--project-name','yongtuo-desktop','--project-directory',$root,'--env-file',$envFile,'-f','docker-compose.local.yml','-f','docker-compose.phase5.yml')
    $script:ContainerPort = 80
    Invoke-DesktopCompose @('config','--quiet')
    if ($Action -eq 'Stop') {
        Invoke-DesktopCompose @('stop')
        Write-Host 'Stopped. Database and passwords are preserved.'
        exit 0
    }
    Assert-DesktopPort $port 'nginx'
    Invoke-DesktopCompose @('up','-d','--build','--wait','--wait-timeout','900')
    # Send the shell script through stdin to preserve quotes in Windows PowerShell 5.1.
    $query = 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" -D"$MYSQL_DATABASE" -N -e "SELECT COUNT(*) FROM admin_user;"'
    $count = $query | & docker @script:Compose exec -T mysql sh
    if ($LASTEXITCODE -ne 0 -or "$count".Trim() -notmatch '^\d+$') { throw 'Could not check the administrator table.' }
    if ([int]"$count".Trim() -eq 0) {
        $accountFile = Join-Path $root '.env.desktop-admin'
        if (Test-Path -LiteralPath $accountFile) {
            $account = Read-DesktopEnv $accountFile
            $password = $account['PASSWORD']
            if (-not $password -or $password.Length -lt 12) { throw 'Invalid .env.desktop-admin. Restore it or remove it before retrying on an empty database.' }
        } else {
            $password = New-DesktopSecret
            Write-NewDesktopFile $accountFile "USERNAME=admin`nPASSWORD=$password`n"
        }
        $oldEncoding = $OutputEncoding
        try {
            $OutputEncoding = New-Object Text.UTF8Encoding($false)
            "admin`n$password" | & docker @script:Compose exec -T api java '-Dloader.main=com.yongtuo.site.auth.AdminBootstrap' -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher
            if ($LASTEXITCODE -ne 0) { throw 'Administrator initialization failed. Retry; no existing account is reset.' }
        } finally { $OutputEncoding = $oldEncoding; $password = $null }
    }
    $url = "http://localhost:$port/"
    Wait-DesktopHttp ($url + 'api/v1/public/site-health')
    Write-Host "Ready: $url"
    Write-Host ("Admin: " + $url + 'manage/')
    Write-Host 'New databases: login credentials are in .env.desktop-admin. Existing accounts are preserved.'
    if (-not $NoBrowser) { Start-Process $url }
} catch {
    Write-Host ("Launch failed: " + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    if ($lock) { $lock.Dispose() }
}
