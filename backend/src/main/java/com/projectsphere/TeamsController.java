package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/teams")
class TeamsController extends ModuleEndpoints {
 TeamsController(ReadService reads,WorkspaceService writes){super("teams",reads,writes);}
 @PutMapping("/{id}/members") public Map<String,Object> members(@PathVariable String id,@RequestBody Map<String,Object> body,HttpServletRequest req){return fields(id,body,Set.of("memberIds","leadId"),req);}
}
