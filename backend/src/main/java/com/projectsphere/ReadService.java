package com.projectsphere;

import static com.projectsphere.Policy.*;
import java.util.*;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
class ReadService {
  final Store store;
  final Policy policy;
  final WorkspaceService workspace;
  ReadService(Store store, Policy policy, WorkspaceService workspace) {
    this.store=store; this.policy=policy; this.workspace=workspace;
  }
  Map<String,Object> session(String uid) {
    var context=store.loadKinds(List.of("users","roles"));
    var user=workspace.me(context,uid);
    var role=get(context,"roles",str(user,"roleId"));
    return Map.of("user",user,"roleName",role==null?"":str(role,"title"),"permissions",
      role!=null && "Active".equals(role.get("status")) ? role.getOrDefault("permissions",Map.of()):Map.of());
  }
  int page(Map<String,String> query) { return number(query,"page",0,0,100000); }
  int size(Map<String,String> query) { return number(query,"size",25,1,100); }
  int number(Map<String,String> query,String key,int fallback,int min,int max) {
    try {int v=Integer.parseInt(query.getOrDefault(key,""+fallback));if(v<min||v>max)throw new NumberFormatException();return v;}
    catch(NumberFormatException e){throw new ApiError(400,"Invalid "+key);}
  }
  boolean visible(Map<String,List<Map<String,Object>>> context,Map<String,Object> user,String kind,Map<String,Object> item) {
    if (Set.of("comments","documents").contains(kind) && !str(item,"entityKind").isBlank()) {
      String parent=str(item,"entityKind");
      if(!KINDS.contains(parent)||Set.of("comments","documents").contains(parent)) return false;
      try {
        var target=store.find(parent,str(item,"entityId"));
        return policy.can(context,user,parent,"read") && policy.scope(context,user,parent,target);
      } catch(ApiError e){if(e.status==404)return false;throw e;}
    }
    return policy.scope(context,user,kind,item);
  }
  // Reads one module in bounded database batches. No application-wide snapshot.
  void scan(String kind,Map<String,String> query,Consumer<Map<String,Object>> accept) {
    if(!KINDS.contains(kind))throw new ApiError(404,"Module not found");
    StringBuilder sql=new StringBuilder("SELECT payload,version FROM entity WHERE kind=?");
    List<Object> args=new ArrayList<>();args.add(kind);
    for(String field:List.of("status","projectId","ownerId")) {
      String value=query.getOrDefault(field,"");
      if(!value.isBlank()){sql.append(" AND ").append(field.equals("projectId")?"project_id":field.equals("ownerId")?"owner_id":"status").append("=?");args.add(value);}
    }
    String q=query.getOrDefault("q","").trim();
    if(q.length()>200)throw new ApiError(400,"Search is too long");
    if(!q.isBlank()){sql.append(" AND LOWER(title) LIKE ? ESCAPE '!'");args.add("%"+q.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}
    sql.append(" ORDER BY created_at DESC,id DESC LIMIT 200 OFFSET ?");
    int offset=0;
    while(true){
      List<Object> batchArgs=new ArrayList<>(args);batchArgs.add(offset);
      var batch=store.db.query(sql.toString(),(rs,n)->{var d=store.parse(rs.getString(1));d.put("version",rs.getInt(2));return d;},batchArgs.toArray());
      for(var item:batch){
        boolean matches=true;
        for(String field:List.of("entityKind","entityId","courseId","assessmentId","reportingTo"))
          if(query.containsKey(field)&&!query.get(field).equals(str(item,field)))matches=false;
        if(matches)accept.accept(item);
      }
      if(batch.size()<200)return;offset+=200;
    }
  }
  static class Enough extends RuntimeException {}
  Map<String,Object> list(String uid,String kind,Map<String,String> query) {
    var context=store.context();var user=workspace.me(context,uid);policy.require(context,user,kind,"read");
    int page=page(query),size=size(query);long start=(long)page*size;long[] seen={0};
    List<Map<String,Object>> items=new ArrayList<>();
    try { scan(kind,query,item->{
      if(!visible(context,user,kind,item))return;
      if(seen[0]++<start)return;
      items.add(present(context,user,kind,item,false));
      if(items.size()>size)throw new Enough();
    }); } catch(Enough done){}
    boolean next=items.size()>size;if(next)items.remove(items.size()-1);
    return Map.of("items",items,"page",page,"size",size,"hasNext",next);
  }
  Map<String,Object> detail(String uid,String kind,String id) {
    var context=store.context();var user=workspace.me(context,uid);policy.require(context,user,kind,"read");
    var item=store.find(kind,id);
    if(!visible(context,user,kind,item))throw new ApiError(404,"Record not found");
    return present(context,user,kind,item,true);
  }
  Map<String,Object> present(Map<String,List<Map<String,Object>>> context,Map<String,Object> user,String kind,Map<String,Object> item,boolean detail) {
    var result=policy.sanitize(context,user,kind,item);
    if(!detail){result.remove("questions");result.remove("lessons");result.remove("answers");result.remove("permissions");result.remove("submission");}
    var labels=new LinkedHashMap<String,String>();
    for(var entry:Map.of("ownerId","users","projectId","projects","roleId","roles","reportingTo","users","courseId","courses","sprintId","sprints","leadId","users").entrySet()) {
      String id=str(item,entry.getKey());if(id.isBlank())continue;
      try {var target=store.find(entry.getValue(),id);
        if(policy.scope(context,user,entry.getValue(),target))labels.put(entry.getKey(),str(target,"title"));
      }catch(ApiError ignored){}
    }
    result.put("labels",labels);
    var project=get(context,"projects",str(item,"projectId"));
    result.put("canManage",policy.manages(user,project)||!str(item,"ownerId").equals(str(user,"id"))&&policy.reports(context,str(user,"id")).contains(str(item,"ownerId")));
    return result;
  }
  static final Map<String,Set<String>> REFERENCES=Map.ofEntries(
    Map.entry("users",Set.of("users","roles")),Map.entry("teams",Set.of("users")),
    Map.entry("projects",Set.of("teams","users")),Map.entry("tasks",Set.of("projects","users","sprints","tasks")),
    Map.entry("bugs",Set.of("projects","users")),Map.entry("sprints",Set.of("projects")),
    Map.entry("milestones",Set.of("projects")),Map.entry("risks",Set.of("projects","users")),
    Map.entry("standups",Set.of("projects")),Map.entry("retrospectives",Set.of("projects","sprints")),
    Map.entry("assignments",Set.of("users")),Map.entry("enrollments",Set.of("users","courses")),
    Map.entry("assessments",Set.of("courses")),Map.entry("skills",Set.of("users")),
    Map.entry("reviews",Set.of("users")),Map.entry("announcements",Set.of("projects")),
    Map.entry("documents",Set.of("projects","tasks","bugs","assignments","courses","sprints")));
  Map<String,Object> options(String uid,String source,Map<String,String> query) {
    String target=query.getOrDefault("context",source);
    var context=store.context();var user=workspace.me(context,uid);
    boolean permitted=source.equals(target)&&policy.can(context,user,source,"read")
      || REFERENCES.getOrDefault(target,Set.of()).contains(source)
      && (policy.can(context,user,target,"create")||policy.can(context,user,target,"update"));
    if(!permitted)throw new ApiError(403,"Reference list is unavailable");
    int page=page(query),size=size(query);int[] seen={0};List<Map<String,Object>> items=new ArrayList<>();
    Map<String,String> filter=new HashMap<>(query);
    if(source.equals("users"))filter.remove("projectId");
    String projectId=query.getOrDefault("projectId","");
    var project=projectId.isBlank()?null:store.find("projects",projectId);
    if(project!=null&&!policy.scope(context,user,"projects",project))throw new ApiError(404,"Project not found");
    Set<String> selected=new HashSet<>(Arrays.asList(query.getOrDefault("ids","").split(",")));
    boolean selectIds=query.containsKey("ids");
    try {scan(source,filter,item->{
      if(!visible(context,user,source,item)||"Inactive".equals(item.get("status")))return;
      if(selectIds&&!selected.contains(str(item,"id")))return;
      if(source.equals("users")&&project!=null&&!policy.members(context,project).contains(str(item,"id")))return;
      if(seen[0]++<page*size)return;
      items.add(Map.of("id",item.get("id"),"title",item.get("title")));
      if(items.size()>size)throw new Enough();
    });}catch(Enough done){}
    boolean next=items.size()>size;if(next)items.remove(items.size()-1);
    return Map.of("items",items,"page",page,"size",size,"hasNext",next);
  }
  Map<String,Object> summary(String uid) {
    var context=store.context();var user=workspace.me(context,uid);
    Map<String,Long> counts=new LinkedHashMap<>();Map<String,Long> statuses=new LinkedHashMap<>();
    for(String kind:List.of("tasks","projects","bugs")) {
      if(!policy.can(context,user,kind,"read"))continue;
      long[] total={0},done={0},overdue={0};
      scan(kind,Map.of(),item->{if(!visible(context,user,kind,item))return;total[0]++;
        if("Done".equals(item.get("status")))done[0]++;
        if(kind.equals("tasks")){statuses.merge(str(item,"status"),1L,Long::sum);
          String due=str(item,"dueDate");if(!due.isBlank()&&due.compareTo(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString())<0&&!"Done".equals(item.get("status")))overdue[0]++;}
      });counts.put(kind,total[0]);if(kind.equals("tasks")){counts.put("completed",done[0]);counts.put("overdue",overdue[0]);}
    }
    return Map.of("counts",counts,"taskStatuses",statuses);
  }
  Map<String,Object> currentAttendance(String uid) {
    var context=store.context();var user=workspace.me(context,uid);policy.require(context,user,"attendance","read");
    var found=new LinkedHashMap<String,Object>();
    try {scan("attendance",Map.of("ownerId",uid),item->{if(!item.containsKey("checkOut")){found.putAll(item);throw new Enough();}});}catch(Enough done){}
    return found;
  }
  Map<String,Object> workload(String uid,Map<String,String> query) {
    var context=store.context();var user=workspace.me(context,uid);policy.require(context,user,"reports","read");policy.require(context,user,"tasks","read");policy.require(context,user,"users","read");
    var page=list(uid,"users",query);
    var metrics=new LinkedHashMap<String,Map<String,Object>>();
    for(var person:(List<Map<String,Object>>)page.get("items")) {
      var row=new LinkedHashMap<String,Object>();row.put("id",person.get("id"));row.put("title",person.get("title"));row.put("capacity",person.getOrDefault("capacity",40));row.put("assigned",0L);row.put("completed",0L);row.put("hours",0.0);metrics.put(str(person,"id"),row);
    }
    scan("tasks",Map.of(),task->{var row=metrics.get(str(task,"ownerId"));if(row==null||!visible(context,user,"tasks",task))return;
      row.put("assigned",(Long)row.get("assigned")+1);if("Done".equals(task.get("status")))row.put("completed",(Long)row.get("completed")+1);
      else row.put("hours",(Double)row.get("hours")+((Number)task.getOrDefault("estimatedHours",0)).doubleValue());
    });
    return Map.of("items",metrics.values(),"page",page.get("page"),"size",page.get("size"),"hasNext",page.get("hasNext"));
  }
}
