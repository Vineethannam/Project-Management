package com.projectsphere;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.core.annotation.Order(0)
class Bootstrap implements ApplicationRunner {
  final Store store;
  final PasswordEncoder encoder;

  @Value("${app.bootstrap-email:}")
  String email;

  @Value("${app.bootstrap-password:}")
  String password;

  Bootstrap(Store s, PasswordEncoder e) {
    store = s;
    encoder = e;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    store.lock();
    if (store.db.queryForObject("SELECT COUNT(*) FROM entity", Long.class) > 0) return;
    if (password.length() < 12)
      throw new IllegalStateException(
          "Set BOOTSTRAP_PASSWORD to at least 12 characters before first startup.");
    var seed =
        store.json.readValue(
            getClass().getResourceAsStream("/seed.json"),
            new TypeReference<Map<String, List<Map<String, Object>>>>() {});
    for (var entry : seed.entrySet())
      for (var d : entry.getValue())
        if (entry.getKey().equals("roles")
            || entry.getKey().equals("settings")
            || (entry.getKey().equals("users") && d.get("id").equals("u1"))) {
          if (entry.getKey().equals("users")) {
            if (email.isBlank()) throw new IllegalStateException("Set BOOTSTRAP_EMAIL");
            d.put("email", email.toLowerCase(Locale.ROOT));
            d.put("title", "Workspace administrator");
          }
          store.save(entry.getKey(), d, true);
          if (entry.getKey().equals("users"))
            store.db.update(
                "INSERT INTO account(user_id,email,password_hash) VALUES(?,?,?)",
                d.get("id"),
                d.get("email"),
                encoder.encode(
                    d.get("id").equals("u1")
                        ? password
                        : UUID.randomUUID().toString() + UUID.randomUUID()));
        }
  }
}
