# 临时公网预览：启动与停止

当前验证地址：https://easier-receive-respective-conviction.trycloudflare.com

这是用户选择的临时公网演示，不是 Phase 6 正式生产上线。网站由当前电脑提供，电脑、网络和 Docker Desktop 必须持续运行。关闭或重启隧道后地址可能改变，启动脚本会显示最新地址。Cloudflare 的临时隧道说明：https://developers.cloudflare.com/tunnel/setup/

## 启动

1. 打开 Docker Desktop，等待启动完成。
2. 在 `E:\yongtuo-site\.worktrees\phase-5-public-web` 文件夹双击 `启动公网预览.cmd`。
3. 等待窗口显示“公网预览地址”，把该 HTTPS 地址发给访问者。

也可以在 PowerShell 中运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "E:\yongtuo-site\.worktrees\phase-5-public-web\scripts\Start-PublicPreview.ps1"
```

脚本会启动本地官网、API、数据库与公网入口，不重复导入示例内容，不删除数据。已经运行时再次执行可查看当前地址。所需镜像已在本机准备完成，cloudflared 使用已验证的 2026.7.3 镜像摘要。

## 停止

双击同一文件夹中的 `停止公网预览.cmd`，或运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "E:\yongtuo-site\.worktrees\phase-5-public-web\scripts\Stop-PublicPreview.ps1"
```

这会关闭公网预览；本地官网和后台继续运行。若要连本地网站一起停止：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "E:\yongtuo-site\.worktrees\phase-5-public-web\scripts\Stop-PublicPreview.ps1" -All
```

这两种停止方式都保留数据库与图片。无需执行删除数据卷的命令。

## 访问范围与验证

- 官网、图片和 `/api/v1/public/` 的只读请求可通过 HTTPS 访问。
- 用户于 2026-09-07 授权开放公网后台：在公网地址后加 `/manage/`，使用现有管理员账号登录。管理 API 仍由原有身份认证保护；官网公开 API 只允许读取。
- 隐藏文件、其他 API、健康检查及接口文档仍不通过公网入口。
- 临时预览设置 `X-Robots-Tag: noindex` 和 robots 禁止抓取；当前本机 canonical 信息不作为正式 SEO 上线结果。
- 本地后台仍在 http://127.0.0.1:8085/manage/ 使用原管理员账号。
- 公网入口本机检查地址：http://127.0.0.1:8086/ 。
- Nginx 配置检测、网关路由/方法限制检查、HTTPS 检查和启动/停止再启动演练均完成。停止演练后重新启动得到上面的当前地址。

维护文件：`docker-compose.public-preview.yml`、`infra/nginx/public-preview.conf`、`scripts/Start-PublicPreview.ps1`、`scripts/Stop-PublicPreview.ps1`。

实测结果（2026-09-07）：当前公网 HTTPS 地址的首页、产品列表、新闻列表、产品搜索均通过 Chrome 手机/桌面检查，未发现浏览器控制台错误或横向溢出。HTTP 检查确认公开资源返回 200、公开接口写入返回 405。开放后台后补充验证：登录页和静态资源返回 200；未登录的管理读取、修改、删除返回 401；无效刷新令牌返回 401；真实 HTTPS 登录、联系方式总览、刷新页面续期及退出均通过，未修改业务内容。

后台与官网使用同一临时隧道，启动/停止方式相同。地址变化后，后台地址也相应变化。启动脚本会同时显示两个入口。

回归验证：`python scripts/verify-public-admin.py <公网地址>`；浏览器验证 `node apps/web/e2e/public-admin.mjs`，通过进程环境变量 `YT_TEST_BASE`、`YT_TEST_USERNAME`、`YT_TEST_PASSWORD` 传入配置，不把凭据写入脚本。此次仅调整公网代理路由和操作说明，未改变认证机制、数据结构或数据库。
