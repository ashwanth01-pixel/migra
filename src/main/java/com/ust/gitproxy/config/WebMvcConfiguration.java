package com.ust.gitproxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ust.gitproxy.controller.resolver.CredentialsArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author Valeriy Kucherenko
 * @since 14.11.2022
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final ObjectFactory<ObjectMapper> objectMapperFactory;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CredentialsArgumentResolver(objectMapperFactory.getObject()));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Because proxy controller has wildcard pattern it's necessary to increase predecence of resource handler
        // in order to correctly handle /swagger-ui/swagger.html and other swagger requests. Otherwise, they will be
        // routed to ProxyController.
        registry.setOrder(-50);
    }
}
