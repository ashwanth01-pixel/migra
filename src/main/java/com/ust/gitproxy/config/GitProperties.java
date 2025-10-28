package com.ust.gitproxy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author Valeriy Kucherenko
 * @since 15.11.2022
 */
@Getter
@Setter
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "app.git")
public class GitProperties {
    /**
     * Location where GIT will check out repositories. Default: system temp directory.
     */
    private String checkoutPath = System.getProperty("java.io.tmpdir");

    /**
     * Internal executor gracefull shutdown timeout. Default: 10 seconds.
     */
    private Duration executorShutdownTimeout = Duration.ofSeconds(10);

    /**
     * Parallelism level of GIT service work-stealing pool.
     * 0 - use the number of available processors as its target parallelism level (default).
     * The parallelism level corresponds to the maximum number of threads actively engaged in, or available to engage in, task processing.
     * The actual number of threads may grow and shrink dynamically.
     */
    private int parallelism = 0;
}
