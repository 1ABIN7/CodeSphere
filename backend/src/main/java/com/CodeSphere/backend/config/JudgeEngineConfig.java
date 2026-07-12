package com.CodeSphere.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Configuration for the sandboxed code judge engine.
 *
 * Defines supported languages, their compilation/execution commands,
 * thread pool settings for concurrent test case execution,
 * and default time/memory limits.
 */
@Configuration
@ConfigurationProperties(prefix = "judge")
@Getter
@Setter
public class JudgeEngineConfig {

    /**
     * Default time limit per test case in milliseconds.
     */
    private int defaultTimeLimit = 2000;

    /**
     * Default memory limit per test case in KB.
     */
    private int defaultMemoryLimit = 262144; // 256 MB

    /**
     * Maximum code length in characters.
     */
    private int maxCodeLength = 65536; // 64KB

    /**
     * Thread pool core size for concurrent test case execution.
     */
    private int threadPoolCoreSize = 4;

    /**
     * Thread pool max size.
     */
    private int threadPoolMaxSize = 8;

    /**
     * Queue capacity for pending executions.
     */
    private int queueCapacity = 100;

    /**
     * Temporary directory for compilation artifacts.
     */
    private String tempDir = "./judge-temp";

    /**
     * Supported programming languages.
     */
    private List<String> supportedLanguages = List.of(
            "java", "python", "cpp", "c", "javascript"
    );

    /**
     * File extensions for each language.
     */
    private Map<String, String> fileExtensions = Map.of(
            "java", ".java",
            "python", ".py",
            "cpp", ".cpp",
            "c", ".c",
            "javascript", ".js"
    );

    /**
     * Creates a thread pool executor for the judge engine.
     */
    @Bean(name = "judgeExecutor")
    public Executor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolCoreSize);
        executor.setMaxPoolSize(threadPoolMaxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("judge-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
