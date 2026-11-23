# API 接口清单

本文档定义小组协作工具箱的目标 REST 接口，并区分当前已经实现、需要补充实现和后期 AI 三种状态。统一前缀为 `/api/v1`。除注册、登录、密码重置申请、系统健康检查和匿名取件外，接口均需携带 Bearer Token；文件下载、预览、二维码和 ZIP 归档直接返回二进制内容。

接口统一返回 `success`、`code`、`message`、`data`、`timestamp` 和可选的 `traceId`。写接口应使用服务端状态校验、唯一约束或幂等键防止重复提交。通过资源编号访问小组、任务、讨论和文件时，服务端必须重新校验当前用户、成员关系、资源归属和具体能力。

状态说明：

- **已有**：当前控制器和服务已提供主要能力。
- **补充设计**：需求已经确定，编码阶段需要实现。
- **后期 AI**：不进入当前非 AI 基线，默认关闭。

## 1 认证与用户

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有 | `POST /auth/register` | 注册账号 | 公开、限流 |
| 已有 | `POST /auth/login` | 登录并签发会话 | 公开、限流 |
| 补充设计 | `POST /auth/logout` | 撤销当前会话 | 有效会话 |
| 已有 | `GET /users/me` | 查询当前用户资料 | 当前用户 |
| 补充设计 | `PATCH /users/me` | 修改昵称、头像等资料 | 当前用户 |
| 补充设计 | `POST /auth/password/change` | 修改密码并撤销旧会话 | 当前用户 |
| 补充设计 | `POST /auth/password-reset/request` | 申请密码重置 | 公开、限流、统一提示 |
| 补充设计 | `POST /auth/password-reset/confirm` | 使用一次性凭据重置密码 | 凭据有效且未使用 |

## 2 小组成员与权限

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有 | `GET /groups`、`POST /groups` | 查询本人小组或创建小组 | 登录用户 |
| 补充设计 | `GET /groups/{groupId}` | 查询小组详情 | 本组成员 |
| 补充设计 | `PATCH /groups/{groupId}` | 修改名称、简介和头像 | 组长或成员管理权限 |
| 已有 | `GET /groups/{groupId}/members` | 查询成员及其有效权限 | 本组成员 |
| 已有 | `GET/POST /groups/{groupId}/invitations` | 查看或生成邀请码 | 组长或成员管理权限 |
| 已有 | `DELETE /groups/{groupId}/invitations/{invitationId}` | 撤销邀请码 | 组长或成员管理权限 |
| 已有 | `POST /groups/join` | 使用直接加入邀请码进入小组 | 邀请码有效 |
| 补充设计 | `POST /groups/{groupId}/join-requests` | 提交加入申请 | 登录用户且不是成员 |
| 补充设计 | `GET /groups/{groupId}/join-requests` | 查询待处理申请 | 组长或成员管理权限 |
| 补充设计 | `POST /groups/{groupId}/join-requests/{requestId}/approve` | 批准加入申请 | 组长或成员管理权限 |
| 补充设计 | `POST /groups/{groupId}/join-requests/{requestId}/reject` | 拒绝加入申请 | 组长或成员管理权限 |
| 已有 | `POST /groups/{groupId}/members/{userId}/permissions` | 授予一项权限 | 仅组长 |
| 已有 | `DELETE /groups/{groupId}/members/{userId}/permissions/{permission}` | 撤销一项权限 | 仅组长 |
| 已有 | `DELETE /groups/{groupId}/members/{userId}` | 移除成员 | 组长或成员管理权限，不能移除组长 |
| 已有 | `POST /groups/{groupId}/leave` | 主动退出小组 | 非组长成员 |
| 已有 | `POST /groups/{groupId}/leader/{userId}` | 转让组长 | 当前组长 |

## 3 任务成果与审核

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有并扩展参数 | `GET /groups/{groupId}/tasks?status=&assigneeId=&keyword=&overdue=&page=&size=` | 分页筛选任务 | 本组成员 |
| 已有 | `POST /groups/{groupId}/tasks` | 创建任务 | 组长或任务创建权限 |
| 补充设计 | `GET /tasks/{taskId}` | 查询任务详情、提交版本和审核记录 | 任务所在小组成员 |
| 已有 | `PATCH /tasks/{taskId}` | 修改任务定义和执行人 | 任务管理权限 |
| 补充设计 | `PATCH /tasks/{taskId}/progress` | 更新完成百分比和进度说明 | 当前执行人 |
| 已有 | `POST /tasks/{taskId}/start` | 开始任务 | 当前执行人 |
| 已有 | `POST /tasks/{taskId}/cancel` | 取消任务 | 任务管理权限 |
| 已有 | `POST /tasks/{taskId}/submissions` | 提交或重新提交成果版本 | 当前执行人 |
| 已有 | `POST /tasks/{taskId}/reviews` | 审核通过或退回 | 任务审核权限 |
| 补充设计 | `GET /groups/{groupId}/task-statistics` | 任务状态、逾期、待审核和进度统计 | 本组成员 |
| 补充设计 | `GET /groups/{groupId}/submissions/export?taskIds=&assigneeIds=&versionScope=` | 按范围批量下载提交 | 审核或任务管理权限 |
| 已有 | `GET /groups/{groupId}/task-results/archive` | 下载已验收成果包 | 本组成员 |

## 4 讨论公告通知

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有 | `GET/POST /groups/{groupId}/discussions` | 查询或发布小组讨论 | 本组成员 |
| 已有 | `GET/POST /tasks/{taskId}/comments` | 查询或发布任务留言 | 任务所在小组成员 |
| 已有 | `GET/POST /groups/{groupId}/announcements` | 查询或发布公告 | 查看为成员，发布需公告权限 |
| 补充设计 | `PATCH /collaboration-posts/{postId}` | 修改本人讨论或有权限的公告 | 作者或公告权限 |
| 已有 | `PATCH /collaboration-posts/{postId}/pin` | 置顶或取消置顶 | 公告权限 |
| 已有 | `DELETE /collaboration-posts/{postId}` | 撤回讨论、留言或公告 | 作者或相应管理权限 |
| 已有 | `GET /notifications` | 查询本人通知 | 通知接收人 |
| 已有 | `GET /notifications/unread-count` | 查询未读数量 | 当前用户 |
| 已有 | `PATCH /notifications/{id}/read` | 标记单条通知已读 | 通知接收人 |
| 已有 | `PATCH /notifications/read-all` | 标记全部通知已读 | 当前用户 |
| 补充设计 | `GET/PATCH /notification-preferences` | 查看或维护通知偏好 | 当前用户 |

## 5 小组资料库和文件流转

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有并扩展参数 | `GET /groups/{groupId}/files?keyword=&category=&uploaderId=&page=&size=` | 分页检索有效文件 | 本组成员 |
| 已有 | `POST /groups/{groupId}/files` | 上传共享资料 | 本组成员 |
| 补充设计 | `POST /groups/{groupId}/file-imports` | 从个人设备或临时中转导入独立副本 | 源内容所有者且为本组成员 |
| 已有 | `GET /groups/{groupId}/files/trash` | 查询回收站 | 本组成员 |
| 已有 | `GET /groups/{groupId}/files/quota` | 查询容量使用情况 | 本组成员 |
| 已有 | `GET /files/{fileId}/download` | 下载文件 | 本组成员，重新鉴权 |
| 已有 | `GET /files/{fileId}/preview` | 预览支持的文件 | 本组成员，重新鉴权 |
| 补充设计 | `GET /files/{fileId}/provenance` | 查询导入、任务提交和归档来源 | 本组成员 |
| 已有 | `PATCH /files/{fileId}` | 重命名文件 | 上传者或文件管理权限 |
| 已有 | `PATCH /files/{fileId}/category` | 修改文件分类 | 上传者或文件管理权限 |
| 已有 | `DELETE /files/{fileId}` | 移入回收站 | 上传者或文件管理权限 |
| 已有 | `POST /files/{fileId}/restore` | 恢复文件 | 文件管理权限 |
| 已有 | `DELETE /files/{fileId}/purge` | 彻底删除文件 | 文件管理权限且未被任务引用 |

统一导入请求使用 `sourceType`、`sourceId` 和可选的 `pickupCode` 表示来源。无论来源是个人设备还是临时中转，导入成功后都生成新的小组文件记录和来源记录；原记录删除或到期不影响目标文件。

## 6 临时文件中转

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有 | `POST /transfers` | 创建带有效期和次数限制的分享 | 登录用户 |
| 已有 | `GET /transfers` | 查询本人创建的分享 | 分享创建者 |
| 已有 | `DELETE /transfers/{id}` | 提前撤销分享 | 分享创建者 |
| 已有 | `GET /pickup/{code}` | 匿名查询最小文件信息 | 取件码有效、IP 限流 |
| 已有 | `GET /pickup/{code}/download` | 匿名下载文件 | 未到期且剩余次数有效 |
| 已有 | `GET /pickup/{code}/qr` | 生成取件页面二维码 | 取件码有效 |
| 已有 | `POST /transfer-imports/{code}/device` | 将临时文件保存到本人设备空间 | 登录用户且取件码有效 |

## 7 个人设备空间

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有 | `GET /device-items` | 查询本人文字、链接和文件 | 当前用户 |
| 已有 | `POST /device-items/text` | 保存文字或链接 | 当前用户 |
| 已有 | `POST /device-items/files` | 保存个人文件 | 当前用户 |
| 已有 | `GET /device-items/{id}/download` | 下载本人设备文件 | 内容所有者 |
| 已有 | `DELETE /device-items/{id}` | 删除本人内容 | 内容所有者 |
| 已有，后续由统一接口替代 | `POST /groups/{groupId}/files/import-device/{itemId}` | 导入小组资料库 | 内容所有者且为本组成员 |

## 8 工作台与统一搜索

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 补充设计 | `GET /workbench/summary` | 汇总跨小组待办、待审核、逾期、文件和未读数 | 当前用户 |
| 补充设计 | `GET /workbench/recent-items` | 查询最近任务、文件和通知 | 当前用户 |
| 补充设计 | `GET /search?keyword=&types=&groupIds=&page=&size=` | 分页搜索任务、文件、讨论和公告 | 服务端按当前权限过滤 |

工作台和搜索是只读聚合接口，不允许直接修改其他业务域。页面打开具体对象时，对应业务模块必须再次鉴权。

## 9 公共能力与审计

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有 | `GET /system/health` | 健康检查 | 公开 |
| 已有 | `GET /audit-logs` | 查询本人最近写操作 | 当前用户 |
| 补充设计 | `GET /groups/{groupId}/audit-logs?actorId=&action=&page=&size=` | 查询本组关键业务审计 | 组长或管理权限 |

关键业务服务通过内部 `AuditRecorder` 记录操作者、小组、动作、资源类型、资源编号、结果、时间和关联标识。HTTP 请求过滤器只补充请求级日志，不能替代对象级业务审计。

## 10 AI 项目助手

AI 只保留四项后期功能，不提供文件改名、文件删除、成员管理、AI 授权管理或使用量查看接口。

| 状态 | 方法与路径 | 用途 | 访问条件 |
| --- | --- | --- | --- |
| 已有预留，需调整 | `GET /ai/capabilities` | 返回启用状态和四项用户能力 | 登录用户 |
| 后期 AI | `POST /ai/discussion-summaries` | 整理明确选择的讨论、会议记录或批注 | 对所选内容有访问权 |
| 后期 AI | `POST /ai/knowledge-queries` | 基于选定文件进行带来源问答 | 对所选文件有访问权 |
| 后期 AI | `POST /ai/task-drafts` | 生成可编辑任务规划草稿 | 输入内容在用户权限内 |
| 后期 AI | `POST /ai/task-drafts/{draftId}/apply` | 用户确认后创建正式任务 | 任务管理权限、幂等确认 |
| 后期 AI | `POST /ai/progress-summaries` | 解释系统计算的进度和风险数据 | 目标小组成员 |

AI 接口只处理用户本次明确选择的内容。资料问答必须返回来源；进度数字由任务模块计算；任务草稿在应用前不得写入正式任务。模型不可用时，所有非 AI 接口继续正常工作。

## 11 内部模块接口

| 提供模块 | 接口 | 用途 |
| --- | --- | --- |
| AuthUser | `CurrentUserProvider` | 向业务模块提供当前用户和会话状态 |
| GroupPermission | `GroupAuthorizationPort`、`GroupMemberQueryPort` | 统一成员、归属和分项权限校验 |
| TaskWorkflow | `TaskQueryPort`、`TaskCommandPort` | 向工作台、通知、归档和 AI 提供受控能力 |
| GroupFile | `FileReferencePort`、`FileImportPort` | 校验文件引用并执行跨域副本导入 |
| CollaborationNotification | `NotificationPublisher` | 根据业务事件创建去重通知 |
| CommonAudit | `AuditRecorder` | 写入对象级业务审计 |
| CommonAudit | `FileStoragePort` | 隔离本地目录、MinIO、OSS 或 COS 实现 |
| WorkbenchSearch | `WorkbenchQuery`、`UnifiedSearchQuery` | 提供服务端权限过滤的只读聚合结果 |
