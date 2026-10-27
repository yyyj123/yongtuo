# Windows 双击部署

1. 在另一台 Windows 电脑安装并启动 [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/)，完成 WSL 2 / 虚拟化设置，使用 Linux 容器。Docker Compose 需 2.24.4 或更新版本。
2. 从 GitHub 选择 **Code → Download ZIP**，解压整个项目到可写目录。不要直接在压缩包里运行，也不要只复制启动文件。无需安装 Git、Java 或 Node。
3. 双击 **部署并启动.cmd**。首次联网下载镜像、编译、创建数据库，完成后自动打开 `http://localhost:8085/`。
4. 后台为 `http://localhost:8085/manage/`。空数据库会自动创建 `admin`，随机密码保存于根目录的 `.env.desktop-admin`。已有管理员保持原样，使用原账号登录。
5. 以后继续双击同一文件；双击 **停止服务.cmd** 停止此部署并保留数据。

本入口使用现有 Phase 5 本机 Compose 配置，固定项目名 `yongtuo-desktop`，只绑定本机，不创建公网隧道。默认端口为 8085。首次没有 `.env` 时，复用原初始化脚本生成随机数据库和 JWT 密钥。已有 `.env` 保持原样。

端口冲突时，可在 `.env` 添加或修改 `PHASE5_HTTP_PORT`，同时将 `PUBLIC_SITE_URL`、`ADMIN_SITE_URL`、`API_BASE_URL` 中的端口改为相同值；下次双击会重新构建启动。数据库创建后请保留原密码，不要直接更换 `.env` 中的数据库凭据。

`.env` 和 `.env.desktop-admin` 都是本机私密文件，不要上传或随源码转发。数据保存在 Docker 命名卷中，停止不会删除数据。复制源码到新电脑会建立新的本机数据，不会自动搬迁原电脑的数据。不要用 `docker compose down -v` 排查启动问题。每台电脑建议只保留一个此入口的实例。

首次管理员初始化通过容器内离线 AdminBootstrap 执行，密码经标准输入传入，只有管理员表为空时才执行；启动失败后可直接重试。

失败窗口会保留原因。检查 Docker Desktop / Linux engine、端口、磁盘和内存，以及 Docker 镜像、Maven、npm 下载网络。服务日志可在 Docker Desktop 中找到 `yongtuo-desktop` 查看。

高级命令（仓库根目录）：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\desktop-deploy.ps1 -NoBrowser
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\desktop-deploy.ps1 -Action Stop
```
