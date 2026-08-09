# ===================================================
# 煤层瓦斯智能分析平台 — 一键部署脚本
# 用法：PowerShell 中执行 .\deploy.ps1
# ===================================================
param(
    [string]$ServerIP = "121.43.115.221",
    [string]$ServerUser = "root",
    [string]$RemotePath = "/opt/meitan",
    [string]$JarDir = "/opt/meitan-server",
    [string]$WebDir = "/var/www/meitan",
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$SkipRestart
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  煤层瓦斯智能分析平台 — 自动部署开始" -ForegroundColor Cyan
Write-Host "  目标服务器: ${ServerUser}@${ServerIP}" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. Git 拉取最新代码
Write-Host "[1/6] 拉取最新代码..." -ForegroundColor Yellow
Set-Location $ProjectRoot
git pull
Write-Host "  ✅ 代码已更新`n" -ForegroundColor Green

# 2. 构建后端
if (-not $SkipBackend) {
    Write-Host "[2/6] 构建后端 (mvn package)..." -ForegroundColor Yellow
    Set-Location "$ProjectRoot\meitan-server"
    mvn clean package -DskipTests -q
    $JarFile = Get-ChildItem target\*.jar | Select-Object -First 1
    Write-Host "  ✅ 后端构建完成: $($JarFile.Name)`n" -ForegroundColor Green
}

# 3. 构建前端
if (-not $SkipFrontend) {
    Write-Host "[3/6] 构建前端 (npm run build)..." -ForegroundColor Yellow
    Set-Location "$ProjectRoot\meitan-web"
    npm run build
    Write-Host "  ✅ 前端构建完成`n" -ForegroundColor Green
}

# 4. 上传 jar
if (-not $SkipBackend) {
    Write-Host "[4/6] 上传后端 jar 到服务器..." -ForegroundColor Yellow
    scp "$ProjectRoot\meitan-server\target\*.jar" "${ServerUser}@${ServerIP}:${JarDir}/"
    Write-Host "  ✅ jar 上传完成`n" -ForegroundColor Green
}

# 5. 上传前端
if (-not $SkipFrontend) {
    Write-Host "[5/6] 上传前端 dist 到服务器..." -ForegroundColor Yellow
    scp -r "$ProjectRoot\meitan-web\dist\*" "${ServerUser}@${ServerIP}:${WebDir}/"
    Write-Host "  ✅ 前端文件上传完成`n" -ForegroundColor Green
}

# 6. 服务器端更新
Write-Host "[6/6] 服务器端同步代码并重启服务..." -ForegroundColor Yellow
$RemoteScript = @"
echo '  → 拉取服务器代码...'
cd ${RemotePath} && git pull

echo '  → 更新数据库（如有变更）...'
sudo mysql < ${RemotePath}/sql/init.sql 2>/dev/null || true

echo '  → 重启 Python 计算服务...'
systemctl restart meitan-python

echo '  → 重启 Spring Boot 后端...'
systemctl restart meitan-server

echo '  → 检查服务状态...'
sleep 3
systemctl is-active --quiet meitan-python && echo '    meitan-python  ✅' || echo '    meitan-python  ❌'
systemctl is-active --quiet meitan-server && echo '    meitan-server  ✅' || echo '    meitan-server  ❌'
systemctl is-active --quiet nginx          && echo '    nginx           ✅' || echo '    nginx           ❌'
"@

if ($SkipRestart) {
    Write-Host "  ⚠️  跳过重启（--SkipRestart）`n" -ForegroundColor DarkYellow
} else {
    ssh "${ServerUser}@${ServerIP}" $RemoteScript
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  🎉 部署完成！" -ForegroundColor Green
Write-Host "  访问地址: http://${ServerIP}" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan
