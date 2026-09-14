[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    docker info --format '{{.ServerVersion}}' *> $null
    if ($LASTEXITCODE -ne 0) { throw '请先打开 Docker Desktop，等待 Docker 启动完成。' }
    docker compose -p yongtuo-phase5 -f docker-compose.local.yml -f docker-compose.phase5.yml up -d
    if ($LASTEXITCODE -ne 0) { throw '本地网站启动失败，请检查 Docker 输出。' }
    docker compose -p yongtuo-public-preview -f docker-compose.public-preview.yml --profile public up -d --pull never
    if ($LASTEXITCODE -ne 0) { throw '公网预览启动失败，请检查 Docker 输出。' }
    $startedAt = docker inspect --format '{{.State.StartedAt}}' yongtuo-public-preview-tunnel-1
    $previewUrl = $null
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        $tunnelLog = (docker compose -p yongtuo-public-preview -f docker-compose.public-preview.yml logs --no-log-prefix --since $startedAt tunnel | Out-String)
        $matchesFound = [regex]::Matches($tunnelLog, 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($matchesFound.Count -gt 0 -and $tunnelLog.Contains('Registered tunnel connection')) {
            $previewUrl = $matchesFound[$matchesFound.Count - 1].Value
            break
        }
        Start-Sleep -Seconds 2
    }
    if (!$previewUrl) { throw '隧道尚未连接。稍后重新运行本脚本即可查看地址。' }
    Write-Host "`n公网预览地址：$previewUrl"
    Write-Host "公网后台地址：$previewUrl/manage/"
    Write-Host '电脑、网络和 Docker 需要保持运行。停止后重新启动，地址可能变化。'
    Write-Host '停止公网预览：运行 scripts\Stop-PublicPreview.ps1'
} finally { Pop-Location }
