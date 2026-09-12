package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/settings")
class SettingsController extends ModuleEndpoints {
 SettingsController(ReadService reads,WorkspaceService writes){super("settings",reads,writes);}
}
