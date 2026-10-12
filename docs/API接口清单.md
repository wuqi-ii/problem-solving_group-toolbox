# API 接口清单

统一前缀为 `/api/v1`，除注册、登录、系统健康检查和匿名取件外，均需携带 Bearer Token。接口统一返回 `success`、`code`、`message`、`data` 和 `timestamp`；文件下载、预览、二维码与 ZIP 归档直接返回二进制内容。

| 功能域 | 主要接口 | 用途 |
| --- | --- | --- |
| 账号 | `POST /auth/register`、`POST /auth/login`、`POST /auth/logout`、`GET /users/me` | 注册、登录、退出和当前用户 |
| 小组 | `GET/POST /groups`、`POST /groups/join`、`POST /groups/{id}/leave` | 小组列表、创建、加入和退出 |
| 成员权限 | `GET /groups/{id}/members`、`POST/DELETE /groups/{id}/members/{userId}/permissions` | 成员查看与分项授权 |
| 邀请与组长 | `GET/POST /groups/{id}/invitations`、`DELETE /groups/{id}/invitations/{invitationId}`、`POST /groups/{id}/leader/{userId}` | 邀请码、撤销和组长转让 |
| 任务 | `GET/POST /groups/{id}/tasks`、`PATCH /tasks/{id}`、`POST /tasks/{id}/start`、`POST /tasks/{id}/cancel` | 查询、发布、修改、开始与取消 |
| 成果审核 | `POST /tasks/{id}/submissions`、`POST /tasks/{id}/reviews` | 多版本成果提交和审核 |
| 协作 | `GET/POST /groups/{id}/discussions`、`GET/POST /groups/{id}/announcements`、`GET/POST /tasks/{id}/comments` | 讨论、公告和任务留言 |
| 共享资料 | `GET/POST /groups/{id}/files`、`GET /files/{id}/download`、`GET /files/{id}/preview` | 上传、列表、下载和预览 |
| 文件管理 | `PATCH /files/{id}`、`PATCH /files/{id}/category`、`DELETE /files/{id}`、`POST /files/{id}/restore` | 重命名、分类、回收和恢复 |
| 成果归档 | `GET /groups/{id}/task-results/archive` | 打包下载已验收任务的最新附件 |
| 临时中转 | `POST/GET /transfers`、`GET /pickup/{code}`、`GET /pickup/{code}/download`、`GET /pickup/{code}/qr` | 创建分享、匿名取件、下载和二维码 |
| 跨设备空间 | `GET /device-items`、`POST /device-items/text`、`POST /device-items/files` | 本人文字、链接和文件中转 |
| 工具衔接 | `POST /transfer-imports/{code}/device`、`POST /groups/{id}/files/import-device/{itemId}` | 取件转个人空间、个人文件转资料库 |
| 通知 | `GET /notifications`、`GET /notifications/unread-count`、`PATCH /notifications/read-all` | 通知列表、未读数和已读处理 |
| 审计 | `GET /audit-logs` | 当前用户最近 100 条写操作 |
| AI 预留 | `GET /agent/capabilities` | 仅返回关闭状态和预留能力，不调用模型 |

详细请求字段以对应的前端 `src/api` 类型和后端 Request Record 为准。所有通过 ID 访问小组、任务和文件的接口均在服务端重新鉴权。
