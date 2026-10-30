package com.swizima.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Converts "downstream service unreachable / timed out" failures into a clean
 * {@code 503 Service Unavailable} RFC 7807 problem response instead of a generic 500.
 * Runs inside {@link RequestIdFilter} so the response still carries {@code X-Request-Id}.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class DownstreamErrorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (ServletException | RuntimeException ex) {
            if (!isDownstreamUnavailable(ex) || response.isCommitted()) {
                throw ex;
            }
            log.warn("Downstream unavailable for {} {}: {}", request.getMethod(), request.getRequestURI(),
                    rootMessage(ex));
            writeProblem(request, response);
        }
    }

    private static boolean isDownstreamUnavailable(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ResourceAccessException) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
    }

    private static void writeProblem(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.resetBuffer();
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("""
                {"type":"about:blank","title":"Service Unavailable","status":503,\
                "detail":"The requested service is currently unavailable. Please try again later.",\
                "instance":"%s","timestamp":"%s"}""".formatted(path, Instant.now()));
    }
}
