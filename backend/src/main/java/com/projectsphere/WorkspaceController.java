package com.projectsphere;

import static com.projectsphere.Policy.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class WorkspaceController {
  final WorkspaceService service;
  final Store store;
  final Policy policy;

  WorkspaceController(WorkspaceService w, Store s, Policy p) {
    service = w;
    store = s;
    policy = p;
  }

  @PostMapping("/attendance/check-in")
  @Transactional
  public Map<String,Object> checkIn(HttpServletRequest req) { return attendance("check-in", req); }

  @PostMapping("/attendance/check-out")
  @Transactional
  public Map<String,Object> checkOut(HttpServletRequest req) { return attendance("check-out", req); }

  @PostMapping("/attendance/break-start")
  @Transactional
  public Map<String,Object> startBreak(HttpServletRequest req) { return attendance("break-start", req); }

  @PostMapping("/attendance/break-end")
  @Transactional
  public Map<String,Object> endBreak(HttpServletRequest req) { return attendance("break-end", req); }

  @Transactional
  public Map<String, Object> attendance(String action, HttpServletRequest req) {
    String uid = AuthController.userId(req);
    store.lock();
    var s = store.context("attendance");
    var me = service.me(s, uid);
    policy.require(s, me, "attendance", action.equals("check-in") ? "create" : "update");
    var current =
        s.get("attendance").stream()
            .filter(d -> uid.equals(d.get("ownerId")) && !d.containsKey("checkOut"))
            .findFirst();
    Instant now = Instant.now();
    Map<String, Object> d;
    if (action.equals("check-in")) {
      service.assertTrue(current.isEmpty(), "You are already checked in");
      d = new LinkedHashMap<>();
      d.put("id", UUID.randomUUID().toString());
      d.put("title", LocalDate.now(ZoneOffset.UTC).toString());
      d.put("status", "Present");
      d.put("ownerId", uid);
      d.put("checkIn", now.toString());
      d.put("breakSeconds", 0);
      d.put("version", 0);
      store.save("attendance", d, true);
    } else {
      d = current.orElseThrow(() -> new ApiError(400, "Check in first"));
      switch (action) {
        case "break-start" -> {
          service.assertTrue(!d.containsKey("breakStart"), "Break already started");
          d.put("breakStart", now.toString());
          d.put("status", "On break");
        }
        case "break-end" -> {
          service.assertTrue(d.containsKey("breakStart"), "No active break");
          finishBreak(d, now);
          d.put("status", "Present");
        }
        case "check-out" -> {
          if (d.containsKey("breakStart")) finishBreak(d, now);
          d.put("checkOut", now.toString());
          long secs =
              Duration.between(Instant.parse(str(d, "checkIn")), now).getSeconds()
                  - ((Number) d.getOrDefault("breakSeconds", 0)).longValue();
          d.put("workingHours", Math.round(secs / 36.0) / 100.0);
          d.put("status", "Checked out");
        }
        default -> throw new ApiError(400, "Unknown attendance action");
      }
      store.save("attendance", d, false);
    }
    store.audit(uid, action, "attendance", str(d, "id"));
    return store.find("attendance", str(d, "id"));
  }

  void finishBreak(Map<String, Object> d, Instant now) {
    long sec = Duration.between(Instant.parse(str(d, "breakStart")), now).getSeconds();
    d.put("breakSeconds", ((Number) d.getOrDefault("breakSeconds", 0)).longValue() + sec);
    d.remove("breakStart");
  }

  @PostMapping("/assessments/{id}/submit")
  @Transactional
  public Map<String, Object> assess(
      @PathVariable String id, @RequestBody Map<String, Object> body, HttpServletRequest req) {
    store.lock();
    String uid = AuthController.userId(req);
    var s = store.context("assessments", "attempts");
    var me = service.me(s, uid);
    policy.require(s, me, "attempts", "create");
    policy.require(s, me, "assessments", "read");
    var a = store.find("assessments", id);
    if (!policy.scope(s, me, "assessments", a)) throw new ApiError(404, "Assessment not found");
    service.assertTrue("Published".equals(a.get("status")), "Assessment is not available");
    long attempts =
        s.get("attempts").stream()
            .filter(d -> uid.equals(d.get("ownerId")) && id.equals(d.get("assessmentId")))
            .count();
    service.assertTrue(
        attempts < ((Number) a.getOrDefault("maxAttempts", 3)).intValue(),
        "Maximum attempts reached");
    List<?> questions = (List<?>) a.get("questions");
    List<?> answers = (List<?>) body.get("answers");
    service.assertTrue(
        answers != null && answers.size() == questions.size(), "Answer every question");
    int correct = 0;
    for (int i = 0; i < questions.size(); i++) {
      var q = (Map<?, ?>) questions.get(i);
      if (Objects.toString(q.get("answer")).equals(Objects.toString(answers.get(i)))) correct++;
    }
    int score = (int) Math.round(correct * 100.0 / questions.size());
    var d = new LinkedHashMap<String, Object>();
    d.put("id", UUID.randomUUID().toString());
    d.put("title", a.get("title"));
    d.put(
        "status",
        score >= ((Number) a.getOrDefault("passingScore", 70)).intValue() ? "Passed" : "Failed");
    d.put("ownerId", uid);
    d.put("assessmentId", id);
    d.put("score", score);
    d.put("answers", answers);
    d.put("attempt", attempts + 1);
    d.put("createdAt", Instant.now().toString());
    d.put("version", 0);
    store.save("attempts", d, true);
    store.audit(uid, "Assessment submitted", "attempts", str(d, "id"));
    service.notifyUser(uid, "Assessment result: " + score + "%");
    return d;
  }

  @PostMapping("/assignments/{id}/evaluate")
  @Transactional
  public Map<String, Object> evaluate(
      @PathVariable String id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
    store.lock();
    String uid = AuthController.userId(req);
    var s = store.context("assignments");
    var me = service.me(s, uid);
    policy.require(s, me, "assignments", "update");
    var a = store.find("assignments", id);
    if (!policy.scope(s, me, "assignments", a) || uid.equals(a.get("ownerId")))
      throw new ApiError(403, "A manager must evaluate the submission");
    service.assertTrue("Submitted".equals(a.get("status")), "Assignment has not been submitted");
    int score = ((Number) b.get("score")).intValue();
    service.assertTrue(score >= 0 && score <= 100, "Score must be between 0 and 100");
    a.put("score", score);
    a.put("feedback", str(b, "feedback"));
    a.put("status", "Evaluated");
    store.save("assignments", a, false);
    store.audit(uid, "Evaluated", "assignments", id);
    service.notifyUser(str(a, "ownerId"), "Assignment evaluated: " + str(a, "title") + " (" + score + "%)");
    return store.find("assignments", id);
  }

  @PostMapping("/assignments/{id}/submit")
  @Transactional
  public Map<String,Object> submitAssignment(@PathVariable String id,@RequestBody Map<String,Object> body,HttpServletRequest req) {
    store.lock();String uid=AuthController.userId(req);var item=store.find("assignments",id);
    if(!uid.equals(item.get("ownerId")))throw new ApiError(403,"Only the assignee can submit work");
    String submission=str(body,"submission");service.assertTrue(!submission.isBlank(),"Submission is required");
    var update=new LinkedHashMap<String,Object>();update.put("version",body.get("version"));update.put("submission",submission);update.put("status","Submitted");
    return service.save(uid,"assignments",id,update);
  }

  @PostMapping("/enrollments/{id}/lessons/{lesson}/complete")
  @Transactional
  public Map<String,Object> completeLesson(@PathVariable String id,@PathVariable int lesson,@RequestBody Map<String,Object> body,HttpServletRequest req) {
    store.lock();var item=store.find("enrollments",id);var completed=new ArrayList<>(ids(item,"completedLessons"));
    if(!completed.contains(""+lesson))completed.add(""+lesson);
    var update=new LinkedHashMap<String,Object>();update.put("version",body.get("version"));update.put("completedLessons",completed);
    return service.save(AuthController.userId(req),"enrollments",id,update);
  }

  @PostMapping("/notifications/{id}/read")
  @Transactional
  public Map<String, Object> read(@PathVariable String id, HttpServletRequest req) {
    store.lock();
    String uid = AuthController.userId(req);
    var s = store.context("notifications");
    var me = service.me(s, uid);
    policy.require(s, me, "notifications", "update");
    var d = store.find("notifications", id);
    if (!uid.equals(d.get("ownerId"))) throw new ApiError(404, "Notification not found");
    d.put("status", "Read");
    store.save("notifications", d, false);
    return d;
  }
}
