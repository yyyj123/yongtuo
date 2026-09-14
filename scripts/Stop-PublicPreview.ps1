[CmdletBinding()]
param([switch]$All)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    docker compose -p yongtuo-public-preview -f docker-compose.public-preview.yml --profile public stop tunnel gateway
    if ($LASTEXITCODE -ne 0) { throw '停止公网预览失败，请检查 Docker 输出。' }
    if ($All) {
        docker compose -p yongtuo-phase5 -f docker-compose.local.yml -f docker-compose.phase5.yml stop
        if ($LASTEXITCODE -ne 0) { throw '停止本地网站失败，请检查 Docker 输出。' }
        Write-Host '公网预览与本地网站均已停止，数据库和文件保留。'
    } else {
        Write-Host '公网预览已停止，本地官网和后台仍可访问。数据库和文件保留。'
    }
} finally { Pop-Location }
