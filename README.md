# 问题求解——小组协作工具箱

本仓库用于《实战问题求解》课程项目“小组协作工具箱”的需求分析、系统框架和开发计划管理。

项目面向课程小组、比赛团队和小型项目组，以小组管理和任务协作为核心，提供成员授权、任务发布、成果提交与审核、共享文件库、凭取件码限时传输文件。

## 当前内容

- `需求分析_word/`：简要用户需求、详细系统需求规格说明书及课程模板。
- `plan/项目目标与实施计划.md`：项目目标、功能范围、隐形需求与验收目标。
- `plan/系统总体框架.md`：总体技术架构、业务分区、接口和代码目录建议。
- `plan/分区开发计划书.md`：人员分区、串并行顺序、八周里程碑及资源申请计划。
- `plan/AI协作Agent设计与开发计划.md`：DeepSeek Agent 的功能、权限、隐私、工具、接口和 A0～A5 开发阶段。
- `plan/系统验收与部署清单.md`：自动化结果、目标环境演练步骤和已知边界。
- `docs/API接口清单.md`、`docs/数据库结构说明.md`：接口与 Flyway V1～V8 数据结构说明。
- `deploy/`：MySQL、后端、前端的 Docker Compose 部署及联合备份恢复脚本。

## 核心业务闭环

> 创建小组 → 邀请成员 → 分配权限 → 发布任务 → 提交成果 → 审核与修改 → 成果归档 → 共享或临时传递

## 工程目录

- `frontend/`：Vue 3、TypeScript、Vite、Pinia、Vue Router 和 Element Plus 前端。
- `backend/`：Spring Boot、Spring Security、JPA、Flyway 后端。
- `plan/`：项目框架、分区计划和资源计划。
- `需求分析_word/`：需求规格说明书及课程模板。
- `storage/`：本地开发文件目录，运行时生成且不提交 Git。

## 本地启动

前端要求 Node.js 20+：

```bash
cd frontend
npm install
npm run dev
```

后端要求 JDK 21+。仓库已包含 Maven Wrapper，不需要单独安装 Maven：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS 或 Linux 使用 `./mvnw spring-boot:run`。

默认开发环境使用内存数据库 H2，并以 MySQL 兼容模式运行，不需要先安装数据库。后端默认地址为 `http://localhost:8080`，前端默认地址为 `http://localhost:5173`，前端会把 `/api` 请求代理到后端。

使用 MySQL 时，先创建数据库并设置环境变量，然后启动 `mysql` Profile：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DB_URL = 'jdbc:mysql://localhost:3306/team_toolbox?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'your-password'
.\mvnw.cmd spring-boot:run
```

## 基础验证

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run build
```

可复制根目录 `.env.example` 作为本地配置参考。详细架构见 `plan/系统总体框架.md`，开发顺序见 `plan/分区开发计划书.md`。

## Docker 部署

安装 Docker Desktop 后进入 `deploy`，先执行 `./init-env.ps1` 生成本地密钥，再执行：

```powershell
docker compose up -d --build
```

如果当前安装提供的是独立 Compose 命令，则使用 `docker-compose up -d --build`。

默认访问 `http://localhost:8088`。部署使用 MySQL 8.4、持久化数据库卷和文件卷，Nginx 统一提供前端与 API 反向代理。备份、恢复及健康检查见 `deploy/README.md`。

当前已完成非 AI 可交付版本：账号注册登录、小组与权限管理、任务发布—提交—审核闭环、小组讨论、任务留言、公告、共享资料库、匿名取件码文件中转，以及任务、公告和评论通知。本地文件存储可通过适配接口替换为 OSS、COS 或 MinIO，基础版本无需申请云端资源。

AI 协作 Agent 已正式纳入后续增强计划，首个模型供应商为 DeepSeek API。计划能力包括项目查询、任务规划与确认发布、受控系统代操作、聊天总结、授权资料问答和进度分析。Agent 采用现有用户权限、工具白名单、写操作确认、隐私同意和审计记录，不具有独立的超级管理员权限。

当前已预留 AI 协作助手页面、`/api/v1/agent/capabilities` 能力描述接口和后端 `AgentToolPort` 工具扩展契约。`app.ai.enabled` 默认关闭，不会调用外部模型；实际 DeepSeek 接入安排在全部非 AI 功能和系统验收完成之后。

小组管理现已支持邀请码历史与取消、成员退出与移除、重新加入、组长转让，以及真正可用的成员管理授权。当前小组会在工作台、任务与资料库之间同步并持久保存，移动端可通过抽屉菜单访问全部核心页面。

任务模块已支持修改、取消、状态筛选、关键词搜索、逾期标记和全部提交版本追溯。取消任务仍保留历史执行人；已作为任务成果附件的资料库文件不能被误删。工作台已经接入真实任务、审核、文件与通知数据，不再使用静态完成状态。

协作模块已提供小组讨论、带权限的公告发布与置顶、任务留言和相关站内通知。任务支持修改通知、截止前 24 小时提醒和逾期提醒；多人任务当前采用共同完成模式。

成员离组后，其旧任务指派会保留为历史记录；通过邀请码重新加入不会自动恢复旧任务执行权。小组成果附件使用共享资料库中的文件。

资料库现已支持分类、同名版本号、图片/文本/PDF 预览、容量配额、回收站、恢复与彻底删除；审核通过的最新附件自动归为任务成果，并可按小组一键打包下载。个人工作台聚合全部可访问小组，统一搜索覆盖任务、文件、讨论和公告。

安全与交付能力包括用户写操作审计、取件接口按 IP 限流、取件二维码、Flyway V1～V8、Docker Compose/MySQL 部署、数据库与文件联合备份恢复脚本。前端提供“操作记录”页面，后端审计失败不会覆盖原业务响应。

临时中转的取件码只在创建成功时显示一次，数据库仅保存 SHA-256 摘要；取件页 `/pickup` 可在未登录浏览器中使用。取件人可直接下载，中转到期、取消或次数耗尽后停止取件。
