package com.ust.gitproxy.controller;

import com.ust.gitproxy.config.ProxyProperties;
import com.ust.gitproxy.model.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Objects;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valeriy Kucherenko
 * @since 09.02.2023
 */
@Import(ProxyProperties.class)
@WebMvcTest(controllers = ProxyController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {"app.proxy.connect-timeout=5s", "app.proxy.request-timeout=55s", "app.proxy.max-per-route=5", "app.proxy.max-total=25"})
class ProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProxyController proxyController;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplate proxyRestTemplate = (RestTemplate) ReflectionTestUtils.getField(proxyController, "restTemplate");
        mockServer = MockRestServiceServer.createServer(Objects.requireNonNull(proxyRestTemplate));
    }

    @Test
    void requestProxyTest() throws Exception {
        String expectedContent = "got it!";

        mockServer
                .expect(ExpectedCount.once(),
                        requestTo(new URI("http://example.com:8080/?q=IterationId,%2520Edm.String")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(expectedContent));

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/http/example.com:8080/?q=IterationId,%20Edm.String")
                        .header(Credentials.HEADER, "ewogICJnaXQiOiAidXNlciBwYXNzIiwKICAicHJveHkiOiAiQXV0aG9yaXphdGlvbiBCZWFyZXIgdG9rZW4iCn0="))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(expectedContent));
    }

    @Test
    void badCredentialsTest() throws Exception {
        mockServer
                .expect(ExpectedCount.once(),
                        requestTo(new URI("http://example.com:8080")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/http/example.com:8080")
                        .header(Credentials.HEADER, "this_must_be_a_joke"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
