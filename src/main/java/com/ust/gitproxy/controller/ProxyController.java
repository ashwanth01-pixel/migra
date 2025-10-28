package com.ust.gitproxy.controller;

import com.ust.gitproxy.config.ProxyProperties;
import com.ust.gitproxy.model.Credentials;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;

/**
 * @author Valeriy Kucherenko
 * @since 11.11.2022
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class ProxyController {
    private final ProxyProperties properties;

    private RestTemplate restTemplate;

    @PostConstruct
    void init() {
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(properties.getMaxTotal());
        connManager.setDefaultMaxPerRoute(properties.getMaxPerRoute());

        HttpClient client = HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .disableAutomaticRetries()
                .build();

        restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(properties.getConnectTimeout())
                .messageConverters(new ByteArrayHttpMessageConverter())
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .interceptors((request, body, execution) -> {
                    ClientHttpResponse r = execution.execute(request, body);
                    cleanupHeaders(r.getHeaders());
                    return r;
                })
                .build();
    }

    @SuppressWarnings("java:S1452")
    @RequestMapping(value = "/{scheme}/**", consumes = MediaType.ALL_VALUE, produces = MediaType.ALL_VALUE)
    @Operation(summary = "Proxies the request. Example: http://localhost:8088/https/gitlab.dagility.com/api/v4/version")
    public ResponseEntity<?> proxy(@Parameter(hidden = true)
                                   @RequestBody(required = false)
                                           byte[] body,
                                   @Schema(allowableValues = {"http", "https"})
                                   @PathVariable("scheme")
                                           String scheme,
                                   @RequestHeader
                                           HttpHeaders headers,
                                   Credentials credentials,
                                   HttpMethod method,
                                   HttpServletRequest request) {
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong value supplied for 'scheme'. Must be one of: http, https.");
        }

        String uri = request.getRequestURI().substring(scheme.length() + 1);
        uri = scheme + ":/" + uri;
        if (StringUtils.hasText(request.getQueryString())) {
            uri = uri + "?" + request.getQueryString();
        }

        uri = UriUtils.decode(uri, Charset.defaultCharset());

        cleanupHeaders(headers);

        if (StringUtils.hasText(credentials.getProxy())) {
            String[] proxyAuthTokens = credentials.getProxy().split(" ", 2);
            if (proxyAuthTokens.length == 2) {
                headers.set(proxyAuthTokens[0], proxyAuthTokens[1]);
            } else {
                log.warn("Wrong value supplied for proxy credentials: {}", credentials.getProxy());
            }
        }

        log.debug("Proxying request to {}", uri);

        try {
            return restTemplate.exchange(uri, method, new HttpEntity<>(body, headers), byte[].class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).headers(e.getResponseHeaders()).body(e.getMessage());
        } catch (RestClientException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(e.getCause().getMessage());
            }
            if (e.getCause() instanceof MalformedURLException) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getCause().getMessage());
            }

            log.error("Error proxying request to server: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private void cleanupHeaders(HttpHeaders headers) {
        try {
            headers.remove(Credentials.HEADER);
            headers.remove(HttpHeaders.HOST);
            headers.remove(HttpHeaders.AUTHORIZATION);
            headers.remove(HttpHeaders.CONNECTION);
            headers.remove(HttpHeaders.TRANSFER_ENCODING);
        } catch (UnsupportedOperationException ignored) {
            // ignored
        }
    }
}
