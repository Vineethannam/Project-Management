package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/reviews")
class ReviewsController extends ModuleEndpoints {
 ReviewsController(ReadService reads,WorkspaceService writes){super("reviews",reads,writes);}
}
