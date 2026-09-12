package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/audit")
class AuditController extends ReadModuleEndpoints {
 AuditController(ReadService reads){super("audit",reads);}
}
