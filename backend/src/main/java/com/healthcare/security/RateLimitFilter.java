package com.healthcare.security;

import com.healthcare.config.AppProperties;
import com.healthcare.exception.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window, in-memory rate limiter per client address for login/registration and prediction requests.
 * It protects against password guessing and abuse of the (expensive) AI call on a single instance. When running
 * several instances, or behind a proxy, use a shared limiter (gateway / Redis) - see docs/deployment notes.
 * Created by SecurityConfig; intentionally not a Spring bean.
 */
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 60_000L;

    private final AppProperties.RateLimit config;
    private final ApiErrorWriter errors;
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>(); // {windowStart, count}

    public RateLimitFilter(AppProperties.RateLimit config, ApiErrorWriter errors) {
        this.config = config;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String bucket = config.enabled() ? bucketFor(request) : null;
        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }
        int limit = bucket.equals("auth") ? config.authPerMinute() : config.predictionPerMinute();
        long now = System.currentTimeMillis();
        long[] window = windows.compute(bucket + "|" + request.getRemoteAddr(), (k, v) -> {
            if (v == null || now - v[0] >= WINDOW_MS) {
                return new long[]{now, 1};
            }
            v[1]++;
            return v;
        });
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> now - e.getValue()[0] >= WINDOW_MS);
        }
        if (window[1] > limit) {
            long retryAfter = Math.max(1, (WINDOW_MS - (now - window[0])) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            errors.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Too many requests. Please try again later.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String bucketFor(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            return "auth";
        }
        if (path.equals("/api/predictions")) {
            return "prediction";
        }
        return null;
    }
}
