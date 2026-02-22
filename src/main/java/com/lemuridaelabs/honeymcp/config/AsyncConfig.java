package com.lemuridaelabs.honeymcp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous method execution with proper thread pool sizing and error handling.
 *
 * <p>Enables Spring's async capabilities which are used by the
 * {@link com.lemuridaelabs.honeymcp.modules.notifications.service.PushNotificationService}
 * to send push notifications without blocking the main request thread.</p>
 *
 * <p>Configures:</p>
 * <ul>
 *   <li>Custom thread pool with bounded queue to prevent memory exhaustion</li>
 *   <li>Async exception handler to log uncaught exceptions from async methods</li>
 * </ul>
 *
 * @since 1.0
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Creates and configures the thread pool executor for async tasks.
     *
     * <p>Configuration:</p>
     * <ul>
     *   <li>Core pool size: 2 threads (minimum always available)</li>
     *   <li>Max pool size: 10 threads (scales up under load)</li>
     *   <li>Queue capacity: 500 tasks (bounded to prevent memory issues)</li>
     *   <li>Thread name prefix: "HoneyMCP-Async-" for easy identification in logs</li>
     * </ul>
     *
     * @return configured ThreadPoolTaskExecutor
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("HoneyMCP-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Async executor configured: corePoolSize=2, maxPoolSize=10, queueCapacity=500");
        return executor;
    }

    /**
     * Provides a custom exception handler for uncaught exceptions in async methods.
     *
     * <p>Logs exceptions at ERROR level with method details to ensure async failures
     * are visible in application logs rather than being silently swallowed.</p>
     *
     * @return AsyncUncaughtExceptionHandler that logs exceptions
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    /**
     * Custom exception handler that logs uncaught async exceptions.
     */
    @Slf4j
    private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        /**
         * Handles uncaught exceptions thrown during the execution of asynchronous methods.
         * Logs the exception details, including the method name and provided parameters.
         *
         * @param ex the exception that was thrown during asynchronous method execution
         * @param method the method where the exception was thrown
         * @param params the parameters that were passed to the method
         */
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Uncaught exception in async method '{}' with parameters {}: {}",
                    method.getName(),
                    params,
                    ex.getMessage(),
                    ex);
        }
    }
}
