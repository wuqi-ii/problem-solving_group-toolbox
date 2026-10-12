# 部署与备份说明

## Docker Compose 启动

1. 安装 Docker Desktop，并确认 `docker compose version` 可用。
2. 执行 `./init-env.ps1`，生成仅保存在本机的随机数据库密码和 JWT 密钥；也可以手动复制 `.env.example` 后修改。
3. 在本目录执行 `docker compose up -d --build`。若本机安装的是独立 Compose，则使用 `docker-compose up -d --build`。
4. 等待容器健康后访问 `http://localhost:8088`；Web 端口可在 `.env` 中修改。MySQL 默认只绑定本机 `127.0.0.1:3307`，用于迁移验证和维护，不直接暴露到局域网。

该方案使用 MySQL 8.4、Spring Boot 后端和 Nginx 前端。数据库与用户文件分别存入具名卷，Flyway 在后端启动时自动执行 V1～V8 数据库迁移。AI 开关仍保持关闭。

需要快速准备答辩数据时，可在服务启动后执行 `./demo-data.ps1`。脚本会生成唯一的组长/组员账号、小组、公告、讨论、任务、成果文件和已审核记录，并在终端输出本次账号。

若 Docker Hub 直连在拉取基础镜像时反复出现 EOF，可临时从 `m.daocloud.io/docker.io/` 前缀镜像地址拉取对应镜像，再用 `docker tag` 标记为 Dockerfile 中的原始名称后重新构建。镜像加速服务属于外部依赖，只用于下载，部署后的容器运行不依赖它。

## 日常检查

- `docker compose ps` 或 `docker-compose ps`：确认三个服务均运行，MySQL 和后端显示 healthy。
- `docker compose logs -f backend` 或 `docker-compose logs -f backend`：检查迁移或业务错误。
- `http://localhost:8088/api/v1/system/health`：检查应用健康接口。

## 备份与恢复

在本目录执行 `./backup.ps1`，脚本会同时导出 MySQL 和文件存储，生成带时间戳的目录。两部分必须作为同一份备份保管，不能只恢复其中一项。

恢复会替换当前数据库和用户文件，先停止用户写入并另做一次备份，再执行：

```powershell
./restore.ps1 -BackupDirectory ./backups/team-toolbox-时间戳 -ConfirmDataReplacement
```

恢复后应登录测试账号，抽查小组、任务、文件下载和操作记录。生产环境还应将备份复制到另一台设备或独立对象存储，并按学校要求配置 HTTPS。
