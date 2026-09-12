package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationsController extends ReadModuleEndpoints {
 NotificationsController(ReadService reads){super("notifications",reads);}
}
