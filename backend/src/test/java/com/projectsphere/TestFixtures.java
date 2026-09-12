package com.projectsphere;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@TestConfiguration
class TestFixtures {
 @Bean @Order(100)
 ApplicationRunner fixtureLoader(Store store,PasswordEncoder encoder,TransactionTemplate tx) {
  return args -> {
   var fixtures=store.json.readValue(getClass().getResourceAsStream("/seed-fixtures.json"),new TypeReference<Map<String,List<Map<String,Object>>>>(){});
   tx.executeWithoutResult(t -> {
    store.lock();
    for(var entry:fixtures.entrySet()) for(var item:entry.getValue()) {
     String id=item.get("id").toString();
     if(store.db.queryForObject("SELECT COUNT(*) FROM entity WHERE id=?",Integer.class,id)>0) continue;
     store.save(entry.getKey(),item,true);
     if(entry.getKey().equals("users")) store.db.update("INSERT INTO account(user_id,email,password_hash) VALUES(?,?,?)",id,item.get("email"),encoder.encode(UUID.randomUUID().toString()));
    }
   });
  };
 }
}
