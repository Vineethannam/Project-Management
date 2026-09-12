package com.projectsphere;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:passwordtests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","app.bootstrap-email=vineeth@example.test","app.bootstrap-password=test-only-password-12345"})
@org.springframework.context.annotation.Import(TestFixtures.class)
@AutoConfigureMockMvc
@Transactional
class PasswordTests {
 @Autowired PasswordService passwords;
 @Autowired Store store;
 @Autowired MockMvc mvc;
 @Autowired PasswordEncoder encoder;
 @MockitoBean EmailService email;
 static final String TOKEN="a".repeat(43);
 void token(Instant expiry){store.db.update("INSERT INTO password_reset(token_hash,user_id,expires_at) VALUES(?,?,?)",PasswordService.hash(TOKEN),"u1",Timestamp.from(expiry));}
 MockHttpSession session(){var s=new MockHttpSession();s.setAttribute("userId","u1");s.setAttribute("authVersion",0);return s;}
 @Test void resetIsSingleUseAndRevokesSessions() throws Exception {
  var old=session();token(Instant.now().plusSeconds(900));passwords.reset(TOKEN,"new-secure-password-123");
  assertTrue(encoder.matches("new-secure-password-123",store.db.queryForObject("SELECT password_hash FROM account WHERE user_id='u1'",String.class)));
  assertThrows(ApiError.class,()->passwords.reset(TOKEN,"another-secure-password"));
  mvc.perform(get("/api/v1/tasks").session(old)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"vineeth@example.test\",\"password\":\"new-secure-password-123\"}")).andExpect(status().isOk());
 }
 @Test void expiredTokenCannotChangePassword(){token(Instant.now().minusSeconds(10));assertThrows(ApiError.class,()->passwords.reset(TOKEN,"new-secure-password-123"));}
 @Test void changeChecksCurrentPasswordAndLimitsAttempts(){for(int i=0;i<5;i++)assertEquals(400,assertThrows(ApiError.class,()->passwords.change("u1","wrong","new-secure-password-123")).status);assertEquals(429,assertThrows(ApiError.class,()->passwords.change("u1","wrong","new-secure-password-123")).status);}
 @Test void successfulChangeRevokesTokensAndSession() throws Exception {token(Instant.now().plusSeconds(900));var s=session();mvc.perform(post("/api/v1/auth/change-password").session(s).with(csrf()).contentType("application/json").content("{\"currentPassword\":\"test-only-password-12345\",\"newPassword\":\"new-secure-password-123\"}")).andExpect(status().isOk());assertTrue(s.isInvalid());assertEquals(0,store.db.queryForObject("SELECT COUNT(*) FROM password_reset",Integer.class));}
 @Test void forgotIsGenericAndRateLimited() throws Exception {
  when(email.enabled()).thenReturn(true);
  for(int i=0;i<6;i++)passwords.forgot("vineeth@example.test");
  verify(email,times(5)).reset(eq("vineeth@example.test"),anyString());
  String a=mvc.perform(post("/api/v1/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\"missing@example.test\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String b=mvc.perform(post("/api/v1/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\"vineeth@example.test\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertEquals(a,b);
 }
 @Test void recoveryRequiresCsrf() throws Exception {mvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json").content("{\"email\":\"vineeth@example.test\"}")).andExpect(status().isForbidden());}
 @Test void shortPasswordRejected(){assertThrows(ApiError.class,()->passwords.reset(TOKEN,"short"));}
 @Autowired org.springframework.context.ApplicationEventPublisher events;
 @Autowired org.springframework.transaction.support.TransactionTemplate tx;
 @Test
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 void notificationEmailWaitsForCommitAndSkipsRollback() {
  when(email.enabled()).thenReturn(true);
  tx.executeWithoutResult(t -> {
   events.publishEvent(new NotificationEmail.Event("u1","Assignment ready"));
   verify(email,never()).notification(anyString(),anyString());
  });
  verify(email,timeout(2000)).notification("vineeth@example.test","Assignment ready");
  tx.executeWithoutResult(t -> {
   events.publishEvent(new NotificationEmail.Event("u1","Rolled back"));
   t.setRollbackOnly();
  });
  verify(email,never()).notification(anyString(),eq("Rolled back"));
 }
}
