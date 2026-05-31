package com.hethongtrongbanking.nienluancosonganh.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hethongtrongbanking.nienluancosonganh.dto.request.LoginRequest;
import com.hethongtrongbanking.nienluancosonganh.dto.request.RegisterRequest;
import com.hethongtrongbanking.nienluancosonganh.dto.response.AuthResponse;
import com.hethongtrongbanking.nienluancosonganh.entity.User;
import com.hethongtrongbanking.nienluancosonganh.enums.Role;
import com.hethongtrongbanking.nienluancosonganh.exception.BankingException;
import com.hethongtrongbanking.nienluancosonganh.exception.ErrorCode;
import com.hethongtrongbanking.nienluancosonganh.repository.UserRepository;
import com.hethongtrongbanking.nienluancosonganh.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Dang ky tai khoan moi (mac dinh role = VIEWER)
     */
    public AuthResponse register(RegisterRequest request) {
        // Kiem tra username da ton tai chua
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BankingException(ErrorCode.INVALID_INPUT);
        }

        // Kiem tra email da ton tai chua
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BankingException(ErrorCode.INVALID_INPUT);
        }

        // Tao user moi
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(Role.VIEWER) // Mac dinh la VIEWER (khach hang)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("User registered: {}", user.getUsername());

        // Tu dong login sau khi register
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("Dang ky thanh cong!")
                .build();
    }

    /**
     * Dang nhap va tra ve JWT token
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        User user = (User) authentication.getPrincipal();
        log.info("User logged in: {} (role={})", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("Dang nhap thanh cong!")
                .build();
    }
}
