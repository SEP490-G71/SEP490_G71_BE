package vn.edu.fpt.medicaldiagnosis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean("deferredResultExecutor")
    public TaskExecutor deferredResultExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);       // số thread tối thiểu
        executor.setMaxPoolSize(100);       // tối đa concurrent request
        executor.setQueueCapacity(100);     // nếu nhiều hơn max, đưa vào queue
        executor.setThreadNamePrefix("DeferredResult-");
        executor.initialize();
        return executor;
    }
}
