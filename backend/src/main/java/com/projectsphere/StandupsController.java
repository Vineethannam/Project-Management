package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/standups")
class StandupsController extends ModuleEndpoints {
 StandupsController(ReadService reads,WorkspaceService writes){super("standups",reads,writes);}
}
