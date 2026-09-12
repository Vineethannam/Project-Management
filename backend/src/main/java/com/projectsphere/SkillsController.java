package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/skills")
class SkillsController extends ModuleEndpoints {
 SkillsController(ReadService reads,WorkspaceService writes){super("skills",reads,writes);}
}
