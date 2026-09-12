package com.projectsphere;

import static com.projectsphere.Policy.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
class FileController {
  final Store store;
  final Policy policy;
  final WorkspaceService service;

  @Value("${app.upload-dir}")
  String directory;

  FileController(Store s, Policy p, WorkspaceService w) {
    store = s;
    policy = p;
    service = w;
  }

  @PostMapping
  @Transactional
  public Map<String, Object> upload(
      @RequestParam String entityKind,
      @RequestParam String entityId,
      @RequestParam MultipartFile file,
      HttpServletRequest req)
      throws Exception {
    String uid = AuthController.userId(req);
    var s = store.context();
    var me = service.me(s, uid);
    policy.require(s, me, "documents", "create");
    policy.require(s, me, entityKind, "read");
    var target = store.find(entityKind, entityId);
    if (!policy.scope(s, me, entityKind, target)) throw new ApiError(404, "Record not found");
    String name =
        Objects.toString(file.getOriginalFilename(), "file").replaceAll("[\\r\\n\\\\/]", "_");
    service.assertTrue(
        name.length() <= 250 && file.getSize() > 0 && file.getSize() <= 10 * 1024 * 1024,
        "Upload a file up to 10 MB");
    String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    service.assertTrue(
        Set.of("pdf", "png", "jpg", "jpeg", "txt", "csv", "docx", "xlsx", "pptx", "mp4", "webm")
            .contains(ext),
        "Unsupported file type");
    String id = UUID.randomUUID().toString();
    Path base = Path.of(directory).toAbsolutePath().normalize();
    Files.createDirectories(base);
    Path path = base.resolve(id);
    file.transferTo(path);
    // Delete bytes if the surrounding database transaction rolls back.
    org.springframework.transaction.support.TransactionSynchronizationManager
        .registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
              public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED)
                  try {
                    Files.deleteIfExists(path);
                  } catch (Exception ignored) {
                  }
              }
            });
    store.db.update(
        "INSERT INTO uploaded_file(id,entity_id,filename,media_type,storage_key,bytes,uploaded_by)"
            + " VALUES(?,?,?,?,?,?,?)",
        id,
        entityId,
        name,
        "application/octet-stream",
        id,
        file.getSize(),
        uid);
    store.audit(uid, "Uploaded file", entityKind, entityId);
    return Map.of("id", id, "filename", name, "bytes", file.getSize());
  }

  @GetMapping
  public Map<String,Object> list(@RequestParam String entityKind, @RequestParam String entityId,
      @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size, HttpServletRequest req) {
    if(page<0||page>100000||size<1||size>100)throw new ApiError(400,"Invalid pagination");
    var s=store.context();var me=service.me(s,AuthController.userId(req));
    policy.require(s,me,"documents","read");policy.require(s,me,entityKind,"read");
    var item=store.find(entityKind,entityId);
    if(!policy.scope(s,me,entityKind,item))throw new ApiError(404,"Record not found");
    var rows=store.db.queryForList("SELECT id,filename,bytes,created_at FROM uploaded_file WHERE entity_id=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",entityId,size+1,page*size);
    boolean next=rows.size()>size;if(next)rows.remove(rows.size()-1);
    return Map.of("items",rows,"page",page,"size",size,"hasNext",next);
  }

  @GetMapping("/{id}")
  public ResponseEntity<FileSystemResource> download(
      @PathVariable String id, HttpServletRequest req) {
    var s = store.context();
    var me = service.me(s, AuthController.userId(req));
    policy.require(s, me, "documents", "read");
    var rows =
        store.db.queryForList(
            "SELECT f.*,e.kind FROM uploaded_file f JOIN entity e ON e.id=f.entity_id WHERE f.id=?",
            id);
    if (rows.isEmpty()) throw new ApiError(404, "File not found");
    var f = rows.get(0);
    String kind = f.get("kind").toString();
    policy.require(s, me, kind, "read");
    if (!policy.scope(s, me, kind, store.find(kind, f.get("entity_id").toString())))
      throw new ApiError(404, "File not found");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(f.get("filename").toString(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(new FileSystemResource(Path.of(directory).resolve(f.get("storage_key").toString())));
  }
}
