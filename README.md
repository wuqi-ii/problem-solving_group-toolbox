# 问题求解——小组协作工具箱

本仓库用于《实战问题求解》课程项目“小组协作工具箱”的需求分析、系统框架和开发计划管理。

项目面向课程小组、比赛团队和小型项目组，以小组管理和任务协作为核心，提供成员授权、任务发布、成果提交与审核、共享文件库、临时文件中转以及个人跨设备内容传递功能。

## 当前内容

- `需求分析_word/`：简要用户需求、详细系统需求规格说明书及课程模板。
- `plan/项目目标与实施计划.md`：项目目标、功能范围、隐形需求与验收目标。
- `plan/系统总体框架.md`：总体技术架构、业务分区、接口和代码目录建议。
- `plan/分区开发计划书.md`：人员分区、串并行顺序、八周里程碑及资源申请计划。

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

当前已完成工程基础骨架、账号注册登录、JWT 认证、小组创建、邀请码加入、成员查看和组长授权。任务、文件中转与跨设备功能将在此基础上分阶段实现。AI 功能暂不纳入第一版开发范围。
