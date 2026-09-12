package com.projectsphere;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;

abstract class ReadModuleEndpoints {
 final String kind;
 final ReadService reads;
 ReadModuleEndpoints(String kind,ReadService reads){this.kind=kind;this.reads=reads;}
 @GetMapping public Map<String,Object> list(@RequestParam Map<String,String> query,HttpServletRequest req){return reads.list(AuthController.userId(req),kind,query);}
 @GetMapping("/options") public Map<String,Object> options(@RequestParam Map<String,String> query,HttpServletRequest req){return reads.options(AuthController.userId(req),kind,query);}
 @GetMapping("/{id}") public Map<String,Object> detail(@PathVariable String id,HttpServletRequest req){return reads.detail(AuthController.userId(req),kind,id);}
}
abstract class ModuleEndpoints extends ReadModuleEndpoints {
 final WorkspaceService writes;
 ModuleEndpoints(String kind,ReadService reads,WorkspaceService writes){super(kind,reads);this.writes=writes;}
 @PostMapping public Map<String,Object> create(@RequestBody Map<String,Object> body,HttpServletRequest req){return writes.save(AuthController.userId(req),kind,null,body);}
 @PutMapping("/{id}") public Map<String,Object> update(@PathVariable String id,@RequestBody Map<String,Object> body,HttpServletRequest req){return writes.save(AuthController.userId(req),kind,id,body);}
 @DeleteMapping("/{id}") public Map<String,Boolean> delete(@PathVariable String id,@RequestParam int version,HttpServletRequest req){writes.delete(AuthController.userId(req),kind,id,version);return Map.of("success",true);}
 Map<String,Object> fields(String id,Map<String,Object> body,Set<String> allowed,HttpServletRequest req){
  if(body.keySet().stream().anyMatch(key->!key.equals("version")&&!allowed.contains(key)))throw new ApiError(400,"Unexpected fields for this action");
  return writes.save(AuthController.userId(req),kind,id,body);
 }
}
