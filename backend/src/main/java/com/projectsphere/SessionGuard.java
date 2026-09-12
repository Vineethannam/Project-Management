package com.projectsphere;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
class SessionGuard implements WebMvcConfigurer {
  final Store store;
  SessionGuard(Store store) { this.store=store; }
  @Override public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new HandlerInterceptor() {
      @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler) {
        String uid=AuthController.userId(req);
        var rows=store.db.queryForList("SELECT auth_version FROM account WHERE user_id=?",uid);
        var session=req.getSession(false);
        Object version=session.getAttribute("authVersion");
        if (rows.isEmpty() || !(version instanceof Number)
            || ((Number)version).intValue()!=((Number)rows.get(0).get("auth_version")).intValue()
            || !"Active".equals(store.find("users",uid).get("status"))) {
          session.invalidate(); throw new ApiError(401,"Session expired. Please sign in again.");
        }
        return true;
      }
    }).addPathPatterns("/api/v1/**").excludePathPatterns("/api/v1/auth/csrf","/api/v1/auth/login","/api/v1/auth/forgot-password","/api/v1/auth/reset-password");
  }
}
