package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/roles")
class RolesController extends ModuleEndpoints {
 RolesController(ReadService reads,WorkspaceService writes){super("roles",reads,writes);}
 @PutMapping("/{id}/permissions") public Map<String,Object> permissions(@PathVariable String id,@RequestBody Map<String,Object> body,HttpServletRequest req){return fields(id,body,Set.of("permissions"),req);}
}
