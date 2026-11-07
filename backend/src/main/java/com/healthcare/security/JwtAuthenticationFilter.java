package com.healthcare.security;

import com.healthcare.entity.User;
import com.healthcare.exception.ApiErrorWriter;
import com.healthcare.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request from the HttpOnly session cookie or an "Authorization: Bearer" header.
 *
 * CSRF defence for cookie authentication: SameSite cookies + strict CORS + the requirement that every
 * state-changing, cookie-authenticated request carries "X-Requested-With: XMLHttpRequest". Browsers cannot
 * add a custom header to a cross-site form post, and a cross-origin fetch with that header needs a CORS
 * pre-flight that only the configured front-end origin passes. Bearer-token callers (Postman, scripts)
 * are not affected because browsers never attach those automatically.
 *
 * The user is re-loaded on every request, so a deleted account stops working immediately.
 * This filter is created by SecurityConfig and is deliberately NOT a Spring bean (to avoid double registration).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final JwtService jwtService;
    private final UserRepository users;
    private final ApiErrorWriter errors;
    private final String cookieName;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users, ApiErrorWriter errors,
                                   String cookieName) {
        this.jwtService = jwtService;
        this.users = users;
        this.errors = errors;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = null;
        boolean fromCookie = false;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        } else if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (cookieName.equals(c.getName())) {
                    token = c.getValue();
                    fromCookie = true;
                }
            }
        }

        if (token != null && !token.isBlank()) {
            Optional<Claims> claims = jwtService.parse(token);
            if (claims.isPresent()) {
                if (fromCookie && !SAFE_METHODS.contains(request.getMethod())
                        && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    errors.write(request, response, HttpStatus.FORBIDDEN, "CSRF_CHECK_FAILED",
                            "Missing X-Requested-With header");
                    return;
                }
                authenticate(claims.get(), request);
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        try {
            Long id = Long.valueOf(claims.getSubject());
            Optional<User> user = users.findById(id);
            if (user.isPresent()) {
                User u = user.get();
                AuthUser principal = new AuthUser(u.getId(), u.getEmail(), u.getRole());
                // The role comes from the database, not from the token, so a role change takes effect at once.
                var auth = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (NumberFormatException ignored) {
            // malformed subject: treat as unauthenticated
        }
    }
}
