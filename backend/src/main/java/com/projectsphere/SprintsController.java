package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/sprints")
class SprintsController extends ModuleEndpoints {
 SprintsController(ReadService reads,WorkspaceService writes){super("sprints",reads,writes);}
}
