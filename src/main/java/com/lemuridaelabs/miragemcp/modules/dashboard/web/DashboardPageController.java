package com.lemuridaelabs.miragemcp.modules.dashboard.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for the security dashboard UI.
 *
 * <p>Serves the Thymeleaf-rendered dashboard page for monitoring honeypot activity.
 * Access is protected by the {@code DashboardAccessInterceptor} which requires a valid
 * token parameter. Invalid access attempts return a 404 response to hide the dashboard's
 * existence from attackers.</p>
 *
 * @see com.lemuridaelabs.miragemcp.interceptors.DashboardAccessInterceptor
 * @since 1.0
 */
@Controller
@Slf4j
public class DashboardPageController {

    /**
     * Handles GET requests to the "/dashboard" URL and returns the view name
     * for the dashboard index page. Note this is protected by the DashboardAccessInterceptor.
     * Attempting to view the dashboard page without a valid token will return a 404 not found
     * to hide the information.
     *
     * @return the name of the view representing the dashboard index page
     */
    @GetMapping("/dashboard")
    public String dashboardIndex() {
        return "dashboard/index";
    }

}
