package com.projectsphere;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
class ReportsController {
 final ReadService reads;
 ReportsController(ReadService reads){this.reads=reads;}
 @GetMapping("/dashboard/summary") Map<String,Object> dashboard(HttpServletRequest req){return reads.summary(AuthController.userId(req));}
 @GetMapping("/reports/workload") Map<String,Object> workload(@RequestParam Map<String,String> query,HttpServletRequest req){return reads.workload(AuthController.userId(req),query);}
 @GetMapping("/reports/tasks.csv") org.springframework.http.ResponseEntity<String> csv(HttpServletRequest req){
  String uid=AuthController.userId(req);var context=reads.store.context();var user=reads.workspace.me(context,uid);
  reads.policy.require(context,user,"reports","read");reads.policy.require(context,user,"tasks","read");
  StringBuilder out=new StringBuilder("Title,Status,Priority,Due date,Estimated hours,Actual hours\r\n");
  reads.scan("tasks",Map.of(),item->{if(!reads.visible(context,user,"tasks",item))return;
    List<String> cells=new ArrayList<>();for(String field:List.of("title","status","priority","dueDate","estimatedHours","actualHours")){
      String value=Policy.str(item,field);if(value.matches("^[=+@-].*"))value="'"+value;cells.add("\""+value.replace("\"","\"\"")+"\"");}
    out.append(String.join(",",cells)).append("\r\n");
  });
  return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=projectsphere-tasks.csv").header("Cache-Control","no-store").contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8")).body(out.toString());
 }
}
