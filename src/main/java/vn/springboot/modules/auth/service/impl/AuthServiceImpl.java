package vn.springboot.modules.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.modules.auth.dto.request.LoginRequest;
import vn.springboot.modules.auth.dto.request.RefreshTokenRequest;
import vn.springboot.modules.auth.dto.request.RegisterRequest;
import vn.springboot.modules.auth.dto.response.AuthResponse;
import vn.springboot.modules.user.dto.response.UserResponse;
import vn.springboot.modules.auth.entity.RefreshToken;
import vn.springboot.modules.user.entity.Role;
import vn.springboot.modules.user.entity.User;
import vn.springboot.modules.user.mapper.UserMapper;
import vn.springboot.modules.auth.repository.RefreshTokenRepository;
import vn.springboot.modules.user.repository.RoleRepository;
import vn.springboot.modules.user.repository.UserRepository;
import vn.springboot.security.CustomUserDetails;
import vn.springboot.security.JwtProperties;
import vn.springboot.security.jwt.JwtService;
import vn.springboot.modules.auth.service.AuthService;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .enabled(true)
                .roles(Set.of(defaultRole))
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return issueTokens(principal.getUser(), principal);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (stored.isRevoked()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }
        if (stored.isExpired()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // Rotate: revoke the presented token before issuing a fresh pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return issueTokens(user, new CustomUserDetails(user));
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userMapper.toResponse(principal.getUser());
    }

    private AuthResponse issueTokens(User user, CustomUserDetails principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .user(userMapper.toResponse(user))
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }
}
