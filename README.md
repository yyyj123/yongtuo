# 勇拓五金实业官网

YONGTUO 中英双语企业网站，面向五金产品、机械加工件及 CNC 定制加工业务，配套产品与内容管理后台。

中文站使用根路径，英文站使用 `/en/`。网站提供产品浏览、分类筛选、内容展示和联系方式，不包含商城、支付或在线报价。

## 项目文档

- [产品需求](01-PRD.md)
- [数据库设计](02-database-design.md)
- [接口说明](03-api-contract.md)
- [验收清单](05-acceptance-checklist.md)
- [技术决策与上线前准备](06-known-decisions-and-gates.md)

## 目录结构

```text
apps/web/         Nuxt 官网
apps/admin/       Vue 管理后台
apps/api/         Spring Boot API 与 Flyway 数据库迁移
infra/            Nginx 配置与验证脚本
scripts/          环境初始化及运维脚本
docs/architecture/ 本地启动与部署说明
```

## 本地运行

使用 Docker Desktop 的 Linux 容器模式，在仓库根目录执行。首次启动先初始化本地环境变量；已有 `.env` 时保留原文件。

```powershell
if (-not (Test-Path -LiteralPath .env)) { .\scripts\Initialize-Phase3Env.ps1 }
docker compose -p yongtuo-phase5 -f docker-compose.local.yml -f docker-compose.phase5.yml config --quiet
docker compose -p yongtuo-phase5 -f docker-compose.local.yml -f docker-compose.phase5.yml up -d --build --wait
```

- 官网：<http://localhost:8085/>
- 英文站：<http://localhost:8085/en/>
- 管理后台：<http://localhost:8085/manage/>

详细环境说明见 [本地预览](docs/architecture/phase-5-local-start.md)。管理员初始化步骤见 [认证服务本地启动](docs/architecture/phase-3-local-start.md)，执行时应使用目标环境的 Compose 项目名。

`.env` 包含本地凭据，不能提交。数据库结构统一通过 Flyway 迁移。

## 开发与验证

Node、Java 及依赖版本以各应用清单、锁文件和 Dockerfile 为准。API 测试需要运行中的 Docker。

```powershell
Push-Location apps/api
.\mvnw.cmd test
Pop-Location
Push-Location apps/web
npm ci
npm test -- --run
npm run build
Pop-Location
Push-Location apps/admin
npm ci
npm test -- --run
npm run build
Pop-Location
```

CI 配置见 `.github/workflows/verify.yml`。公网域名、HTTPS、生产凭据和备份应按实际部署环境配置。
