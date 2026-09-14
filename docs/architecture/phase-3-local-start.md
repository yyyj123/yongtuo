# Phase 3 本地启动

使用工作树 `E:\yongtuo-site\.worktrees\phase-3-content-auth`，分支 `codex/phase-3-content-auth`。需要 Docker Desktop 正常运行和支持 `!override` 的 Docker Compose（本机已验证 v5.1.4）。容器负责编译，启动不要求宿主机安装 JDK/Node。

## 1. 环境变量

```powershell
Set-Location 'E:\yongtuo-site\.worktrees\phase-3-content-auth'
# 仅在没有 .env 时执行；已有文件会被保护，不会覆盖。
if (-not (Test-Path .env)) { .\scripts\Initialize-Phase3Env.ps1 }
```

脚本为 `DB_PASSWORD`、`DB_ROOT_PASSWORD`、`JWT_ACCESS_SECRET`、`JWT_REFRESH_SECRET` 分别生成随机值，写入已被 Git 忽略的 `.env`，不打印密钥。数据库卷创建后请保留该文件；修改数据库环境变量不会同步修改已存在数据库的密码。

默认数据库 `DB_HOST=mysql`、`DB_NAME=yongtuo`、`DB_USER=yongtuo`，内部端口 3306 不对宿主机开放。JWT 两个密钥必须不同且各至少 32 字符。`NUXT_PUBLIC_API_BASE=/api/v1` 保持同源代理。对象存储和翻译供应商仍默认禁用。

## 2. 独立启动

```powershell
docker compose -p yongtuo-phase3 -f docker-compose.local.yml -f docker-compose.phase3.yml config --quiet
docker compose -p yongtuo-phase3 -f docker-compose.local.yml -f docker-compose.phase3.yml up --build -d --wait
docker compose -p yongtuo-phase3 -f docker-compose.local.yml -f docker-compose.phase3.yml ps
Invoke-RestMethod 'http://localhost:8083/api/v1/public/site-health'
Invoke-RestMethod 'http://localhost:8083/api/v1/public/home' -Headers @{'Accept-Language'='zh-CN'}
```

固定使用上述项目名和两份 Compose 文件。覆盖文件将 Nginx 端口替换为 8083，项目网络、MySQL 卷均与 Phase 2 隔离，不占用 Phase 2 的 80 端口。首次启动空数据库自动执行 Flyway 至 V21。若 8083 被其他程序占用，在 `.env` 修改 `PHASE3_HTTP_PORT` 并调整访问地址。

- 公开站点骨架：`http://localhost:8083/`
- 后台骨架：`http://localhost:8083/manage/`
- API：`http://localhost:8083/api/v1/`

Phase 3 交付后端；完整后台页面与公开网站分别属于 Phase 4/5，当前页面仍是基础骨架。

## 3. 首次管理员

没有默认账号或密码。服务健康后，由使用者在自己的终端执行：

```powershell
.\scripts\Initialize-Phase3Admin.ps1
```

脚本提示输入自选用户名及隐藏密码。用户名仅含字母、数字、点、下划线、连字符，长度 1–64；密码至少 12 字符、UTF-8 不超过 72 字节。密码经标准输入传入容器内离线工具，使用 BCrypt 后入库，不放入命令参数或配置文件。

工具仅在 `admin_user` 为空时创建一个管理员；已有账号则拒绝，不覆盖或重置密码。创建后使用 `POST /api/v1/admin/auth/login` 登录；详见 `03-api-contract.md`。该工具不是 HTTP 注册接口，必须具有本地 Docker 操作权限。

## 4. 日志与停止

```powershell
docker compose -p yongtuo-phase3 -f docker-compose.local.yml -f docker-compose.phase3.yml logs --tail 100 api
docker compose -p yongtuo-phase3 -f docker-compose.local.yml -f docker-compose.phase3.yml stop
```

`stop` 保留数据。不要用 `down -v` 清除数据库卷。此处命令只操作 `yongtuo-phase3`，不停止 Phase 2。

## 本地 API 测试

需要 JDK 21 和 Docker Desktop：

```powershell
Set-Location 'E:\yongtuo-site\.worktrees\phase-3-content-auth\apps\api'
.\mvnw.cmd clean test
```

测试使用一次性 Testcontainers MySQL 和合成凭据，不需要也不使用本地 `.env` 的真实凭据。
