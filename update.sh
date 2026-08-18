#!/bin/bash
# ============================================================
# 煤层瓦斯智能分析平台 — 服务器自动更新脚本
# 位置：/opt/meitan/update.sh
# 用法：bash /opt/meitan/update.sh
# ============================================================
set -e

PROJECT_ROOT="/opt/meitan"
JAR_DIR="/opt/meitan-server"
WEB_DIR="/var/www/meitan"
WEB_REVISION_FILE="$WEB_DIR/.meitan-web-revision"
NODE_HEAP_MB="${MEITAN_NODE_HEAP_MB:-768}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
log()  { echo -e "${GREEN}[√]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }

echo ""
echo "========================================"
echo "  煤层瓦斯智能分析平台 - 自动更新"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"
echo ""

# ---- 1. 拉取最新代码 ----
echo -e "${YELLOW}[1/5]${NC} 拉取 GitHub 最新代码..."
cd "$PROJECT_ROOT"
git pull
log "代码已同步到最新"

# ---- 2. 更新数据库 ----
echo ""
echo -e "${YELLOW}[2/5]${NC} 检查并更新数据库..."
sudo mysql < "$PROJECT_ROOT/sql/init.sql" 2>/dev/null && log "数据库已更新" || warn "数据库无变更（可忽略）"

# ---- 3. 构建后端 ----
echo ""
echo -e "${YELLOW}[3/5]${NC} 构建后端..."
cd "$PROJECT_ROOT/meitan-server"

if command -v mvn &>/dev/null; then
    mvn clean package -DskipTests -q
    cp target/*.jar "$JAR_DIR/"
    log "后端构建完成 -> $JAR_DIR"
else
    warn "未安装 Maven，跳过后端构建（仅重启已有 jar）"
fi

# ---- 4. 构建前端 ----
echo ""
echo -e "${YELLOW}[4/5]${NC} 构建前端..."
cd "$PROJECT_ROOT/meitan-web"

if command -v npm &>/dev/null; then
    FRONTEND_REVISION=$(git -C "$PROJECT_ROOT" rev-parse HEAD:meitan-web 2>/dev/null || true)
    DEPLOYED_REVISION=$(cat "$WEB_REVISION_FILE" 2>/dev/null || true)

    if [[ -n "$FRONTEND_REVISION" && "$FRONTEND_REVISION" == "$DEPLOYED_REVISION" ]]; then
        log "前端源码未变化，跳过依赖检查和构建"
    else
        LOCK_HASH=$(sha256sum package-lock.json | awk '{print $1}')
        LOCK_HASH_FILE="node_modules/.meitan-package-lock.sha256"
        INSTALLED_LOCK_HASH=$(cat "$LOCK_HASH_FILE" 2>/dev/null || true)

        if [[ "$LOCK_HASH" == "$INSTALLED_LOCK_HASH" ]] || \
           [[ -z "$INSTALLED_LOCK_HASH" && -f node_modules/.package-lock.json && ! package-lock.json -nt node_modules/.package-lock.json ]]; then
            log "前端依赖未变化，跳过 npm install"
        else
            echo "  -> package-lock.json 已变化，更新前端依赖..."
            npm install --prefer-offline --no-audit --no-fund
        fi
        mkdir -p node_modules
        printf '%s\n' "$LOCK_HASH" > "$LOCK_HASH_FILE"

        echo "  -> 执行 Vite 构建（Node 堆内存上限 ${NODE_HEAP_MB} MB）..."
        NODE_OPTIONS="--max-old-space-size=${NODE_HEAP_MB}" npm run build
        mkdir -p "$WEB_DIR"
        cp -r dist/* "$WEB_DIR/"
        printf '%s\n' "$FRONTEND_REVISION" > "$WEB_REVISION_FILE"
        log "前端构建完成 -> $WEB_DIR"
    fi
else
    warn "未安装 Node.js，跳过前端构建（Nginx 使用已有文件）"
fi

# ---- 5. 重启服务 ----
echo ""
echo -e "${YELLOW}[5/5]${NC} 重启服务..."

systemctl restart meitan-python && log "meitan-python 已重启" || warn "meitan-python 重启失败"
systemctl restart meitan-server && log "meitan-server 已重启" || warn "meitan-server 重启失败"

sleep 2
echo ""
echo "========================================"
echo "  当前服务状态"
echo "========================================"
check() {
    if systemctl is-active --quiet "$1"; then
        echo -e "  $1  ${GREEN}● 运行中${NC}"
    else
        echo -e "  $1  ${RED}● 已停止${NC}"
    fi
}
check meitan-python
check meitan-server
check nginx
check mysql

echo ""
log "更新完成！"
