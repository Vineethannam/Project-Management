package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/attendance")
class AttendanceController extends ReadModuleEndpoints {
 AttendanceController(ReadService reads){super("attendance",reads);}
 @GetMapping("/current") public Map<String,Object> current(HttpServletRequest req){return reads.currentAttendance(AuthController.userId(req));}
}
