package com.healthcare.controller;

import com.healthcare.dto.AuthDtos.AuthResponse;
import com.healthcare.dto.AuthDtos.LoginRequest;
import com.healthcare.dto.AuthDtos.RegisterRequest;
import com.healthcare.dto.AuthDtos.UserDto;
import com.healthcare.security.AuthCookieService;
import com.healthcare.security.AuthUser;
import com.healthcare.service.AuthService;
import com.healthcare.service.AuthService.AuthResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService cookies;

    public AuthController(AuthService authService, AuthCookieService cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                                 HttpServletResponse response) {
        AuthResult result = authService.register(req);
        cookies.write(response, result.token());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(result.user()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        AuthResult result = authService.login(req);
        cookies.write(response, result.token());
        return new AuthResponse(result.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        cookies.clear(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthUser user) {
        return authService.me(user.id());
    }
}
