package com.hethongtrongbanking.nienluancosonganh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.hethongtrongbanking.nienluancosonganh.dto.request.LoginRequest;
import com.hethongtrongbanking.nienluancosonganh.dto.request.RegisterRequest;
import com.hethongtrongbanking.nienluancosonganh.dto.response.AuthResponse;
import com.hethongtrongbanking.nienluancosonganh.entity.User;
import com.hethongtrongbanking.nienluancosonganh.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "isActive", user.isActive()));
    }
}
