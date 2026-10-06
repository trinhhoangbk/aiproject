package com.mbs.hub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * PLAN-034 — populates the {@code requestId} MDC slot that {@code logback-spring.xml}
 * renders as the {@code "req"} JSON field. Honours an inbound {@code X-Request-Id}
 * header for cross-system tracing when the operator fronts the Hub with a tunnel
 * that forwards one; otherwise generates a UUID per request.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY     = "requestId";
    public static final String HEADER      = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = req.getHeader(HEADER);
        if (id == null || id.isBlank() || id.length() > 128) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, id);
        res.setHeader(HEADER, id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
