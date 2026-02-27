package com.lemuridaelabs.miragemcp.modules.dashboard.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to serve the service worker file through the dashboard authentication interceptor.
 * This ensures the service worker is only accessible with a valid dashboard token.
 * <p>
 * Service workers must be served from the same origin and path they control,
 * so this endpoint serves the sw.js file under /dashboard/js/ path.
 */
@Controller
@RequestMapping("/dashboard/js")
public class ServiceWorkerController {

    @GetMapping(value = "/sw.js", produces = "application/javascript")
    public ResponseEntity<String> getServiceWorker() throws IOException {
        var resource = new ClassPathResource("static/dashboard/js/sw.js");
        var content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/javascript")
                // Service worker specific headers
                .header("Service-Worker-Allowed", "/dashboard/")
                .body(content);
    }
}
