package com.projectsphere;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class PasswordService {
  final Store store;
  final PasswordEncoder encoder;
  final TransactionTemplate tx;
  final EmailService email;
  PasswordService(Store store, PasswordEncoder encoder, TransactionTemplate tx, EmailService email) {
    this.store=store; this.encoder=encoder; this.tx=tx; this.email=email;
  }
  static String hash(String value) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
  void validate(String password) {
    if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ApiError(400, "Use at least 12 characters and at most 72 UTF-8 bytes for your password.");
  }
  // Called under the database lock; persists across backend restarts and instances.
  boolean rate(String key, int limit) {
    store.db.update("DELETE FROM auth_rate_limit WHERE expires_at < CURRENT_TIMESTAMP");
    var rows=store.db.queryForList("SELECT attempts FROM auth_rate_limit WHERE bucket=?",key);
    if (rows.isEmpty()) {
      store.db.update("INSERT INTO auth_rate_limit(bucket,attempts,expires_at) VALUES(?,1,?)",key,Timestamp.from(Instant.now().plusSeconds(900)));
      return true;
    }
    if (((Number)rows.get(0).get("attempts")).intValue() >= limit) return false;
    store.db.update("UPDATE auth_rate_limit SET attempts=attempts+1 WHERE bucket=?",key); return true;
  }
  void forgot(String address) {
    String normalized=address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    if (normalized.isBlank() || normalized.length()>254) throw new ApiError(400,"Enter a valid email address.");
    byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
    String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String recipient=tx.execute(t -> {
      store.lock();
      // Global cap also bounds the number of distinct email buckets.
      if (!rate("forgot-global",100) || !rate("forgot:"+hash(normalized),5)) return null;
      store.db.update("DELETE FROM password_reset WHERE expires_at < CURRENT_TIMESTAMP");
      var accounts=store.db.queryForList("SELECT user_id,email FROM account WHERE email=?",normalized);
      if (accounts.isEmpty() || !email.enabled()) return null;
      String uid=accounts.get(0).get("user_id").toString();
      if (!"Active".equals(store.find("users",uid).get("status"))) return null;
      store.db.update("DELETE FROM password_reset WHERE user_id=?",uid);
      store.db.update("INSERT INTO password_reset(token_hash,user_id,expires_at) VALUES(?,?,?)",hash(token),uid,Timestamp.from(Instant.now().plusSeconds(900)));
      return normalized;
    });
    if (recipient != null) {
      try { email.reset(recipient, token); }
      catch (org.springframework.core.task.TaskRejectedException e) {
        store.db.update("DELETE FROM password_reset WHERE token_hash=?",hash(token));
      }
    }
  }
  void update(String uid,String password) {
    store.db.update("UPDATE account SET password_hash=?,auth_version=auth_version+1,failed_attempts=0,locked_until=NULL WHERE user_id=?",encoder.encode(password),uid);
    store.db.update("DELETE FROM password_reset WHERE user_id=?",uid);
    store.audit(uid,"Password changed","users",uid);
  }
  void reset(String token,String password) {
    validate(password);
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw new ApiError(400,"Reset link is invalid or expired. Request a new link.");
    String recipient=tx.execute(t -> {
      store.lock();
      var rows=store.db.queryForList("SELECT a.user_id,a.email FROM password_reset r JOIN account a ON a.user_id=r.user_id WHERE r.token_hash=? AND r.expires_at>CURRENT_TIMESTAMP",hash(token));
      if (rows.isEmpty()) throw new ApiError(400,"Reset link is invalid or expired. Request a new link.");
      var a=rows.get(0); String uid=a.get("user_id").toString();
      if (!"Active".equals(store.find("users",uid).get("status"))) throw new ApiError(400,"Reset link is invalid or expired. Request a new link.");
      update(uid,password); return a.get("email").toString();
    });
    email.changed(recipient);
  }
  void change(String uid,String current,String password) {
    validate(password);
    if (current == null || current.length()>200) throw new ApiError(400,"Enter your current password.");
    String recipient=tx.execute(t -> {
      store.lock();
      if (!rate("change:"+hash(uid),5)) return "RATE";
      var a=store.db.queryForMap("SELECT * FROM account WHERE user_id=?",uid);
      if (!encoder.matches(current,a.get("password_hash").toString())) return null;
      if (encoder.matches(password,a.get("password_hash").toString())) throw new ApiError(400,"Choose a different new password.");
      update(uid,password); return a.get("email").toString();
    });
    if (recipient == null) throw new ApiError(400,"Current password is incorrect.");
    if (recipient.equals("RATE")) throw new ApiError(429,"Too many attempts. Try again in 15 minutes.");
    email.changed(recipient);
  }
}
