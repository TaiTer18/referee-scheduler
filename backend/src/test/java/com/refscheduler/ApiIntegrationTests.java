package com.refscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refscheduler.model.Game;
import com.refscheduler.model.GameAssignment;
import com.refscheduler.model.RefereeAvailability;
import com.refscheduler.model.User;
import com.refscheduler.repository.GameAssignmentRepository;
import com.refscheduler.repository.GameRepository;
import com.refscheduler.repository.RefereeAvailabilityRepository;
import com.refscheduler.repository.RefreshTokenRepository;
import com.refscheduler.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RefereeAvailabilityRepository availabilityRepository;

    @Autowired
    private GameAssignmentRepository assignmentRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        assignmentRepository.deleteAll();
        availabilityRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        gameRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerLoginMeAndLogoutFlowWorks() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "admin@example.com",
                      "password": "password123",
                      "fullName": "Admin User",
                      "phoneNumber": "555-1000",
                      "role": "ADMIN"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value("admin@example.com"))
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String registeredAccessToken = readAccessToken(registerResponse);
        String registeredRefreshToken = readRefreshToken(registerResponse);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", bearer(registeredAccessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(registeredRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(registeredRefreshToken)))
            .andExpect(status().isUnauthorized());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "admin@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String loginAccessToken = readAccessToken(loginResponse);
        String loginRefreshToken = readRefreshToken(loginResponse);

        String rotatedRefreshResponse = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(loginRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String rotatedRefreshToken = readRefreshToken(rotatedRefreshResponse);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(loginRefreshToken)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(rotatedRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out successfully."));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(rotatedRefreshToken)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", bearer(loginAccessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void adminGameCrudEndpointsWork() throws Exception {
        String adminToken = registerAndGetAccessToken("admin@example.com", "ADMIN");

        String createResponse = mockMvc.perform(post("/api/admin/games")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-01",
                      "gameTime": "18:30:00",
                      "location": "Central Field",
                      "homeTeam": "Lions",
                      "awayTeam": "Tigers",
                      "ageGroup": "U16",
                      "notes": "Bring whistles"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.location").value("Central Field"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Integer gameId = readId(createResponse);

        mockMvc.perform(get("/api/admin/games")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(gameId))
            .andExpect(jsonPath("$[0].homeTeam").value("Lions"));

        mockMvc.perform(put("/api/admin/games/{id}", gameId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-02",
                      "gameTime": "19:00:00",
                      "location": "North Field",
                      "homeTeam": "Lions",
                      "awayTeam": "Bears",
                      "ageGroup": "U18",
                      "status": "OPEN",
                      "notes": "Updated"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.location").value("North Field"))
            .andExpect(jsonPath("$.awayTeam").value("Bears"));

        mockMvc.perform(delete("/api/admin/games/{id}", gameId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Game deleted successfully."));

        mockMvc.perform(get("/api/admin/games")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void refereeAvailabilityAndAssignmentEndpointsWork() throws Exception {
        String adminToken = registerAndGetAccessToken("admin@example.com", "ADMIN");
        String refereeToken = registerAndGetAccessToken("ref@example.com", "REFEREE");

        User referee = userRepository.findByEmail("ref@example.com").orElseThrow();
        Game openGame = saveGame("Open Field", "Lions", "Tigers", LocalDate.of(2026, 6, 1), LocalTime.of(18, 30));
        Game assignmentGame = saveGame("West Field", "Bears", "Sharks", LocalDate.of(2026, 6, 2), LocalTime.of(19, 0));

        mockMvc.perform(get("/api/referees/available-games")
                .header("Authorization", bearer(refereeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].location").value("Open Field"));

        mockMvc.perform(post("/api/referees/availability")
                .header("Authorization", bearer(refereeToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameId": %d,
                      "isAvailable": true
                    }
                    """.formatted(openGame.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").value(openGame.getId()))
            .andExpect(jsonPath("$.isAvailable").value(true));

        mockMvc.perform(post("/api/admin/assignments")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameId": %d,
                      "refereeId": %d,
                      "notes": "Please arrive early"
                    }
                    """.formatted(assignmentGame.getId(), referee.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").value(assignmentGame.getId()))
            .andExpect(jsonPath("$.refereeId").value(referee.getId()))
            .andExpect(jsonPath("$.status").value("ASSIGNED"));

        mockMvc.perform(get("/api/referees/{id}/availability", referee.getId())
                .header("Authorization", bearer(refereeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refereeId").value(referee.getId()))
            .andExpect(jsonPath("$.availability[0].gameId").value(openGame.getId()));

        mockMvc.perform(get("/api/referees/{id}/assignments", referee.getId())
                .header("Authorization", bearer(refereeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].gameId").value(assignmentGame.getId()))
            .andExpect(jsonPath("$[0].notes").value("Please arrive early"));

        mockMvc.perform(get("/api/admin/referees")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(referee.getId()))
            .andExpect(jsonPath("$[0].role").value("REFEREE"));

        mockMvc.perform(get("/api/admin/referees/{id}", referee.getId())
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.referee.id").value(referee.getId()))
            .andExpect(jsonPath("$.availability[0].gameId").value(openGame.getId()))
            .andExpect(jsonPath("$.assignments[0].gameId").value(assignmentGame.getId()));
    }

    @Test
    void adminCanInspectGameRefereesAndUnassign() throws Exception {
        String adminToken = registerAndGetAccessToken("admin@example.com", "ADMIN");
        registerAndGetAccessToken("ref1@example.com", "REFEREE");
        registerAndGetAccessToken("ref2@example.com", "REFEREE");

        User refereeOne = userRepository.findByEmail("ref1@example.com").orElseThrow();
        User refereeTwo = userRepository.findByEmail("ref2@example.com").orElseThrow();
        Game game = saveGame("South Field", "Wolves", "Falcons", LocalDate.of(2026, 6, 3), LocalTime.of(20, 0));

        availabilityRepository.save(new RefereeAvailability(refereeOne.getId(), game.getId(), true));
        availabilityRepository.save(new RefereeAvailability(refereeTwo.getId(), game.getId(), false));

        mockMvc.perform(get("/api/admin/games/{id}/referees", game.getId())
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].referee.role").value("REFEREE"))
            .andExpect(jsonPath("$[0].isAvailable").isBoolean());

        String assignmentResponse = mockMvc.perform(post("/api/admin/assignments")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameId": %d,
                      "refereeId": %d,
                      "notes": "Late game"
                    }
                    """.formatted(game.getId(), refereeOne.getId())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Integer assignmentId = readPathId(assignmentResponse, "assignmentId");

        mockMvc.perform(delete("/api/admin/assignments/{id}", assignmentId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Referee unassigned successfully."));

        Game updatedGame = gameRepository.findById(game.getId()).orElseThrow();
        GameAssignment updatedAssignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(updatedGame.getAssignedRefereeId()).isNull();
        assertThat(updatedGame.getStatus()).isEqualTo("OPEN");
        assertThat(updatedAssignment.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void refereeCannotReadAnotherRefereesData() throws Exception {
        String firstRefToken = registerAndGetAccessToken("ref1@example.com", "REFEREE");
        registerAndGetAccessToken("ref2@example.com", "REFEREE");

        User secondRef = userRepository.findByEmail("ref2@example.com").orElseThrow();

        mockMvc.perform(get("/api/referees/{id}/availability", secondRef.getId())
                .header("Authorization", bearer(firstRefToken)))
            .andExpect(status().isForbidden());
    }

    private String registerAndGetAccessToken(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "password123",
                      "fullName": "%s",
                      "phoneNumber": "555-9999",
                      "role": "%s"
                    }
                    """.formatted(email, role + " User", role)))
            .andExpect(status().isOk())
            .andReturn();

        return readAccessToken(result.getResponse().getContentAsString());
    }

    private Game saveGame(String location, String homeTeam, String awayTeam, LocalDate gameDate, LocalTime gameTime) {
        return gameRepository.save(new Game(
            gameDate,
            gameTime,
            location,
            homeTeam,
            awayTeam,
            "U16"
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String readAccessToken(String json) throws Exception {
        return readPath(json, "accessToken");
    }

    private String readRefreshToken(String json) throws Exception {
        return readPath(json, "refreshToken");
    }

    private Integer readId(String json) throws Exception {
        return readPathId(json, "id");
    }

    private Integer readPathId(String json, String field) throws Exception {
        return Integer.valueOf(readPath(json, field));
    }

    private String readPath(String json, String field) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get(field).asText();
    }
}
