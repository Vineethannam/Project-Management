package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/projects")
class ProjectsController extends ModuleEndpoints {
 ProjectsController(ReadService reads,WorkspaceService writes){super("projects",reads,writes);}
 @PutMapping("/{id}/teams") public Map<String,Object> teams(@PathVariable String id,@RequestBody Map<String,Object> body,HttpServletRequest req){return fields(id,body,Set.of("teamIds"),req);}
}
