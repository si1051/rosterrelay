package com.sriram.shiftmate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class Api {

    static final ObjectMapper MAPPER = new ObjectMapper();
    private final MockMvc mvc;

    public Api(MockMvc mvc) {
        this.mvc = mvc;
    }

    public String register(String role, String name) throws Exception {
        String body = """
                {"email":"%s@example.com","password":"Secret123","name":"%s","role":"%s","organization":"Helping Hands"}
                """.formatted(UUID.randomUUID(), name, role);
        String res = mvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return MAPPER.readTree(res).get("token").asText();
    }

    public long createShift(String coordinator, String title, Duration startsIn, Duration length, int capacity)
            throws Exception {
        Instant start = Instant.now().plus(startsIn).truncatedTo(ChronoUnit.SECONDS);
        String body = """
                {"title":"%s","location":"Community Center","startsAt":"%s","endsAt":"%s","capacity":%d}
                """.formatted(title, start, start.plus(length), capacity);
        String res = mvc.perform(MockMvcRequestBuilders.post("/api/coordinator/shifts")
                        .header("Authorization", "Bearer " + coordinator)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return MAPPER.readTree(res).get("id").asLong();
    }

    public ResultActions get(String token, String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token));
    }

    public ResultActions post(String token, String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).header("Authorization", "Bearer " + token));
    }

    public ResultActions post(String token, String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public long signUp(String volunteer, long shiftId) throws Exception {
        return json(post(volunteer, "/api/shifts/" + shiftId + "/signup").andExpect(status().isOk())).get("id").asLong();
    }

    public static JsonNode json(ResultActions r) throws Exception {
        return MAPPER.readTree(r.andReturn().getResponse().getContentAsString());
    }
}
