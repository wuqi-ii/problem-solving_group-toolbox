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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        MockMultipartFile upload = new MockMultipartFile(
                "file", "prototype-notes.txt", MediaType.TEXT_PLAIN_VALUE, "prototype v1".getBytes());
        String fileBody = mockMvc.perform(multipart("/api/v1/groups/{groupId}/files", groupId)
                        .file(upload)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("prototype-notes.txt"))
                .andReturn().getResponse().getContentAsString();
        String fileId = objectMapper.readTree(fileBody).at("/data/id").asText();

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

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

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
                        .content("{\"content\":\"第二版已补充移动端页面\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestSubmission.versionNo").value(2));
        mockMvc.perform(post("/api/v1/tasks/{taskId}/reviews", taskId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"comment\":\"验收通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/groups/{groupId}/tasks", groupId)
                        .header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].latestSubmission.versionNo").value(2));

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        mockMvc.perform(patch("/api/v1/notifications/read-all").header("Authorization", memberAuthorization))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", memberAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        mockMvc.perform(get("/api/v1/groups").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("课程项目组"));

        mockMvc.perform(get("/api/v1/groups/{groupId}/members", groupId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.account == 'member_one')].permissions[0]").value("TASK_CREATE"));
    }

    @Test
    void protectedEndpointRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
