package com.ust.gitproxy.config;

import com.ust.gitproxy.model.Credentials;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Valeriy Kucherenko
 * @since 13.04.2023
 */
@Profile("!test")
@Configuration(proxyBeanMethods = false)
public class SpringDocConfiguration {

    @Bean
    public ParameterCustomizer openApiParameterCustomizer() {
        return (parameterModel, methodParameter) -> {
            if (parameterModel != null && methodParameter.getParameterType().isAssignableFrom(Credentials.class)) {
                parameterModel
                        .in(ParameterIn.HEADER.toString())
                        .required(false)
                        .name(Credentials.HEADER);
            }
            return parameterModel;
        };
    }

    @Bean
    public OpenAPI openApi() throws IOException {
        return new OpenAPI()
                .info(apiInfo());
    }

    private Info apiInfo() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/openapi/description.html")) {
            return new Info()
                    .title("Git Proxy")
                    .description(StreamUtils.copyToString(is, StandardCharsets.UTF_8));
        }
    }
}
