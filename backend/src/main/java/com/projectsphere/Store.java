package com.projectsphere;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Store {
  final JdbcTemplate db;
  final ObjectMapper json;

  Store(JdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  Map<String, Object> parse(String data) {
    try {
      return json.readValue(data, new TypeReference<>() {});
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  String stringify(Object data) {
    try {
      return json.writeValueAsString(data);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<String, List<Map<String, Object>>> state() {
    Map<String, List<Map<String, Object>>> s = new LinkedHashMap<>();
    for (String k : Policy.KINDS) s.put(k, new ArrayList<>());
    db.query(
        "SELECT kind,payload,version FROM entity ORDER BY created_at,id",
        rs -> {
          Map<String, Object> d = parse(rs.getString("payload"));
          d.put("version", rs.getInt("version"));
          s.computeIfAbsent(rs.getString("kind"), k -> new ArrayList<>()).add(d);
        });
    return s;
  }

  // Load only authorization relationships and the explicitly requested modules.
  Map<String,List<Map<String,Object>>> context(String... extras) {
    var names = new LinkedHashSet<>(List.of("users", "roles", "teams", "projects"));
    names.addAll(Arrays.asList(extras));
    return loadKinds(names);
  }

  Map<String,List<Map<String,Object>>> loadKinds(Collection<String> names) {
    Map<String,List<Map<String,Object>>> result = new LinkedHashMap<>();
    for (String kind : Policy.KINDS) result.put(kind, new ArrayList<>());
    if (names.isEmpty()) return result;
    String slots = String.join(",", Collections.nCopies(names.size(), "?"));
    db.query("SELECT kind,payload,version FROM entity WHERE kind IN ("+slots+") ORDER BY created_at,id",
      rs -> { var item=parse(rs.getString("payload")); item.put("version",rs.getInt("version"));
        result.get(rs.getString("kind")).add(item); }, names.toArray());
    return result;
  }

  Map<String,List<Map<String,Object>>> writeContext(String kind, Map<String,Object> body) {
    var extra = new LinkedHashSet<String>(); extra.add(kind);
    if (Set.of("users","teams","projects","tasks","bugs","sprints").contains(kind)) {
      extra.add("tasks"); extra.add("sprints");
    }
    if (Set.of("enrollments","assessments").contains(kind)) extra.add("courses");
    if (Set.of("comments","documents").contains(kind)) {
      String parent=Policy.str(body,"entityKind");
      if (Policy.KINDS.contains(parent)) extra.add(parent);
    }
    return context(extra.toArray(String[]::new));
  }

  boolean hasReferences(String id) {
    return db.query("SELECT payload FROM entity WHERE id<>? AND kind<>'audit' AND payload LIKE ?",
      rs -> {
        while (rs.next()) {
          var item=parse(rs.getString(1));
          for (String key:List.of("projectId","ownerId","roleId","reportingTo","sprintId","courseId","assessmentId","entityId","leadId","teamIds","memberIds","managerIds","dependencies")) {
            Object value=item.get(key);
            if (id.equals(value) || value instanceof List<?> list && list.contains(id)) return true;
          }
        }
        return false;
      },id,"%"+id+"%");
  }

  void lock() {
    db.queryForObject("SELECT id FROM workspace_lock WHERE id=1 FOR UPDATE", Integer.class);
  }

  Map<String, Object> find(String kind, String id) {
    return db
        .query(
            "SELECT payload,version FROM entity WHERE id=? AND kind=?",
            (rs, n) -> {
              var d = parse(rs.getString(1));
              d.put("version", rs.getInt(2));
              return d;
            },
            id,
            kind)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ApiError(404, "Record not found"));
  }

  void save(String kind, Map<String, Object> d, boolean create) {
    if (create)
      db.update(
          "INSERT INTO entity(id,kind,title,status,owner_id,project_id,payload,version)"
              + " VALUES(?,?,?,?,?,?,?,0)",
          d.get("id"),
          kind,
          d.get("title"),
          d.get("status"),
          d.get("ownerId"),
          d.get("projectId"),
          stringify(d));
    else {
      int n =
          db.update(
              "UPDATE entity SET"
                  + " title=?,status=?,owner_id=?,project_id=?,payload=?,version=version+1,updated_at=CURRENT_TIMESTAMP"
                  + " WHERE id=? AND kind=? AND version=?",
              d.get("title"),
              d.get("status"),
              d.get("ownerId"),
              d.get("projectId"),
              stringify(d),
              d.get("id"),
              kind,
              d.get("version"));
      if (n != 1) throw new ApiError(409, "This record changed. Reload and try again.");
    }
  }

  void audit(String actor, String action, String kind, String id) {
    Map<String, Object> d = new LinkedHashMap<>();
    d.put("id", UUID.randomUUID().toString());
    d.put("title", action + " " + kind);
    d.put("status", "Recorded");
    d.put("ownerId", actor);
    d.put("entityId", id);
    d.put("entityKind", kind);
    d.put("createdAt", java.time.Instant.now().toString());
    d.put("version", 0);
    save("audit", d, true);
  }
}
