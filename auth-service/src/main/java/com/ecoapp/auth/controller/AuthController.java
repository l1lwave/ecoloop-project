package com.ecoapp.auth.controller;

import com.ecoapp.auth.dto.AuthResponse;
import com.ecoapp.auth.dto.LoginRequest;
import com.ecoapp.auth.dto.RegisterRequest;
import com.ecoapp.auth.dto.UserResponse;
import com.ecoapp.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Deliberately NOT reading the X-User-Id header here — this service independently
    // verifies the JWT itself (see JwtAuthFilter/SecurityConfig), so the authenticated
    // principal's name (== userId, set in UserDetailsServiceImpl) is the trustworthy
    // source of identity, not a header a client could otherwise attempt to spoof.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getProfile(authentication.getName()));
    }
}
