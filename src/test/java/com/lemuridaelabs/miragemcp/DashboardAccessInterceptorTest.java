package com.lemuridaelabs.miragemcp;

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
class DashboardAccessInterceptorTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void rejectsMissingToken() throws Exception {
        // Requests without a token should be obscured as 404.
        HttpResponse<String> response = get("/dashboard");
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        // Invalid tokens should be treated the same as missing tokens.
        HttpResponse<String> response = get("/dashboard?token=bad-token");
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void allowsValidToken() throws Exception {
        // A valid token should allow access to the dashboard page.
        HttpResponse<String> response = get("/dashboard?token=test-token");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Miragemcp Dashboard");
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
