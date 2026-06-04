package com.empresa.chat.service.auth;

import com.empresa.chat.domain.model.User;
import com.empresa.chat.dto.request.LoginRequest;
import com.empresa.chat.dto.request.RegisterRequest;
import com.empresa.chat.dto.response.AuthResponse;
import com.empresa.chat.exception.BusinessException;
import com.empresa.chat.repository.UserRepository;
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
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("Email já cadastrado: " + req.email());
        }
        var user = User.builder()
                .nome(req.nome())
                .email(req.email())
                .senha(passwordEncoder.encode(req.senha()))
                .role(req.role())
                .build();
        userRepository.save(user);
        return new AuthResponse(jwtService.gerarToken(user), user.getId(), user.getNome(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.senha()));
        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        return new AuthResponse(jwtService.gerarToken(user), user.getId(), user.getNome(), user.getRole().name());
    }
}
