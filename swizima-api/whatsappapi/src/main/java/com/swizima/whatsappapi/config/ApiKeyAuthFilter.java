package com.swizima.whatsappapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protects outbound WhatsApp relay endpoints with a shared API key (`X-API-Key`, `Authorization: Bearer <key>`,
 * or `?apiKey=<key>`) so Uganda backend services (`stafftrackapi`, `usersapi`) can securely invoke this
 * DigitalOcean service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final WhatsAppRelayProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/v1/whatsapp")) {
            return true;
        }
        return path.equals("/api/v1/whatsapp/health")
                || path.startsWith("/api/v1/whatsapp/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.authRequired()) {
            filterChain.doFilter(request, response);
            return;
        }

        String configuredKey = sanitizeKey(properties.serviceApiKey());
        if (configuredKey.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String source = "X-API-Key";
        String provided = request.getHeader("X-API-Key");
        if (provided == null || provided.isBlank()) {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                source = "Authorization-Bearer";
                provided = auth.substring(7);
            } else if (auth != null && !auth.isBlank()) {
                source = "Authorization";
                provided = auth;
            }
        }
        if (provided == null || provided.isBlank()) {
            String queryKey = request.getParameter("apiKey");
            if (queryKey != null && !queryKey.isBlank()) {
                source = "query-apiKey";
                provided = queryKey;
            } else {
                source = "none";
            }
        }

        String cleanProvided = sanitizeKey(provided);

        byte[] expectedBytes = configuredKey.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = cleanProvided.getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            log.warn("Rejected {} {} from {} — API key mismatch (source={}, providedLen={}, expectedLen={})",
                    request.getMethod(), request.getRequestURI(), request.getRemoteAddr(),
                    source, cleanProvided.length(), configuredKey.length());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":401,"title":"Unauthorized","detail":"Valid X-API-Key or Bearer token is required to use the WhatsApp Relay Service."}
                    """.trim());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String sanitizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.strip();
        while (s.length() >= 2 && (
                (s.startsWith("\"") && s.endsWith("\""))
                        || (s.startsWith("'") && s.endsWith("'"))
                        || (s.startsWith("`") && s.endsWith("`"))
        )) {
            s = s.substring(1, s.length() - 1).strip();
        }
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).strip();
        }
        return s;
    }
}
