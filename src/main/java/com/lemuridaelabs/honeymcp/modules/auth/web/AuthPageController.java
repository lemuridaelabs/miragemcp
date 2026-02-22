package com.lemuridaelabs.honeymcp.modules.auth.web;

import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.honeymcp.modules.events.service.EventLoggingService;
import com.lemuridaelabs.honeymcp.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web controller for authentication pages (honeypot login forms).
 *
 * <p>Serves fake login pages designed to capture credential harvesting attempts.
 * All login submissions are logged as medium-severity events with the captured
 * username and password for threat intelligence purposes. Login attempts always
 * fail with an error message to encourage repeated attempts.</p>
 *
 * @see EventLoggingService
 * @since 1.0
 */
@RequiredArgsConstructor
@Controller
@Slf4j
public class AuthPageController {

    private final EventLoggingService eventLoggingService;


    /**
     * Handles the GET request for the login page.
     * Logs the user action and returns the path to the login view template.
     *
     * @return The path to the login page template as a String.
     */
    @GetMapping("/auth/login")
    public String loginPage() {
        log.info("User has requested a login page..");
        return "/auth/login";
    }


    /**
     * Handles the login page request by processing the username and password provided by the user.
     * Logs the login attempt and redirects to the login page with an error message.
     *
     * @param request  the http servlet request
     * @param username the username provided by the user
     * @param password the password provided by the user
     * @return a redirect view to the login page with an error message
     */
    @PostMapping("/auth/login")
    public String handleLoginPage(HttpServletRequest request,
                                  @RequestParam String username, @RequestParam String password) {
        log.info("User has submitted a login request, username={}, password={}..", username, password);

        eventLoggingService.mediumEvent(
                RequestUtils.getEffectiveRemoteIp(request),
                request.getRequestURI(),
                HoneyEventType.HTTP,
                false,
                String.format("Attempted Application Login for username=%s and password=%s", username, password),
                username + ":" + password);

        return "redirect:/auth/login?error=Invalid username or password";
    }

}
