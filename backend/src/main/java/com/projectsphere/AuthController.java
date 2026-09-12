package com.projectsphere;

import jakarta.servlet.http.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  final Store store;
  final PasswordEncoder encoder;
  final TransactionTemplate tx;
  final String dummyHash;
  final PasswordService passwords;
  final ReadService reads;

  AuthController(Store s, PasswordEncoder e, TransactionTemplate tx, PasswordService passwords, ReadService reads) {
    this.reads = reads;
    this.passwords = passwords;
    store = s;
    encoder = e;
    this.tx = tx;
    dummyHash = e.encode(UUID.randomUUID().toString());
  }

  @GetMapping("/csrf")
  public Map<String, Object> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/login")
  public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletRequest req) {
    String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
    String password = body.getOrDefault("password", "");
    if (email.length() > 254 || password.length() > 200)
      throw new ApiError(400, "Invalid credentials");
    Integer[] version = new Integer[1];
    String uid =
        tx.execute(
            t -> {
              store.lock();
              var accounts = store.db.queryForList("SELECT * FROM account WHERE email=?", email);
              if (accounts.isEmpty()) {
                encoder.matches(password, dummyHash);
                return null;
              }
              var a = accounts.get(0);
              Object raw = a.get("locked_until");
              Instant until =
                  raw instanceof java.time.OffsetDateTime v
                      ? v.toInstant()
                      : raw instanceof Timestamp v ? v.toInstant() : null;
              if (until != null && until.isAfter(Instant.now())) return null;
              String id = a.get("user_id").toString();
              var user = store.find("users", id);
              if (!encoder.matches(password, a.get("password_hash").toString())
                  || !"Active".equals(user.get("status"))) {
                int n = ((Number) a.get("failed_attempts")).intValue() + 1;
                store.db.update(
                    "UPDATE account SET failed_attempts=?,locked_until=? WHERE user_id=?",
                    n,
                    n >= 5 ? Timestamp.from(Instant.now().plusSeconds(900)) : null,
                    id);
                return null;
              }
              store.db.update(
                  "UPDATE account SET failed_attempts=0,locked_until=NULL WHERE user_id=?", id);
              store.audit(id, "Login", "users", id);
              version[0] = ((Number)a.get("auth_version")).intValue();
              return id;
            });
    if (uid == null) throw new ApiError(401, "Invalid credentials or account temporarily locked.");
    var session = req.getSession(true);
    req.changeSessionId();
    session.setAttribute("userId", uid);
    session.setAttribute("authVersion", version[0]);
    return Map.of("user", store.find("users", uid));
  }

  static String userId(HttpServletRequest req) {
    var s = req.getSession(false);
    if (s == null || s.getAttribute("userId") == null) throw new ApiError(401, "Please sign in.");
    return s.getAttribute("userId").toString();
  }

  @PostMapping("/logout")
  public Map<String, Boolean> logout(HttpServletRequest req) {
    String id = userId(req);
    store.audit(id, "Logout", "users", id);
    req.getSession(false).invalidate();
    return Map.of("success", true);
  }

  @PostMapping("/forgot-password")
  public Map<String, String> forgot(@RequestBody Map<String,String> body) {
    passwords.forgot(body.get("email"));
    return Map.of("message","If an active account matches that email, a reset link will be sent. Check your inbox and spam folder.");
  }

  @PostMapping("/reset-password")
  public Map<String, String> reset(@RequestBody Map<String,String> body, HttpServletRequest req) {
    passwords.reset(body.get("token"),body.get("newPassword"));
    if (req.getSession(false)!=null) req.getSession(false).invalidate();
    return Map.of("message","Password reset. Sign in with your new password.");
  }

  @PostMapping("/change-password")
  public Map<String, String> change(@RequestBody Map<String,String> body,HttpServletRequest req) {
    passwords.change(userId(req),body.get("currentPassword"),body.get("newPassword"));
    req.getSession(false).invalidate();
    return Map.of("message","Password changed. Sign in again with your new password.");
  }

  @GetMapping("/session")
  public Map<String,Object> session(HttpServletRequest req) { return reads.session(userId(req)); }

  @GetMapping("/me")
  public Map<String, Object> me(HttpServletRequest req) {
    var u = store.find("users", userId(req));
    if (!"Active".equals(u.get("status"))) throw new ApiError(401, "Account inactive");
    return u;
  }
}
