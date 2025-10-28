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
@ConfigurationProperties(prefix = "app.proxy")
public class ProxyProperties {
    private Duration connectTimeout;
    private Duration requestTimeout;
    private int maxTotal;
    private int maxPerRoute;
}
