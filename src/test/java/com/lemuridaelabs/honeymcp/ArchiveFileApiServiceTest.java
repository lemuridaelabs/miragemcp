package com.lemuridaelabs.honeymcp;

import com.lemuridaelabs.honeymcp.modules.archives.dto.ArchiveFileRecord;
import com.lemuridaelabs.honeymcp.modules.archives.dto.ArchiveFileSummary;
import com.lemuridaelabs.honeymcp.modules.archives.service.ArchiveCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ArchiveFileApiServiceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ArchiveCacheService archiveCacheService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void returnsStubbedFileResponse() throws Exception {
        // When a cached file exists, the API should return a stub response with realistic headers.
        var record = ArchiveFileRecord.builder()
                .id("file-123")
                .name("report.txt")
                .size(123L)
                .build();

        var summary = ArchiveFileSummary.builder()
                .archiveName("finance")
                .count(1)
                .files(List.of(record))
                .build();

        archiveCacheService.storeArchiveFileSummary("127.0.0.1", summary);

        HttpResponse<byte[]> response = getBytes("/archive/files/file-123");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(HttpHeaders.CONTENT_DISPOSITION).orElse(null))
                .isEqualTo("attachment; filename=\"report.txt\"");
        assertThat(response.headers().firstValue(HttpHeaders.CONTENT_LENGTH).orElse("")).isEqualTo("0");
        assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(""))
                .startsWith("application/octet-stream");
        assertThat(response.body()).isNotNull();
        assertThat(response.body().length).isEqualTo(0);
    }

    @Test
    void returnsNotFoundForUnknownFile() throws Exception {
        // Unknown IDs should return a 404 without a body.
        HttpResponse<byte[]> response = getBytes("/archive/files/unknown-id");
        assertThat(response.statusCode()).isEqualTo(404);
    }

    private HttpResponse<byte[]> getBytes(String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
}
