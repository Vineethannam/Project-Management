package com.projectsphere;

import static com.projectsphere.Policy.*;

import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {
  final Store store;
  final Policy policy;
  final PasswordEncoder encoder;
  final org.springframework.context.ApplicationEventPublisher events;

  WorkspaceService(Store s, Policy p, PasswordEncoder e, org.springframework.context.ApplicationEventPublisher events) {
    this.events = events;
    store = s;
    policy = p;
    encoder = e;
  }

  Map<String, Object> me(Map<String, List<Map<String, Object>>> s, String uid) {
    var u = get(s, "users", uid);
    if (u == null || !"Active".equals(u.get("status")))
      throw new ApiError(401, "Account inactive. Please sign in.");
    return u;
  }

  void assertTrue(boolean ok, String message) {
    if (!ok) throw new ApiError(400, message);
  }

  void reference(
      Map<String, List<Map<String, Object>>> s, String kind, Object id, boolean required) {
    if (id == null || id.toString().isBlank()) {
      assertTrue(!required, kind + " is required");
      return;
    }
    assertTrue(get(s, kind, id.toString()) != null, "Invalid " + kind + " reference");
  }

  @Transactional
  public Map<String, Object> save(String uid, String kind, String id, Map<String, Object> body) {
    if (!KINDS.contains(kind)
        || Set.of("audit", "reports", "notifications", "attendance", "attempts").contains(kind))
      throw new ApiError(400, "Use the dedicated workflow for this record.");
    store.lock();
    Map<String,Object> contextBody = id == null ? body : new LinkedHashMap<>(store.find(kind,id));
    if (id != null) contextBody.putAll(body);
    var s = store.writeContext(kind, contextBody);
    var u = me(s, uid);
    boolean create = id == null;
    policy.require(s, u, kind, create ? "create" : "update");
    Map<String, Object> old = create ? null : store.find(kind, id);
    if (old != null && !policy.scope(s, u, kind, old)) throw new ApiError(404, "Record not found");
    var d = new LinkedHashMap<String, Object>();
    if (old != null) d.putAll(old);
    d.putAll(body);
    d.remove("password");
    d.remove("score");
    if (old != null && old.containsKey("score")) d.put("score", old.get("score"));
    d.put("id", create ? UUID.randomUUID().toString() : id);
    d.put("version", create ? 0 : body.get("version"));
    d.put("createdBy", create ? uid : old.get("createdBy"));
    d.put("createdAt", create ? Instant.now().toString() : old.get("createdAt"));
    d.putIfAbsent("status", "Active");
    assertTrue(
        !str(d, "title").isBlank() && str(d, "title").length() <= 250,
        "Title must contain 1–250 characters");
    assertTrue(store.stringify(d).length() < 200000, "Record is too large");
    if (Set.of("roles", "settings", "users", "departments").contains(kind) && !policy.org(u))
      throw new ApiError(403, "Organization scope is required.");
    if (kind.equals("users")) {
      reference(s, "roles", d.get("roleId"), true);
      reference(s, "users", d.get("reportingTo"), false);
      assertTrue(!str(d, "id").equals(str(d, "reportingTo")), "A user cannot report to themselves");
      if (!create)
        assertTrue(
            !policy.reports(s, id).contains(str(d, "reportingTo")),
            "This manager would create a reporting cycle");
      assertTrue(
          Set.of("hierarchy", "organization").contains(str(d, "scope")),
          "Invalid visibility scope");
      assertTrue(Set.of("Active", "Inactive").contains(str(d, "status")), "Invalid user status");
      String email = str(d, "email").trim().toLowerCase(Locale.ROOT);
      assertTrue(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Valid email is required");
      d.put("email", email);
      if (!create && id.equals(uid)) {
        assertTrue(
            Objects.equals(d.get("roleId"), old.get("roleId"))
                && Objects.equals(d.get("scope"), old.get("scope"))
                && "Active".equals(d.get("status")),
            "Use another administrator to change your own access");
      }
      if (!create && !"Active".equals(d.get("status")))
        assertTrue(
            s.get("tasks").stream()
                .noneMatch(t -> id.equals(t.get("ownerId")) && !"Done".equals(t.get("status"))),
            "Reassign active tasks before deactivating this user");
    }
    if (kind.equals("roles")) {
      assertTrue(
          !(!create && Objects.equals(u.get("roleId"), id)),
          "Use another administrator to edit your own role");
      validatePermissions(d);
    }
    if (kind.equals("teams")) {
      for (String member : ids(d, "memberIds")) reference(s, "users", member, true);
      reference(s, "users", d.get("leadId"), false);
      assertTrue(
          str(d, "leadId").isBlank() || ids(d, "memberIds").contains(str(d, "leadId")),
          "The team lead must be a team member");
    }
    if (kind.equals("projects")) {
      if (!create && !policy.manages(u, old))
        throw new ApiError(403, "Only project managers can edit project settings");
      for (String team : ids(d, "teamIds")) reference(s, "teams", team, true);
      for (String manager : ids(d, "managerIds")) reference(s, "users", manager, true);
      if (create && !policy.org(u)) {
        var managers = new ArrayList<>(ids(d, "managerIds"));
        if (!managers.contains(uid)) managers.add(uid);
        d.put("managerIds", managers);
      }
      assertTrue(
          Set.of("Planning", "Active", "On hold", "Completed", "Archived")
              .contains(str(d, "status")),
          "Invalid project status");
    }
    if (PROJECT.contains(kind)) {
      reference(s, "projects", d.get("projectId"), true);
      var p = get(s, "projects", str(d, "projectId"));
      assertTrue(!Set.of("Archived", "Completed").contains(str(p, "status")), "Project is closed");
      if (!policy.manages(u, p) && !policy.members(s, p).contains(uid))
        throw new ApiError(403, "Project access required");
      if (Set.of("tasks", "bugs", "sprints", "milestones", "risks", "announcements").contains(kind)
          && create
          && !policy.manages(u, p)) {
        if (!Set.of("tasks", "bugs").contains(kind)
            || !policy.reports(s, uid).contains(str(d, "ownerId")))
          throw new ApiError(403, "You can assign work only to your reporting hierarchy");
      }
      if (kind.equals("bugs"))
        assertTrue(Boolean.TRUE.equals(p.get("bugsEnabled")), "Bugs are disabled for this project");
      if (!str(d, "ownerId").isBlank())
        assertTrue(
            policy.members(s, p).contains(str(d, "ownerId")),
            "Assignee must be an active project member");
    }
    if (Set.of("tasks", "bugs").contains(kind)) {
      reference(s, "users", d.get("ownerId"), true);
      assertTrue(
          List.of("Backlog", "To do", "In progress", "Review", "Testing", "Done")
              .contains(str(d, "status")),
          "Invalid task status");
      reference(s, "sprints", d.get("sprintId"), false);
      if (!str(d, "sprintId").isBlank())
        assertTrue(
            Objects.equals(
                get(s, "sprints", str(d, "sprintId")).get("projectId"), d.get("projectId")),
            "Sprint must belong to the same project");
      var p = get(s, "projects", str(d, "projectId"));
      boolean manager =
          policy.manages(u, p)
              || !str(d, "ownerId").equals(uid)
                  && policy.reports(s, uid).contains(str(d, "ownerId"));
      if (!manager && "Done".equals(d.get("status")))
        throw new ApiError(403, "A manager must approve completion");
      if (!create && !manager) {
        var allowed = Set.of("status", "actualHours", "version");
        for (String key : body.keySet())
          if (!allowed.contains(key) && !Objects.equals(body.get(key), old.get(key)))
            throw new ApiError(403, "You can update only status and actual hours on your task");
        assertTrue(
            !str(d, "status").equals("Done"),
            "Submit for review; a manager must approve completion");
      }
      if (!create && !Objects.equals(d.get("ownerId"), old.get("ownerId")) && !manager)
        throw new ApiError(403, "Reassignment requires management access");
      if ("Done".equals(d.get("status")) && (create || !"Done".equals(old.get("status"))))
        d.put("completedAt", Instant.now().toString());
      if (!"Done".equals(d.get("status"))) d.remove("completedAt");
      for (String dep : ids(d, "dependencies")) {
        reference(s, "tasks", dep, true);
        assertTrue(!dep.equals(d.get("id")), "A task cannot depend on itself");
        assertTrue(
            Objects.equals(get(s, "tasks", dep).get("projectId"), d.get("projectId")),
            "Dependencies must belong to the same project");
        assertTrue(!reaches(s, dep, str(d, "id"), new HashSet<>()), "Dependency cycle detected");
      }
    }
    if (kind.equals("sprints")) {
      assertTrue(
          List.of("Planned", "Active", "Under review", "Completed").contains(str(d, "status")),
          "Invalid sprint status");
      if ("Completed".equals(d.get("status")))
        assertTrue(
            s.get("tasks").stream()
                .noneMatch(
                    t ->
                        Objects.equals(t.get("sprintId"), d.get("id"))
                            && !"Done".equals(t.get("status"))),
            "Move unfinished tasks to the backlog or another sprint first");
    }
    if (PERSONAL.contains(kind)) {
      reference(s, "users", d.get("ownerId"), true);
      if (!policy.org(u) && !policy.reports(s, uid).contains(str(d, "ownerId")))
        throw new ApiError(403, "Employee is outside your reporting hierarchy");
      boolean own = uid.equals(d.get("ownerId"));
      if (kind.equals("reviews") && own && !policy.org(u))
        throw new ApiError(403, "A manager must create your performance review");
      if (kind.equals("assignments") && own && !policy.org(u)) {
        if (create) throw new ApiError(403, "A manager must assign work");
        for (String key : body.keySet())
          if (!Set.of("submission", "status", "version").contains(key)
              && !Objects.equals(body.get(key), old.get(key)))
            throw new ApiError(403, "Only submission fields can be updated");
        assertTrue(
            List.of("In progress", "Submitted").contains(str(d, "status")),
            "Only your manager can evaluate an assignment");
      }
    }
    if (kind.equals("enrollments") && !create && uid.equals(d.get("ownerId")) && !policy.org(u))
      for (String key : body.keySet())
        if (!Set.of("completedLessons", "status", "version").contains(key)
            && !Objects.equals(body.get(key), old.get(key)))
          throw new ApiError(403, "You can update only your learning progress");
    if (kind.equals("enrollments")) {
      reference(s, "courses", d.get("courseId"), true);
      var course = get(s, "courses", str(d, "courseId"));
      int total = ((List<?>) course.getOrDefault("lessons", List.of())).size();
      Set<Integer> done = new HashSet<>();
      for (String n : ids(d, "completedLessons")) {
        int i = Integer.parseInt(n);
        assertTrue(i >= 0 && i < total, "Invalid lesson");
        done.add(i);
      }
      d.put("completedLessons", new ArrayList<>(done));
      d.put(
          "status",
          total > 0 && done.size() == total
              ? "Completed"
              : done.isEmpty() ? "Assigned" : "In progress");
    }
    if (kind.equals("settings")) {
      double sum = 0;
      for (String field :
          List.of(
              "taskWeight",
              "deliveryWeight",
              "sprintWeight",
              "qualityWeight",
              "learningWeight",
              "collaborationWeight")) {
        double value = Double.parseDouble(Objects.toString(d.get(field), "0"));
        assertTrue(
            Double.isFinite(value) && value >= 0 && value <= 100,
            "Weights must be between 0 and 100");
        sum += value;
      }
      assertTrue(Math.abs(sum - 100) < 0.0001, "Weights must total 100%");
    }
    if (kind.equals("courses")) {
      assertTrue(d.get("lessons") instanceof List<?>, "Lessons are required");
      for (Object lesson : (List<?>) d.get("lessons"))
        assertTrue(
            lesson instanceof Map<?, ?> l
                && l.get("title") instanceof String t
                && !t.isBlank()
                && l.get("content") instanceof String,
            "Every lesson needs a title and content");
    }
    if (kind.equals("assessments")) {
      assertTrue(
          d.get("questions") instanceof List<?> l && !l.isEmpty(),
          "At least one question is required");
      for (Object q : (List<?>) d.get("questions")) {
        assertTrue(q instanceof Map<?, ?>, "Invalid question");
        var question = (Map<?, ?>) q;
        assertTrue(
            question.get("prompt") instanceof String prompt && !prompt.isBlank(),
            "Question text is required");
        assertTrue(
            question.get("options") instanceof List<?> opts
                && opts.size() >= 2
                && opts.stream().allMatch(v -> v instanceof String text && !text.isBlank()),
            "Each question needs at least two nonempty options");
        assertTrue(question.get("answer") instanceof Number, "Correct answer is required");
        int answer = ((Number) question.get("answer")).intValue();
        assertTrue(
            answer >= 0 && answer < ((List<?>) question.get("options")).size(),
            "Invalid correct answer");
      }
      int passing = ((Number) d.getOrDefault("passingScore", 70)).intValue();
      assertTrue(passing >= 0 && passing <= 100, "Passing score must be between 0 and 100");
      assertTrue(
          ((Number) d.getOrDefault("maxAttempts", 3)).intValue() > 0,
          "Maximum attempts must be positive");
    }
    if (kind.equals("comments") || kind.equals("documents")) {
      String targetKind = str(d, "entityKind");
      assertTrue(
          KINDS.contains(targetKind)
              && !Set.of("comments", "documents", "audit", "roles", "settings")
                  .contains(targetKind),
          "Choose a supported parent record");
      reference(s, targetKind, d.get("entityId"), true);
      var target = get(s, targetKind, str(d, "entityId"));
      policy.require(s, u, targetKind, "read");
      if (!policy.scope(s, u, targetKind, target))
        throw new ApiError(404, "Parent record not found");
      d.put("projectId", target.get("projectId"));
      d.put("ownerId", create ? uid : old.get("ownerId"));
      if (!create && !policy.org(u) && !uid.equals(old.get("ownerId")))
        throw new ApiError(403, "Only the author can edit this record");
    }
    for (String n :
        List.of("estimatedHours", "actualHours", "storyPoints", "capacity", "budget", "duration"))
      if (d.get(n) != null) {
        double v = Double.parseDouble(d.get(n).toString());
        assertTrue(Double.isFinite(v) && v >= 0 && v <= 1e12, n + " must be non-negative");
      }
    for (String date : List.of("startDate", "dueDate"))
      if (!str(d, date).isBlank()) LocalDate.parse(str(d, date));
    if (!str(d, "startDate").isBlank() && !str(d, "dueDate").isBlank())
      assertTrue(
          !LocalDate.parse(str(d, "dueDate")).isBefore(LocalDate.parse(str(d, "startDate"))),
          "End date cannot be before start date");
    if (!create && (kind.equals("teams") || kind.equals("projects"))) {
      var hypothetical = new LinkedHashMap<>(s);
      var entries = new ArrayList<>(s.get(kind));
      entries.removeIf(e -> id.equals(e.get("id")));
      entries.add(d);
      hypothetical.put(kind, entries);
      for (var task : s.get("tasks"))
        if (!"Done".equals(task.get("status"))) {
          var p = get(hypothetical, "projects", str(task, "projectId"));
          assertTrue(
              p == null || policy.members(hypothetical, p).contains(str(task, "ownerId")),
              "Reassign active tasks before removing project access");
        }
    }
    store.save(kind, d, create);
    if (kind.equals("users")) {
      String pass = Objects.toString(body.get("password"), "");
      if (create || !pass.isBlank()) {
        assertTrue(
            pass.length() >= 12 && pass.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72, "Password must be at least 12 characters and at most 72 UTF-8 bytes");
        if (create)
          store.db.update(
              "INSERT INTO account(user_id,email,password_hash) VALUES(?,?,?)",
              d.get("id"),
              d.get("email"),
              encoder.encode(pass));
        else
          store.db.update(
              "UPDATE account SET email=?,password_hash=?,auth_version=auth_version+1,failed_attempts=0,locked_until=NULL WHERE"
                  + " user_id=?",
              d.get("email"),
              encoder.encode(pass),
              id);
      } else store.db.update("UPDATE account SET email=? WHERE user_id=?", d.get("email"), id);
      if (!create && (!pass.isBlank() || !Objects.equals(old.get("email"), d.get("email"))))
        store.db.update("DELETE FROM password_reset WHERE user_id=?", id);
    }
    store.audit(uid, create ? "Created" : "Updated", kind, str(d, "id"));
    if (Set.of("tasks", "bugs", "assignments", "enrollments").contains(kind)
        && (create || !Objects.equals(d.get("ownerId"), old.get("ownerId"))))
      notifyUser(str(d, "ownerId"), "Assigned: " + str(d, "title"));
    return store.find(kind, str(d, "id"));
  }

  boolean reaches(
      Map<String, List<Map<String, Object>>> s, String from, String target, Set<String> seen) {
    if (from.equals(target)) return true;
    if (!seen.add(from)) return false;
    var item = get(s, "tasks", from);
    return item != null
        && ids(item, "dependencies").stream().anyMatch(d -> reaches(s, d, target, seen));
  }

  void validatePermissions(Map<String, Object> d) {
    assertTrue(d.get("permissions") instanceof Map<?, ?>, "Permissions are required");
    var p = (Map<?, ?>) d.get("permissions");
    for (var e : p.entrySet()) {
      assertTrue(KINDS.contains(e.getKey()), "Unknown submodule");
      assertTrue(e.getValue() instanceof Map<?, ?>, "Invalid permission row");
      for (var a : ((Map<?, ?>) e.getValue()).entrySet())
        assertTrue(
            Set.of("create", "read", "update", "delete").contains(a.getKey())
                && a.getValue() instanceof Boolean,
            "Invalid CRUD permission");
    }
  }

  void notifyUser(String user, String title) {
    if (user.isBlank()) return;
    var n = new LinkedHashMap<String, Object>();
    n.put("id", UUID.randomUUID().toString());
    n.put("title", title);
    n.put("status", "Unread");
    n.put("ownerId", user);
    n.put("createdAt", Instant.now().toString());
    n.put("version", 0);
    store.save("notifications", n, true);
    events.publishEvent(new NotificationEmail.Event(user, title));
  }

  @Transactional
  public void delete(String uid, String kind, String id, int version) {
    store.lock();
    var d = store.find(kind, id);
    var s = store.writeContext(kind, d);
    var u = me(s, uid);
    policy.require(s, u, kind, "delete");
    if (!policy.scope(s, u, kind, d)) throw new ApiError(404, "Record not found");
    if (Set.of("audit", "settings", "attendance", "attempts", "users").contains(kind))
      throw new ApiError(400, "This history must be retained; deactivate users instead");
    if (kind.equals("roles") && !policy.org(u))
      throw new ApiError(403, "Organization scope required");
    if (Set.of("tasks", "bugs").contains(kind)
        && !policy.manages(u, get(s, "projects", str(d, "projectId")))
        && str(d, "ownerId").equals(uid))
      throw new ApiError(403, "A manager must delete assigned work");
    if (store.hasReferences(id)) throw new ApiError(409,
        "This record is referenced. Archive it or remove its relationships first.");
    int n =
        store.db.update(
            "DELETE FROM entity WHERE id=? AND kind=? AND version=?", id, kind, version);
    if (n != 1) throw new ApiError(409, "Record changed; reload first");
    store.audit(uid, "Deleted", kind, id);
  }
}
