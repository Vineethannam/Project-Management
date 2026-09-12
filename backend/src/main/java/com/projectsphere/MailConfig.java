package com.projectsphere;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration
@EnableAsync
class MailConfig {
  @Bean("mailExecutor")
  ThreadPoolTaskExecutor mailExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2); executor.setMaxPoolSize(2);
    executor.setQueueCapacity(100); executor.setThreadNamePrefix("mail-");
    executor.setWaitForTasksToCompleteOnShutdown(true); executor.setAwaitTerminationSeconds(15);
    return executor;
  }
}
