package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/comments")
class CommentsController extends ModuleEndpoints {
 CommentsController(ReadService reads,WorkspaceService writes){super("comments",reads,writes);}
}
