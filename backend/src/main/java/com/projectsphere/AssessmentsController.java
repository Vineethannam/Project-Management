package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/assessments")
class AssessmentsController extends ModuleEndpoints {
 AssessmentsController(ReadService reads,WorkspaceService writes){super("assessments",reads,writes);}
}
