package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/departments")
class DepartmentsController extends ModuleEndpoints {
 DepartmentsController(ReadService reads,WorkspaceService writes){super("departments",reads,writes);}
}
