package com.projectsphere;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "app.bootstrap-email=vineeth@example.test",
      "app.bootstrap-password=test-only-password-12345"
    })
@org.springframework.context.annotation.Import(TestFixtures.class)
@AutoConfigureMockMvc
@Transactional
class WorkspaceTests {
  @Autowired Store store;
  @Autowired Policy policy;
  @Autowired WorkspaceService service;
  @Autowired MockMvc mvc;

  Map<String, Object> user(String id) {
    return store.find("users", id);
  }

  @Test
  void employeeSeesOnlyOwnTasks() {
    var s = store.state();
    var u = user("u3");
    assertTrue(policy.scope(s, u, "tasks", store.find("tasks", "t1")));
    assertFalse(policy.scope(s, u, "tasks", store.find("tasks", "t2")));
  }

  @Test
  void leadSeesReportsButNotOtherProjects() {
    var s = store.state();
    var u = user("u2");
    assertTrue(policy.scope(s, u, "tasks", store.find("tasks", "t2")));
    assertFalse(policy.scope(s, u, "tasks", store.find("tasks", "t9")));
  }

  @Test
  void overlappingTeamMembershipIsDeduplicated() {
    var s = store.state();
    assertEquals(6, policy.members(s, store.find("projects", "p1")).size());
  }

  @Test
  void reportingCyclesAreRejected() {
    assertThrows(
        ApiError.class,
        () -> service.save("u1", "users", "u2", Map.of("reportingTo", "u3", "version", 0)));
  }

  @Test
  void cannotAssignOutsideProject() {
    var error =
        assertThrows(
            ApiError.class,
            () -> service.save("u1", "tasks", "t7", Map.of("ownerId", "u6", "version", 0)));
    assertEquals(400, error.status);
  }

  @Test
  void employeeCannotApproveOrReassign() {
    assertThrows(
        ApiError.class,
        () -> service.save("u3", "tasks", "t1", Map.of("status", "Done", "version", 0)));
    assertThrows(
        ApiError.class,
        () -> service.save("u3", "tasks", "t1", Map.of("ownerId", "u4", "version", 0)));
  }

  @Test
  void employeeCanSubmitForReview() {
    var t =
        service.save(
            "u3", "tasks", "t1", Map.of("status", "Review", "actualHours", 4, "version", 0));
    assertEquals("Review", t.get("status"));
    assertEquals(1, t.get("version"));
  }

  @Test
  void staleUpdatesAreRejected() {
    service.save("u3", "tasks", "t1", Map.of("status", "Review", "version", 0));
    var e =
        assertThrows(
            ApiError.class,
            () -> service.save("u3", "tasks", "t1", Map.of("status", "Testing", "version", 0)));
    assertEquals(409, e.status);
  }

  @Test
  void teamRemovalCannotOrphanActiveWork() {
    assertThrows(
        ApiError.class,
        () ->
            service.save(
                "u1", "projects", "p1", Map.of("teamIds", List.of("team2"), "version", 0)));
  }

  @Test
  void disabledBugsCannotBeCreated() {
    assertThrows(
        ApiError.class,
        () ->
            service.save(
                "u1",
                "bugs",
                null,
                Map.of("title", "A bug", "projectId", "p3", "ownerId", "u6", "status", "To do")));
  }

  @Test
  void referencedRoleCannotBeDeleted() {
    assertThrows(ApiError.class, () -> service.delete("u1", "roles", "role-employee", 0));
  }

  @Test
  void missingCsrfIsRejected() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute("authVersion", 0);
    session.setAttribute("userId", "u1");
    mvc.perform(
            post("/api/v1/projects")
                .session(session)
                .contentType("application/json")
                .content("{\"title\":\"Invalid\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void unauthenticatedStateIsRejected() throws Exception {
    mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
  }

  @Test
  void assessmentAnswerKeysAreNotExposedToEmployees() {
    var s = store.state();
    var sanitized = policy.sanitize(s, user("u3"), "assessments", store.find("assessments", "as1"));
    for (Object q : (List<?>) sanitized.get("questions"))
      assertFalse(((Map<?, ?>) q).containsKey("answer"));
  }

  @Test
  void attendancePreventsDuplicateCheckIn() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute("authVersion", 0);
    session.setAttribute("userId", "u3");
    mvc.perform(post("/api/v1/attendance/check-in").session(session).with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(post("/api/v1/attendance/check-in").session(session).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void assessmentIsScoredOnServer() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute("authVersion", 0);
    session.setAttribute("userId", "u3");
    mvc.perform(
            post("/api/v1/assessments/as1/submit")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"answers\":[1,1],\"score\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(100));
  }

  @Test
  @SuppressWarnings("unchecked")
  void employeeCannotCreateAlreadyApprovedTask() {
    var role = store.find("roles", "role-employee");
    var permissions = (Map<String, Object>) role.get("permissions");
    ((Map<String, Object>) permissions.get("tasks")).put("create", true);
    store.save("roles", role, false);
    assertThrows(
        ApiError.class,
        () ->
            service.save(
                "u3",
                "tasks",
                null,
                Map.of(
                    "title",
                    "Self approved",
                    "projectId",
                    "p1",
                    "ownerId",
                    "u3",
                    "status",
                    "Done")));
  }
}
