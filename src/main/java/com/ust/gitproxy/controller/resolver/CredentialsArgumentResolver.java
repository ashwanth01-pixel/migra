package com.ust.gitproxy.controller.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ust.gitproxy.model.Credentials;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * @author Valeriy Kucherenko
 * @since 14.11.2022
 */
@RequiredArgsConstructor
public class CredentialsArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameter().getType() == Credentials.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String auth = webRequest.getHeader(Credentials.HEADER);
        if (!StringUtils.hasText(auth)) {
            return new Credentials();
        }

        try {
            return objectMapper.readValue(Base64Utils.decodeFromString(auth), Credentials.class);
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
