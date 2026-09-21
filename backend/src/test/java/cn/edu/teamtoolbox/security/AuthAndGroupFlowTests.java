package cn.edu.teamtoolbox.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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

        mockMvc.perform(post("/api/v1/groups/{groupId}/members/{userId}/permissions", groupId, memberId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permission\":\"TASK_CREATE\"}"))
                .andExpect(status().isOk());

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
