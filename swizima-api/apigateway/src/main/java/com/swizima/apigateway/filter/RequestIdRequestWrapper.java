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
        return RequestIdFilter.HEADER.equalsIgnoreCase(name) ? requestId : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return RequestIdFilter.HEADER.equalsIgnoreCase(name)
                ? Collections.enumeration(Set.of(requestId))
                : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
        names.add(RequestIdFilter.HEADER);
        return Collections.enumeration(names);
    }
}
