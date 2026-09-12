package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/documents")
class DocumentsController extends ModuleEndpoints {
 DocumentsController(ReadService reads,WorkspaceService writes){super("documents",reads,writes);}
}
