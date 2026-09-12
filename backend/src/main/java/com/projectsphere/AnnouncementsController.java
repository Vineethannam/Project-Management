package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/announcements")
class AnnouncementsController extends ModuleEndpoints {
 AnnouncementsController(ReadService reads,WorkspaceService writes){super("announcements",reads,writes);}
}
