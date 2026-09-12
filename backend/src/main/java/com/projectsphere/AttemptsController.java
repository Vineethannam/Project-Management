package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/attempts")
class AttemptsController extends ReadModuleEndpoints {
 AttemptsController(ReadService reads){super("attempts",reads);}
}
