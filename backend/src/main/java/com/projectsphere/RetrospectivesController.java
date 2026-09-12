package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/retrospectives")
class RetrospectivesController extends ModuleEndpoints {
 RetrospectivesController(ReadService reads,WorkspaceService writes){super("retrospectives",reads,writes);}
}
