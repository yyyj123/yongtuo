# Phase 5 本地预览

目录：`E:\yongtuo-site\.worktrees\phase-5-public-web`，分支：`codex/phase-5-public-web`。

## 访问

- 官网：http://localhost:8085/
- 英文官网：http://localhost:8085/en/
- 管理后台：http://localhost:8085/manage/
- Sitemap：http://localhost:8085/sitemap.xml

管理员账号沿用 `hjy666` 和用户指定的密码。Phase 5 使用独立数据库；首次启动时从 Phase 4 做了只读快照并导入全新的 Phase 5 数据库，未改写 Phase 4。不要将验收测试数据导入预览数据库。

## 启动已有环境

```powershell
Set-Location E:\yongtuo-site\.worktrees\phase-5-public-web
docker compose -p yongtuo-phase5 -f docker-compose.local.yml -f docker-compose.phase5.yml up -d --build --wait
```

已有 `.env` 含本机密钥，不要覆盖或提交。更换域名时设置 `PUBLIC_SITE_URL`；浏览器 API 保持同源 `/api/v1`。

## 补充资料

后台可维护产品、属性、案例、文章、资质、目录、首页固定区块和联系方式。未发布资料不显示在前台，未确认英文不冒充正式英文。工厂、设备、产品照片暂缺，页面保留图片区域或文字空状态。

照片接入既定对象存储服务后，在 `.env` 的 `NUXT_IMAGE_DOMAINS` 配置允许的图片域名，多个域名用逗号分隔，再重新构建 web。列表通过 Nuxt Image/IPX 按尺寸输出 WebP，不回退加载未授权远程原图；首次转换后使用缓存。原有对象存储策略保持不变。

## 验证入口

- `apps/web/e2e/acceptance.mjs`：必须使用隔离验收数据；默认访问 3005，也可通过 `WEB_BASE` 指定验收站点。
- `apps/web/e2e/seed.py`：只写固定的 `yongtuo-phase5-test-mysql-1`。
- `docker-compose.phase5-test.yml`：复用已构建镜像，使用独立项目、18085 端口和独立卷；密钥文件为忽略的 `.env.phase5-test`。
- 前端：`npm test -- --run`、`npm run typecheck`、`npm run build`，基准 Node 24.20.0。
- 后端：在 `apps/api` 执行 `.\mvnw.cmd test`。

Phase 6 的公网域名、HTTPS、生产密钥、对象存储实装和正式备份尚未实施。
