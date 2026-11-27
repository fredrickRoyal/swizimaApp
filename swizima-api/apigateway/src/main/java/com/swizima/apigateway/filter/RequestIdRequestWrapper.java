package com.swizima.apigateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Request wrapper that exposes a (possibly generated) X-Request-Id header so the gateway
 * forwards it to downstream services.
 */
class RequestIdRequestWrapper extends HttpServletRequestWrapper {

    private final String requestId;

    RequestIdRequestWrapper(HttpServletRequest request, String requestId) {
        super(request);
        this.requestId = requestId;
    }

    @Override
    public String getHeader(String name) {
        if (RequestIdFilter.HEADER.equalsIgnoreCase(name)) {
            return requestId;
        }
        // CorsFilter runs BEFORE RequestIdFilter at Ordered.HIGHEST_PRECEDENCE, so gateway CORS
        // headers are already handled; stripping Origin here prevents downstream Spring MVC
        // DefaultCorsProcessor from rejecting proxied POST/PUT/DELETE requests with 403.
        if ("Origin".equalsIgnoreCase(name)) {
            return null;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (RequestIdFilter.HEADER.equalsIgnoreCase(name)) {
            return Collections.enumeration(Set.of(requestId));
        }
        if ("Origin".equalsIgnoreCase(name)) {
            return Collections.emptyEnumeration();
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
        names.removeIf("Origin"::equalsIgnoreCase);
        names.add(RequestIdFilter.HEADER);
        return Collections.enumeration(names);
    }
}
