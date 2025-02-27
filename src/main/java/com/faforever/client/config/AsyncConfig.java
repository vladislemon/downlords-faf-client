package com.faforever.client.config;

import com.faforever.client.exception.GlobalExceptionHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EnableAsync
@EnableScheduling
@Configuration
@Slf4j
@AllArgsConstructor
public class AsyncConfig implements AsyncConfigurer, SchedulingConfigurer {

  private final GlobalExceptionHandler globalExceptionHandler;

  @Override
  public Executor getAsyncExecutor() {
    return taskExecutor();
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return globalExceptionHandler;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    taskRegistrar.setTaskScheduler(taskScheduler());
  }

  @Bean
  public ExecutorService taskExecutor() {
    return Executors.newCachedThreadPool();
  }

  @Bean
  public TaskScheduler taskScheduler() {
    return new ThreadPoolTaskScheduler();
  }
  
  @Bean
  public DestructionAwareBeanPostProcessor threadPoolShutdownProcessor() {
    return (Object bean, String beanName) -> {
      if ("taskExecutor".equals(beanName)) {
        log.info("Shutting down ExecutorService '" + beanName + "'");
        ExecutorService executor = (ExecutorService) bean;
        executor.shutdownNow();
      }
    };
  }
}
