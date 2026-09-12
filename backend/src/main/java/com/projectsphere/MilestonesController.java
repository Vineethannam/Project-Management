package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/milestones")
class MilestonesController extends ModuleEndpoints {
 MilestonesController(ReadService reads,WorkspaceService writes){super("milestones",reads,writes);}
}
