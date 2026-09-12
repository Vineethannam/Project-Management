package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/risks")
class RisksController extends ModuleEndpoints {
 RisksController(ReadService reads,WorkspaceService writes){super("risks",reads,writes);}
}
