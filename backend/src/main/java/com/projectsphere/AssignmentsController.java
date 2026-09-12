package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/assignments")
class AssignmentsController extends ModuleEndpoints {
 AssignmentsController(ReadService reads,WorkspaceService writes){super("assignments",reads,writes);}
}
