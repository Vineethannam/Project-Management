package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/courses")
class CoursesController extends ModuleEndpoints {
 CoursesController(ReadService reads,WorkspaceService writes){super("courses",reads,writes);}
}
