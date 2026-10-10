package com.akhil.tripdesk.service;

import com.akhil.tripdesk.dto.AuthDtos.AuthResponse;
import com.akhil.tripdesk.dto.AuthDtos.LoginRequest;
import com.akhil.tripdesk.dto.AuthDtos.RegisterRequest;
import com.akhil.tripdesk.entity.Role;
import com.akhil.tripdesk.entity.User;
import com.akhil.tripdesk.exception.BookingConflictException;
import com.akhil.tripdesk.repository.UserRepository;
import com.akhil.tripdesk.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BookingConflictException("An account with this email already exists");
        }
        User user = userRepository.save(User.builder()
                .name(req.name().trim())
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .role(Role.USER)
                .build());
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        User user = userRepository.findByEmail(email).orElseThrow();
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        var principal = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword()).roles(user.getRole().name()).build();
        String token = jwtService.generateToken(principal, java.util.Map.of("role", user.getRole().name()));
        return new AuthResponse(token, "Bearer", user.getId(), user.getName(), user.getRole().name());
    }
}
