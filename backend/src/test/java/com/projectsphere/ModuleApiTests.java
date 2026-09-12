package com.projectsphere;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","app.bootstrap-email=vineeth@example.test","app.bootstrap-password=test-only-password-12345"})
@Import(TestFixtures.class) @AutoConfigureMockMvc @Transactional
class ModuleApiTests {
 @Autowired Store store;@Autowired MockMvc mvc;@Autowired ReadService reads;@Autowired Policy policy;
 MockHttpSession session(String uid){var s=new MockHttpSession();s.setAttribute("userId",uid);s.setAttribute("authVersion",0);return s;}
 @Test void boundedModuleList() throws Exception {mvc.perform(get("/api/v1/tasks?size=2").session(session("u1"))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.hasNext").value(true)).andExpect(jsonPath("$.state").doesNotExist()).andExpect(jsonPath("$.users").doesNotExist());}
 @Test void pagesDoNotOverlap(){var a=(List<Map<String,Object>>)reads.list("u1","tasks",Map.of("size","2")).get("items");var b=(List<Map<String,Object>>)reads.list("u1","tasks",Map.of("size","2","page","1")).get("items");assertTrue(a.stream().noneMatch(x->b.stream().anyMatch(y->x.get("id").equals(y.get("id")))));}
 @Test void scopeBeforePagination(){var context=store.state();var user=store.find("users","u3");var expected=context.get("tasks").stream().filter(t->policy.scope(context,user,"tasks",t)).map(t->t.get("id")).collect(java.util.stream.Collectors.toSet());Set<Object> seen=new HashSet<>();for(int p=0;;p++){var result=reads.list("u3","tasks",Map.of("size","1","page",""+p));for(var item:(List<Map<String,Object>>)result.get("items"))seen.add(item.get("id"));if(!Boolean.TRUE.equals(result.get("hasNext")))break;}assertEquals(expected,seen);}
 @Test void hiddenDetail() throws Exception {mvc.perform(get("/api/v1/tasks/t2").session(session("u3"))).andExpect(status().isNotFound());}
 @Test void removedBulkEndpoints() throws Exception {mvc.perform(get("/api/v1/state").session(session("u1"))).andExpect(status().isNotFound());mvc.perform(post("/api/v1/entities/projects").session(session("u1")).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isNotFound());}
 @Test void forbiddenIsNotEmptySuccess() throws Exception {mvc.perform(get("/api/v1/settings").session(session("u3"))).andExpect(status().isForbidden());}
 @Test void invalidPagination() throws Exception {mvc.perform(get("/api/v1/tasks?size=101").session(session("u1"))).andExpect(status().isBadRequest());mvc.perform(get("/api/v1/tasks?page=-1").session(session("u1"))).andExpect(status().isBadRequest());}
 @Test void smallSessionResponse() throws Exception {mvc.perform(get("/api/v1/auth/session").session(session("u3"))).andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value("u3")).andExpect(jsonPath("$.permissions.tasks.read").value(true)).andExpect(jsonPath("$.state").doesNotExist());}
 @Test void literalSearch(){assertEquals(0,((List<?>)reads.list("u1","tasks",Map.of("q","%_")).get("items")).size());}
 @Test void minimalReferenceOptions() throws Exception {mvc.perform(get("/api/v1/users/options?context=tasks&size=2").session(session("u1"))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.items[0].email").doesNotExist());mvc.perform(get("/api/v1/roles/options?context=tasks").session(session("u3"))).andExpect(status().isForbidden());}
 @Test void dedicatedProgressAndStaleVersion() throws Exception {mvc.perform(patch("/api/v1/tasks/t1/progress").session(session("u3")).with(csrf()).contentType("application/json").content("{\"status\":\"Done\",\"version\":0}")).andExpect(status().isForbidden());mvc.perform(patch("/api/v1/tasks/t1/progress").session(session("u3")).with(csrf()).contentType("application/json").content("{\"status\":\"Review\",\"version\":0}")).andExpect(status().isOk());mvc.perform(patch("/api/v1/tasks/t1/progress").session(session("u3")).with(csrf()).contentType("application/json").content("{\"status\":\"Testing\",\"version\":0}")).andExpect(status().isConflict());}
 @Test void summaryNotRows(){assertEquals(Set.of("counts","taskStatuses"),reads.summary("u1").keySet());}
 @Test void commentsFollowParent(){var c=new LinkedHashMap<String,Object>();c.put("id","test-comment");c.put("title","Private");c.put("status","Active");c.put("ownerId","u1");c.put("entityKind","tasks");c.put("entityId","t2");store.save("comments",c,true);assertEquals(0,((List<?>)reads.list("u3","comments",Map.of("entityId","t2")).get("items")).size());}
 @Test void resourceCrud() throws Exception {var response=mvc.perform(post("/api/v1/departments").session(session("u1")).with(csrf()).contentType("application/json").content("{\"title\":\"API team\",\"status\":\"Active\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();String id=store.parse(response).get("id").toString();mvc.perform(get("/api/v1/departments/"+id).session(session("u1"))).andExpect(status().isOk());mvc.perform(delete("/api/v1/departments/"+id+"?version=0").session(session("u1")).with(csrf())).andExpect(status().isOk());}
}
