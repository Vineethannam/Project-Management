package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentsController extends ModuleEndpoints {
 EnrollmentsController(ReadService reads,WorkspaceService writes){super("enrollments",reads,writes);}
}
