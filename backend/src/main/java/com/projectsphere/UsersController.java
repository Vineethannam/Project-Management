package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/users")
class UsersController extends ModuleEndpoints {
 UsersController(ReadService reads,WorkspaceService writes){super("users",reads,writes);}
}
