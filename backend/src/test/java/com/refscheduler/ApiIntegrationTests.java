package com.refscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refscheduler.model.Game;
import com.refscheduler.model.Organization;
import com.refscheduler.model.OrganizationMembership;
import com.refscheduler.model.User;
import com.refscheduler.repository.GameAssignmentRepository;
import com.refscheduler.repository.GameRepository;
import com.refscheduler.repository.OrganizationMembershipRepository;
import com.refscheduler.repository.OrganizationRepository;
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

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository organizationMembershipRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        assignmentRepository.deleteAll();
        availabilityRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        organizationMembershipRepository.deleteAll();
        gameRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authFlowSupportsOrganizationCreationMembershipsRefreshRotationAndJoinOrganization() throws Exception {
        AuthFixture admin = registerAdmin("admin@example.com", "Club Alpha");

        assertThat(admin.joinCode()).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", bearer(admin.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"));

        AuthFixture referee = registerReferee("ref@example.com", admin.joinCode());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(admin.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(admin.refreshToken())))
            .andExpect(status().isUnauthorized());

        AuthFixture secondAdmin = registerAdmin("admin2@example.com", "Club Beta");

        mockMvc.perform(post("/api/auth/join-organization")
                .header("Authorization", bearer(referee.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "joinCode": "%s"
                    }
                    """.formatted(secondAdmin.joinCode())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationName").value("Club Beta"))
            .andExpect(jsonPath("$.role").value("REFEREE"));

        mockMvc.perform(post("/api/auth/join-organization")
                .header("Authorization", bearer(referee.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "joinCode": "%s"
                    }
                    """.formatted(secondAdmin.joinCode())))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(referee.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out successfully."));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(referee.refreshToken())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminGameCrudEndpointsAreScopedToOrganization() throws Exception {
        AuthFixture adminOne = registerAdmin("admin1@example.com", "Club Alpha");
        AuthFixture adminTwo = registerAdmin("admin2@example.com", "Club Beta");

        String gameOneResponse = mockMvc.perform(post("/api/admin/games")
                .header("Authorization", bearer(adminOne.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-01",
                      "gameTime": "18:30:00",
                      "location": "Alpha Field",
                      "homeTeam": "Lions",
                      "awayTeam": "Tigers",
                      "ageGroup": "U16",
                      "notes": "Alpha game"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationId").value(adminOne.organizationId()))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Integer gameOneId = readPathId(gameOneResponse, "id");

        mockMvc.perform(post("/api/admin/games")
                .header("Authorization", bearer(adminTwo.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-02",
                      "gameTime": "19:00:00",
                      "location": "Beta Field",
                      "homeTeam": "Bears",
                      "awayTeam": "Sharks",
                      "ageGroup": "U18",
                      "notes": "Beta game"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationId").value(adminTwo.organizationId()));

        mockMvc.perform(get("/api/admin/games")
                .header("Authorization", bearer(adminOne.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$[0].location").value("Alpha Field"));

        mockMvc.perform(get("/api/admin/games")
                .header("Authorization", bearer(adminTwo.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$[0].location").value("Beta Field"));

        mockMvc.perform(put("/api/admin/games/{id}", gameOneId)
                .header("Authorization", bearer(adminTwo.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-03",
                      "gameTime": "20:00:00",
                      "location": "Should Fail",
                      "homeTeam": "Bears",
                      "awayTeam": "Wolves"
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/games/{id}", gameOneId)
                .header("Authorization", bearer(adminTwo.accessToken())))
            .andExpect(status().isForbidden());
    }

    @Test
    void refereeCanBelongToManyOrganizationsAndSeeGamesAcrossMemberships() throws Exception {
        AuthFixture adminOne = registerAdmin("admin1@example.com", "Club Alpha");
        AuthFixture adminTwo = registerAdmin("admin2@example.com", "Club Beta");
        AuthFixture referee = registerReferee("ref@example.com", adminOne.joinCode());

        mockMvc.perform(post("/api/auth/join-organization")
                .header("Authorization", bearer(referee.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "joinCode": "%s"
                    }
                    """.formatted(adminTwo.joinCode())))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/games")
                .header("Authorization", bearer(adminOne.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-01",
                      "gameTime": "18:30:00",
                      "location": "Alpha Field",
                      "homeTeam": "Lions",
                      "awayTeam": "Tigers"
                    }
                    """))
            .andExpect(status().isOk());

        String betaGameResponse = mockMvc.perform(post("/api/admin/games")
                .header("Authorization", bearer(adminTwo.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-02",
                      "gameTime": "19:00:00",
                      "location": "Beta Field",
                      "homeTeam": "Bears",
                      "awayTeam": "Sharks"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Integer betaGameId = readPathId(betaGameResponse, "id");
        Integer refereeId = readPathId(referee.responseBody(), "user.id");

        mockMvc.perform(get("/api/referees/available-games")
                .header("Authorization", bearer(referee.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        mockMvc.perform(post("/api/referees/availability")
                .header("Authorization", bearer(referee.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameId": %d,
                      "isAvailable": true
                    }
                    """.formatted(betaGameId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").value(betaGameId))
            .andExpect(jsonPath("$.game.organizationId").value(adminTwo.organizationId()));

        mockMvc.perform(post("/api/admin/assignments")
                .header("Authorization", bearer(adminTwo.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameId": %d,
                      "refereeId": %d,
                      "notes": "Cross-org membership assignment"
                    }
                    """.formatted(betaGameId, refereeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.game.organizationId").value(adminTwo.organizationId()));

        mockMvc.perform(get("/api/referees/{id}/assignments", refereeId)
                .header("Authorization", bearer(referee.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].gameId").value(betaGameId));
    }

    @Test
    void refereeCannotAccessGamesForOrganizationsTheyHaveNotJoined() throws Exception {
        AuthFixture adminOne = registerAdmin("admin1@example.com", "Club Alpha");
        AuthFixture adminTwo = registerAdmin("admin2@example.com", "Club Beta");
        AuthFixture referee = registerReferee("ref@example.com", adminOne.joinCode());

        mockMvc.perform(post("/api/admin/games")
                .header("Authorization", bearer(adminOne.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-01",
                      "gameTime": "18:30:00",
                      "location": "Alpha Field",
                      "homeTeam": "Lions",
                      "awayTeam": "Tigers"
                    }
                    """))
            .andExpect(status().isOk());

        String betaGameResponse = mockMvc.perform(post("/api/admin/games")
                .header("Authorization", bearer(adminTwo.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameDate": "2026-06-02",
                      "gameTime": "19:00:00",
                      "location": "Beta Field",
                      "homeTeam": "Bears",
                      "awayTeam": "Sharks"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Integer betaGameId = readPathId(betaGameResponse, "id");
        Integer refereeId = readPathId(referee.responseBody(), "user.id");

        mockMvc.perform(get("/api/referees/available-games")
                .header("Authorization", bearer(referee.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$[0].location").value("Alpha Field"));

        mockMvc.perform(post("/api/referees/availability")
                .header("Authorization", bearer(referee.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameId": %d,
                      "isAvailable": true
                    }
                    """.formatted(betaGameId)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/assignments")
                .header("Authorization", bearer(adminTwo.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gameId": %d,
                      "refereeId": %d
                    }
                    """.formatted(betaGameId, refereeId)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void adminViewsOnlyRefereesFromOrganizationsTheyManage() throws Exception {
        AuthFixture adminOne = registerAdmin("admin1@example.com", "Club Alpha");
        AuthFixture adminTwo = registerAdmin("admin2@example.com", "Club Beta");
        AuthFixture sharedReferee = registerReferee("shared@example.com", adminOne.joinCode());
        AuthFixture isolatedReferee = registerReferee("isolated@example.com", adminTwo.joinCode());

        mockMvc.perform(post("/api/auth/join-organization")
                .header("Authorization", bearer(sharedReferee.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "joinCode": "%s"
                    }
                    """.formatted(adminTwo.joinCode())))
            .andExpect(status().isOk());

        Integer sharedRefereeId = readPathId(sharedReferee.responseBody(), "user.id");
        Integer isolatedRefereeId = readPathId(isolatedReferee.responseBody(), "user.id");

        mockMvc.perform(get("/api/admin/referees")
                .header("Authorization", bearer(adminOne.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(sharedRefereeId));

        mockMvc.perform(get("/api/admin/referees")
                .header("Authorization", bearer(adminTwo.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        mockMvc.perform(get("/api/admin/referees/{id}", isolatedRefereeId)
                .header("Authorization", bearer(adminOne.accessToken())))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/referees/{id}", sharedRefereeId)
                .header("Authorization", bearer(adminTwo.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.referee.id").value(sharedRefereeId));
    }

    @Test
    void refereeCannotReadAnotherRefereesData() throws Exception {
        AuthFixture admin = registerAdmin("admin@example.com", "Club Alpha");
        AuthFixture refereeOne = registerReferee("ref1@example.com", admin.joinCode());
        AuthFixture refereeTwo = registerReferee("ref2@example.com", admin.joinCode());

        Integer refereeTwoId = readPathId(refereeTwo.responseBody(), "user.id");

        mockMvc.perform(get("/api/referees/{id}/availability", refereeTwoId)
                .header("Authorization", bearer(refereeOne.accessToken())))
            .andExpect(status().isForbidden());
    }

    private AuthFixture registerAdmin(String email, String organizationName) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "password123",
                      "fullName": "Admin User",
                      "phoneNumber": "555-1000",
                      "role": "ADMIN",
                      "organizationName": "%s"
                    }
                    """.formatted(email, organizationName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.memberships[0].organizationName").value(organizationName))
            .andExpect(jsonPath("$.memberships[0].joinCode").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return new AuthFixture(
            response,
            readPath(response, "accessToken").asText(),
            readPath(response, "refreshToken").asText(),
            readPath(response, "memberships[0].joinCode").asText(),
            readPathId(response, "memberships[0].organizationId")
        );
    }

    private AuthFixture registerReferee(String email, String joinCode) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "password123",
                      "fullName": "Referee User",
                      "phoneNumber": "555-2000",
                      "role": "REFEREE",
                      "joinCode": "%s"
                    }
                    """.formatted(email, joinCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.memberships[0].joinCode").value(joinCode))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Integer organizationId = organizationRepository.findByJoinCode(joinCode)
            .map(Organization::getId)
            .orElseThrow();

        return new AuthFixture(
            response,
            readPath(response, "accessToken").asText(),
            readPath(response, "refreshToken").asText(),
            joinCode,
            organizationId
        );
    }

    private JsonNode readPath(String json, String path) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        for (String part : path.split("\\.")) {
            if (part.contains("[")) {
                String field = part.substring(0, part.indexOf('['));
                int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));
                node = node.path(field).path(index);
            } else {
                node = node.path(part);
            }
        }
        return node;
    }

    private Integer readPathId(String json, String path) throws Exception {
        return readPath(json, path).asInt();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record AuthFixture(
        String responseBody,
        String accessToken,
        String refreshToken,
        String joinCode,
        Integer organizationId
    ) {
    }
}
