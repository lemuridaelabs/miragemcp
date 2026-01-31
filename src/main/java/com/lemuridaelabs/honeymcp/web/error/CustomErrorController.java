package com.lemuridaelabs.honeymcp.web.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class CustomErrorController implements ErrorController {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        Integer statusCode = null;
        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
        }

        // Add common attributes
        model.addAttribute("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        model.addAttribute("path", requestUri != null ? requestUri.toString() : request.getRequestURI());

        if (statusCode != null) {
            model.addAttribute("status", statusCode);

            // Get HttpStatus for better error description
            try {
                HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
                model.addAttribute("error", httpStatus.getReasonPhrase());

                // Add custom message based on status code
                if (message == null || message.toString().isEmpty()) {
                    message = getDefaultMessage(statusCode);
                }
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", "Unknown Error");
            }
        } else {
            model.addAttribute("error", "Unknown Error");
            if (message == null) {
                message = "An unexpected error occurred.";
            }
        }

        model.addAttribute("message", message);

        // Route to specific error page based on status code
        if (statusCode != null) {
            if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            } else if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode >= 500) {
                return "error/5xx";
            }
        }

        // Default error page
        return "error/error";
    }

    private String getDefaultMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "The request could not be understood or was missing required parameters.";
            case 401 -> "Authentication is required to access this resource.";
            case 403 -> "You do not have permission to access this resource.";
            case 404 -> "The requested resource could not be found.";
            case 405 -> "The request method is not supported for this resource.";
            case 500 -> "An internal server error occurred while processing your request.";
            case 502 -> "The server received an invalid response from an upstream server.";
            case 503 -> "The service is temporarily unavailable. Please try again later.";
            default -> "An error occurred while processing your request.";
        };
    }
}
