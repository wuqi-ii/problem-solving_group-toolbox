package cn.edu.teamtoolbox.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import cn.edu.teamtoolbox.task.TaskReminderService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.storage.location=target/test-storage")
@AutoConfigureMockMvc
class AuthAndGroupFlowTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TaskReminderService taskReminderService;

    @Test
    void userCanRegisterCreateGroupAndReadMembers() throws Exception {
        String registerBody = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"team_leader","password":"password123","nickname":"测试组长"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.account").value("team_leader"))
                .andReturn().getResponse().getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerBody);
        String token = registerJson.at("/data/accessToken").asText();
        String leaderId = registerJson.at("/data/user/id").asText();
        String authorization = "Bearer " + token;

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("测试组长"));

        String createGroupBody = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"课程项目组","description":"完成实战问题求解项目"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("LEADER"))
                .andReturn().getResponse().getContentAsString();

        String groupId = objectMapper.readTree(createGroupBody).at("/data/id").asText();

        String invitationBody = mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validHours\":24,\"maxUses\":5}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String invitationCode = objectMapper.readTree(invitationBody).at("/data/code").asText();

        String memberBody = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"member_one","password":"password123","nickname":"普通成员"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode memberJson = objectMapper.readTree(memberBody);
        String memberAuthorization = "Bearer " + memberJson.at("/data/accessToken").asText();
        String memberId = memberJson.at("/data/user/id").asText();

        mockMvc.perform(post("/api/v1/groups/join")
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + invitationCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("MEMBER"));

        mockMvc.perform(patch("/api/v1/groups/{groupId}/members/me/nickname", groupId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"  资料负责人  \"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/groups/{groupId}/members", groupId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.account == 'member_one')].groupNickname").value("资料负责人"));
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("普通成员"));

        MockMultipartFile upload = new MockMultipartFile(
                "file", "prototype-notes.txt", MediaType.TEXT_PLAIN_VALUE, "prototype v1".getBytes());
        String fileBody = mockMvc.perform(multipart("/api/v1/groups/{groupId}/files", groupId)
                        .file(upload)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("prototype-notes.txt"))
                .andReturn().getResponse().getContentAsString();
        String fileId = objectMapper.readTree(fileBody).at("/data/id").asText();

        MockMultipartFile deviceUpload = new MockMultipartFile(
                "file", "device-draft.txt", MediaType.TEXT_PLAIN_VALUE, "device draft".getBytes());
        String deviceBody = mockMvc.perform(multipart("/api/v1/device-items/files")
                        .file(deviceUpload).param("sourceDevice", "宿舍电脑")
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String deviceItemId = objectMapper.readTree(deviceBody).at("/data/id").asText();
        mockMvc.perform(post("/api/v1/groups/{groupId}/files/import-device/{deviceItemId}", groupId, deviceItemId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("device-draft.txt"));

        MockMultipartFile disposableUpload = new MockMultipartFile(
                "file", "temporary-note.txt", MediaType.TEXT_PLAIN_VALUE, "temporary".getBytes());
        String disposableBody = mockMvc.perform(multipart("/api/v1/groups/{groupId}/files", groupId)
                        .file(disposableUpload).header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andReturn().getResponse().getContentAsString();
        String disposableId = objectMapper.readTree(disposableBody).at("/data/id").asText();
        mockMvc.perform(get("/api/v1/files/{fileId}/preview", disposableId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/files/{fileId}/category", disposableId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"DOCUMENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("DOCUMENT"));
        mockMvc.perform(delete("/api/v1/files/{fileId}", disposableId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/groups/{groupId}/files/trash", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("DELETED"));
        mockMvc.perform(post("/api/v1/files/{fileId}/restore", disposableId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/groups/{groupId}/files/quota", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usedBytes").isNumber())
                .andExpect(jsonPath("$.data.limitBytes").isNumber());

        mockMvc.perform(get("/api/v1/files/{fileId}/download", fileId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/files/{fileId}", fileId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"原型说明.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("原型说明.txt"));

        mockMvc.perform(post("/api/v1/groups/{groupId}/members/{userId}/permissions", groupId, memberId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permission\":\"TASK_CREATE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/groups/{groupId}/members/{userId}/permissions", groupId, memberId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permission\":\"MEMBER_MANAGE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/groups").header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].myPermissions.length()").value(2));
        mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validHours\":12,\"maxUses\":2}"))
                .andExpect(status().isOk());

        String taskBody = mockMvc.perform(post("/api/v1/groups/{groupId}/tasks", groupId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"完成需求原型","description":"提交可评审版本","priority":"HIGH","assigneeIds":["%s"]}
                                """.formatted(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(taskBody).at("/data/id").asText();

        mockMvc.perform(patch("/api/v1/tasks/{taskId}", taskId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"完成高保真需求原型","description":"提交可评审版本","priority":"URGENT","assigneeIds":["%s"]}
                                """.formatted(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("完成高保真需求原型"))
                .andExpect(jsonPath("$.data.priority").value("URGENT"));

        String cancelTaskBody = mockMvc.perform(post("/api/v1/groups/{groupId}/tasks", groupId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"待取消任务","description":"验证取消流程","priority":"LOW","assigneeIds":["%s"]}
                                """.formatted(leaderId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String cancelTaskId = objectMapper.readTree(cancelTaskBody).at("/data/id").asText();
        mockMvc.perform(post("/api/v1/tasks/{taskId}/cancel", cancelTaskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.assignees.length()").value(1));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/start", taskId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", taskId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"第一版原型已经完成\",\"attachmentFileId\":\"" + fileId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestSubmission.versionNo").value(1))
                .andExpect(jsonPath("$.data.latestSubmission.attachmentName").value("原型说明.txt"));

        mockMvc.perform(delete("/api/v1/files/{fileId}", fileId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该文件已作为任务成果附件，不能删除"));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/reviews", taskId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"CHANGES_REQUESTED\",\"comment\":\"补充移动端页面\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NEEDS_CHANGES"));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/start", taskId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", taskId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"第二版已补充移动端页面\",\"attachmentFileId\":\"" + fileId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestSubmission.versionNo").value(2))
                .andExpect(jsonPath("$.data.submissions.length()").value(2));
        mockMvc.perform(post("/api/v1/tasks/{taskId}/reviews", taskId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"comment\":\"验收通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(get("/api/v1/groups/{groupId}/files", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + fileId + "')].category").value("TASK_RESULT"));
        mockMvc.perform(get("/api/v1/groups/{groupId}/task-results/archive", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Archive-File-Count", "1"));

        mockMvc.perform(get("/api/v1/groups/{groupId}/tasks", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title == '完成高保真需求原型')].latestSubmission.versionNo").value(2));

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
        mockMvc.perform(patch("/api/v1/notifications/read-all").header("Authorization", memberAuthorization))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        String discussionBody = mockMvc.perform(post("/api/v1/groups/{groupId}/discussions", groupId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"建议周五前完成演示排练\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("DISCUSSION"))
                .andExpect(jsonPath("$.data.authorName").value("普通成员"))
                .andReturn().getResponse().getContentAsString();
        String discussionId = objectMapper.readTree(discussionBody).at("/data/id").asText();
        mockMvc.perform(get("/api/v1/groups/{groupId}/discussions", groupId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("建议周五前完成演示排练"));

        mockMvc.perform(post("/api/v1/groups/{groupId}/announcements", groupId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"无权限公告\"}"))
                .andExpect(status().isForbidden());
        String announcementBody = mockMvc.perform(post("/api/v1/groups/{groupId}/announcements", groupId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"周五晚进行最终演示排练\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String announcementId = objectMapper.readTree(announcementBody).at("/data/id").asText();
        mockMvc.perform(patch("/api/v1/collaboration-posts/{postId}/pin", announcementId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pinned\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinned").value(true));
        mockMvc.perform(get("/api/v1/groups/{groupId}/announcements", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pinned").value(true));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/comments", taskId)
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"最终版本已经整理完毕\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("TASK_COMMENT"));
        mockMvc.perform(get("/api/v1/tasks/{taskId}/comments", taskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(delete("/api/v1/collaboration-posts/{postId}", discussionId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/groups/{groupId}/discussions", groupId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        String dueAt = Instant.now().plus(2, ChronoUnit.HOURS).toString();
        mockMvc.perform(post("/api/v1/groups/{groupId}/tasks", groupId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"准备演示材料","description":"截止提醒测试","priority":"HIGH","dueAt":"%s","assigneeIds":["%s"]}
                                """.formatted(dueAt, memberId)))
                .andExpect(status().isOk());
        taskReminderService.sendReminders();
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("TASK_DUE_SOON"));

        mockMvc.perform(get("/api/v1/groups").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("课程项目组"));

        mockMvc.perform(get("/api/v1/groups/{groupId}/members", groupId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.account == 'member_one')].permissions.length()").value(2));

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{userId}", groupId, memberId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/groups/{groupId}/tasks", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/tasks/{taskId}/start", taskId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/groups/join")
                        .header("Authorization", memberAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + invitationCode + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/tasks/{taskId}/start", taskId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/groups/{groupId}/members", groupId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.account == 'member_one')].permissions.length()").value(0));

        String managedInvitation = mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validHours\":12,\"maxUses\":2}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String managedInvitationId = objectMapper.readTree(managedInvitation).at("/data/id").asText();
        mockMvc.perform(get("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
        mockMvc.perform(delete("/api/v1/groups/{groupId}/invitations/{invitationId}", groupId, managedInvitationId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/groups/{groupId}/leader/{userId}", groupId, memberId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("MEMBER"));
        mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validHours\":12,\"maxUses\":2}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/groups/{groupId}/leave", groupId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/groups").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/v1/groups/{groupId}/members", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].role").value("LEADER"));

        mockMvc.perform(get("/api/v1/agent/capabilities").header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.provider").value("deepseek"));
    }

    @Test
    void protectedEndpointRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
