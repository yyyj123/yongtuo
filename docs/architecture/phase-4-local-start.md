# Phase 4 本地启动

工作目录：`E:\yongtuo-site\.worktrees\phase-4-admin`，分支：`codex/phase-4-admin`。

## 启动与访问

```powershell
Set-Location E:\yongtuo-site\.worktrees\phase-4-admin
# 首次执行；已有 .env 时不要重复生成或替换密钥。
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Initialize-Phase4Env.ps1
docker compose -p yongtuo-phase4 -f docker-compose.local.yml -f docker-compose.phase4.yml up --build -d --wait
```

- 后台：http://localhost:8084/manage/
- 健康检查：http://localhost:8084/api/v1/public/site-health
- 此环境仅绑定本机 127.0.0.1；公开站点目前仍是前阶段基础页，成品属于 Phase 5。
- `.env` 中 PHASE4_HTTP_PORT 可调整端口，勿覆盖已有数据库密码和 JWT 密钥。

## 首次管理员

在以上目录的终端执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Initialize-Phase4Admin.ps1
```

按提示设置用户名和密码。密码至少 12 字符、最多 72 个 UTF-8 字节；通过标准输入交给离线初始化工具，不打印密码。仅空管理员表允许创建，不会重置现有账号。随后访问后台登录。

## 员工使用路径

1. 在分类管理新增分类，在参数配置维护参数，进入分类的“参数配置”选择适用项。
2. 产品列表 → 新增产品 → 填公共信息及中文内容 → 按分类动态填写参数/规格 → 保存草稿。
3. 保存后可上传图片、附件；图片排序与附件权限修改需单独保存。公开、下载开关分别控制。
4. 英文可手动填写，保存后核对并“标记为已确认”。生成初稿需先配置翻译供应商；已有英文覆盖前会再次确认。
5. 点击“检查并发布”并确认。列表可复制、上下架、删除；关键操作有二次确认。
6. Excel 下载模板，填写后预检；错误行阻止确认，确认后才写入。分类标识使用分类管理中的 slug。
7. 新闻、案例、证书、目录在对应模块维护；网站设置维护首页固定区块、CNC/能力、公司信息和联系方式。

本机草稿每 30 秒保存未保存内容，并显示保存时间；离开或关闭页面会提醒。草稿保存在当前浏览器，不是跨设备备份，也不会自动发布。

## 当前服务边界

- 对象存储与 AI 翻译默认禁用；显示“此功能尚未配置服务”，须按既有接口另行接入已选定供应商。
- 上传成功及对象存储下载的真实供应商端到端验收尚未进行。单文件校验、关联排序、独立权限、批量匹配由单元/集成测试覆盖；浏览器实际验证未配置时的提示。
- 联系人、图片、产品/证书/目录等真实资料由业务方提供；正式英文公司全称未确认，不得代填。
- 未合并 main，未推送，未开始 Phase 5 或生产部署。

## 重跑浏览器验收

需要 Docker、Python、Node 24.20+、本机 Chrome 与 Edge。先构建 Phase 4 镜像，然后：

```powershell
docker compose -p yongtuo-phase4-test -f docker-compose.local.yml -f docker-compose.acceptance.yml up -d --no-build --wait
node apps/admin/e2e/acceptance.mjs
```

验收仅操作 `yongtuo-phase4-test`（18084），使用独立数据库与明确标记的合成数据。随机测试密码仅保存在被忽略的 `.env.acceptance-auth.json`，不打印、不提交。重跑时复用测试账号并创建新标识数据。验收脚本包含改密失效验证，会同步更新该本地测试文件。

完成后可清理 **仅测试项目**：

```powershell
docker compose -p yongtuo-phase4-test -f docker-compose.local.yml -f docker-compose.acceptance.yml down -v
Remove-Item -LiteralPath E:\yongtuo-site\.worktrees\phase-4-admin\.env.acceptance-auth.json
```

截图和结果位于 `E:\Codex生成文件\预览\yongtuo-phase4`。正式本地项目 `yongtuo-phase4` 与既有 Phase 2/3 不受测试清理影响。
