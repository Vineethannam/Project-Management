package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/tasks")
class TasksController extends ModuleEndpoints {
 TasksController(ReadService reads,WorkspaceService writes){super("tasks",reads,writes);}
 @PatchMapping("/{id}/progress") public Map<String,Object> progress(@PathVariable String id,@RequestBody Map<String,Object> body,HttpServletRequest req){return fields(id,body,Set.of("status","actualHours"),req);}
 @PatchMapping("/{id}/assignee") public Map<String,Object> assignee(@PathVariable String id,@RequestBody Map<String,Object> body,HttpServletRequest req){return fields(id,body,Set.of("ownerId"),req);}
}
