package com.sdu.rabbix.transaction.config;

import com.sdu.rabbix.transaction.annotation.SecureInvokeConfigurer;
import com.sdu.rabbix.transaction.factory.MyThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Description: 线程池配置
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer, SecureInvokeConfigurer {

    @Override
    public Executor getSecureInvokeExecutor() {
        return threadPoolExecutor();
    }

    @Bean
    @Primary
    public ThreadPoolTaskExecutor threadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("rabbit-executor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadFactory(new MyThreadFactory(executor));
        executor.initialize();
        return executor;
    }
}
