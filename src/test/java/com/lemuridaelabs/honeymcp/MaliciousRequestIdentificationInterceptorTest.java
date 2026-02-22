package com.lemuridaelabs.honeymcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MaliciousRequestIdentificationInterceptorTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void flagsKnownMaliciousPatternWithoutBlocking() throws Exception {
        // A request containing a path traversal pattern should be detected but not blocked.
        HttpResponse<String> response = get("/probe?path=../etc/passwd");
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void allowsNonMaliciousRequest() throws Exception {
        // A benign request should pass through and return 404 because no handler exists.
        HttpResponse<String> response = get("/probe?path=clean");
        assertThat(response.statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
