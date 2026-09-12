package com.projectsphere;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class NotificationEmail {
 record Event(String userId,String title) {}
 final Store store;
 final EmailService email;
 final TaskExecutor executor;
 NotificationEmail(Store store,EmailService email,@Qualifier("mailExecutor") TaskExecutor executor){this.store=store;this.email=email;this.executor=executor;}
 @TransactionalEventListener
 public void deliver(Event event) {
  if (!email.enabled()) return;
  try { executor.execute(() -> {
   var accounts=store.db.queryForList("SELECT a.email FROM account a JOIN entity u ON u.id=a.user_id WHERE a.user_id=? AND u.status='Active'",event.userId());
   if(!accounts.isEmpty()) email.notification(accounts.get(0).get("email").toString(),event.title());
  }); } catch (TaskRejectedException e) {
   org.slf4j.LoggerFactory.getLogger(NotificationEmail.class).warn("Notification email queue full. In-app notification retained.");
  }
 }
}
