package com.projectsphere;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class Policy {
  public static final List<String> KINDS =
      List.of(
          "projects",
          "tasks",
          "bugs",
          "sprints",
          "milestones",
          "risks",
          "standups",
          "retrospectives",
          "users",
          "teams",
          "departments",
          "roles",
          "attendance",
          "assignments",
          "courses",
          "enrollments",
          "assessments",
          "attempts",
          "skills",
          "reviews",
          "announcements",
          "comments",
          "documents",
          "notifications",
          "audit",
          "settings",
          "reports");
  static final Set<String> PERSONAL =
      Set.of("attendance", "assignments", "enrollments", "attempts", "skills", "reviews");
  static final Set<String> PROJECT =
      Set.of(
          "tasks",
          "bugs",
          "sprints",
          "milestones",
          "risks",
          "standups",
          "retrospectives",
          "announcements");

  static String str(Map<String, Object> d, String k) {
    return Objects.toString(d.get(k), "");
  }

  @SuppressWarnings("unchecked")
  static List<String> ids(Map<String, Object> d, String k) {
    Object v = d.get(k);
    return v instanceof List<?> l ? l.stream().map(Object::toString).toList() : List.of();
  }

  static Map<String, Object> get(Map<String, List<Map<String, Object>>> s, String k, String id) {
    return s.getOrDefault(k, List.of()).stream()
        .filter(d -> id.equals(d.get("id")))
        .findFirst()
        .orElse(null);
  }

  boolean org(Map<String, Object> me) {
    return "organization".equals(me.get("scope"));
  }

  @SuppressWarnings("unchecked")
  boolean can(
      Map<String, List<Map<String, Object>>> s, Map<String, Object> me, String k, String action) {
    var role = get(s, "roles", str(me, "roleId"));
    if (role == null || !"Active".equals(role.get("status"))) return false;
    Object p = ((Map<String, Object>) role.getOrDefault("permissions", Map.of())).get(k);
    return p instanceof Map<?, ?> m && Boolean.TRUE.equals(m.get(action));
  }

  void require(
      Map<String, List<Map<String, Object>>> s, Map<String, Object> me, String k, String a) {
    if (!can(s, me, k, a)) throw new ApiError(403, "Your role does not allow this action.");
  }

  Set<String> reports(Map<String, List<Map<String, Object>>> s, String user) {
    Set<String> r = new HashSet<>();
    r.add(user);
    int size = -1;
    while (size != r.size()) {
      size = r.size();
      for (var u : s.get("users")) if (r.contains(u.get("reportingTo"))) r.add(str(u, "id"));
    }
    return r;
  }

  Set<String> members(Map<String, List<Map<String, Object>>> s, Map<String, Object> p) {
    Set<String> r = new HashSet<>(ids(p, "managerIds"));
    for (var t : s.get("teams"))
      if ("Active".equals(t.get("status")) && ids(p, "teamIds").contains(t.get("id")))
        r.addAll(ids(t, "memberIds"));
    r.removeIf(
        id -> {
          var u = get(s, "users", id);
          return u == null || !"Active".equals(u.get("status"));
        });
    return r;
  }

  boolean manages(Map<String, Object> me, Map<String, Object> p) {
    return org(me) || (p != null && ids(p, "managerIds").contains(me.get("id")));
  }

  boolean scope(
      Map<String, List<Map<String, Object>>> s,
      Map<String, Object> me,
      String k,
      Map<String, Object> d) {
    if (org(me)) return true;
    String uid = str(me, "id");
    Set<String> tree = reports(s, uid);
    if (k.equals("roles")) return Objects.equals(d.get("id"), me.get("roleId"));
    if (k.equals("notifications")) return uid.equals(d.get("ownerId"));
    if (k.equals("users"))
      return tree.contains(d.get("id"))
          || s.get("projects").stream()
              .anyMatch(p -> members(s, p).contains(uid) && members(s, p).contains(d.get("id")));
    if (k.equals("projects")) return members(s, d).contains(uid);
    if (k.equals("teams"))
      return ids(d, "memberIds").contains(uid)
          || s.get("projects").stream()
              .anyMatch(p -> manages(me, p) && ids(p, "teamIds").contains(d.get("id")));
    if (Set.of("audit", "settings").contains(k)) return false;
    Map<String, Object> p = get(s, "projects", str(d, "projectId"));
    if (p != null && !members(s, p).contains(uid)) return false;
    if (Set.of("tasks", "bugs").contains(k))
      return manages(me, p) || tree.contains(d.get("ownerId"));
    if (PERSONAL.contains(k))
      return tree.contains(d.get("ownerId"))
          || (k.equals("assignments") && uid.equals(d.get("createdBy")));
    if (k.equals("comments") || k.equals("documents")) {
      String targetKind = str(d, "entityKind");
      if (!targetKind.isBlank()) {
        if (!KINDS.contains(targetKind) || Set.of("comments", "documents").contains(targetKind))
          return false;
        var target = get(s, targetKind, str(d, "entityId"));
        return target != null && can(s, me, targetKind, "read") && scope(s, me, targetKind, target);
      }
      return p != null || uid.equals(d.get("ownerId"));
    }
    return true;
  }

  Map<String, Object> sanitize(
      Map<String, List<Map<String, Object>>> s,
      Map<String, Object> me,
      String k,
      Map<String, Object> d) {
    var copy = new LinkedHashMap<>(d);
    copy.remove("password");
    if (k.equals("users") && !org(me)) {
      copy.remove("scope");
      copy.remove("email");
    }
    if (k.equals("assessments") && !can(s, me, k, "update")) {
      Object qs = copy.get("questions");
      if (qs instanceof List<?> list)
        copy.put(
            "questions",
            list.stream()
                .map(
                    q -> {
                      var m = new LinkedHashMap<>((Map<?, ?>) q);
                      m.remove("answer");
                      return m;
                    })
                .toList());
    }
    return copy;
  }
}
