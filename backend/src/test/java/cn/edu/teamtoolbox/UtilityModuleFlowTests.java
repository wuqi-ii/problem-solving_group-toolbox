package cn.edu.teamtoolbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.storage.location=target/test-storage")
@AutoConfigureMockMvc
class UtilityModuleFlowTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void temporaryTransferSupportsAnonymousPickupAndDownloadLimit() throws Exception {
        String authorization = register("utility_owner", "工具用户");
        MockMultipartFile file = new MockMultipartFile(
                "file", "handoff.txt", MediaType.TEXT_PLAIN_VALUE, "cross-device payload".getBytes());

        String body = mockMvc.perform(multipart("/api/v1/transfers")
                        .file(file).param("validHours", "2").param("maxDownloads", "1")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pickupCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(body).at("/data/pickupCode").asText();

        mockMvc.perform(get("/api/v1/pickup/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("handoff.txt"))
                .andExpect(jsonPath("$.data.remainingDownloads").value(1));
        mockMvc.perform(get("/api/v1/pickup/{code}/download", code))
                .andExpect(status().isOk())
                .andExpect(content().bytes("cross-device payload".getBytes()));
        mockMvc.perform(get("/api/v1/pickup/{code}", code))
                .andExpect(status().isNotFound());
    }

    @Test
    void deviceSpaceIsPrivateToItsOwner() throws Exception {
        String owner = register("device_owner", "设备空间用户");
        String stranger = register("device_stranger", "其他用户");

        mockMvc.perform(post("/api/v1/device-items/text")
                        .header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"LINK\",\"content\":\"https://example.test/demo\",\"sourceDevice\":\"实验室电脑\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/device-items").header("Authorization", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("https://example.test/demo"));
        mockMvc.perform(get("/api/v1/device-items").header("Authorization", stranger))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private String register(String account, String nickname) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"" + account + "\",\"password\":\"password123\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return "Bearer " + json.at("/data/accessToken").asText();
    }
}
