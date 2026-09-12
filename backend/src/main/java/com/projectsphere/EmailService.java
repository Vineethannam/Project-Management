package com.projectsphere;

import java.net.URI;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
class EmailService {
  private final JavaMailSender sender;
  private final boolean enabled;
  private final String from;
  private final String frontend;

  EmailService(JavaMailSender sender, @Value("${app.mail-enabled}") boolean enabled,
      @Value("${app.mail-from}") String from, @Value("${app.frontend-url}") String frontend) {
    this.sender = sender; this.enabled = enabled; this.from = from;
    URI url = URI.create(frontend);
    if (url.getHost() == null || url.getRawQuery() != null || url.getRawFragment() != null
        || url.getUserInfo() != null || !("https".equals(url.getScheme())
        || ("http".equals(url.getScheme()) && ("localhost".equals(url.getHost()) || "127.0.0.1".equals(url.getHost())))))
      throw new IllegalArgumentException("FRONTEND_URL must be an HTTPS URL or local HTTP URL");
    this.frontend = frontend.replaceAll("/+$", "");
    if (enabled && from.isBlank()) throw new IllegalArgumentException("Set MAIL_FROM when email is enabled");
  }
  boolean enabled() { return enabled; }
  @org.springframework.scheduling.annotation.Async("mailExecutor")
  public void reset(String email, String token) {
    // Fragment keeps the secret out of HTTP access logs and Referer headers.
    send(email, "Reset your ProjectSphere password", "Open this link to reset your password:\n\n"
        + frontend + "/reset-password#token=" + token
        + "\n\nThis link expires in 15 minutes and can be used once. If you did not request it, ignore this email.");
  }
  void notification(String email, String title) {
    send(email, "ProjectSphere notification", title + "\n\nOpen your workspace: " + frontend + "/notifications");
  }
  void changed(String email) {
    send(email, "Your ProjectSphere password changed", "Your password was changed and existing sessions were signed out. If this was not you, contact your administrator immediately.");
  }
  private boolean send(String email, String subject, String body) {
    if (!enabled) return false;
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from); message.setTo(email); message.setSubject(subject); message.setText(body);
      sender.send(message); return true;
    } catch (org.springframework.mail.MailException e) {
      // Never log SMTP credentials, recipients, reset URLs, or message content.
      LoggerFactory.getLogger(EmailService.class).warn("Email delivery failed. Check backend SMTP configuration and provider availability.");
      return false;
    }
  }
}
