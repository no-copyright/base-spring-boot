package vn.springboot.modules.auth.service;

import vn.springboot.modules.auth.dto.request.LoginRequest;
import vn.springboot.modules.auth.dto.request.RefreshTokenRequest;
import vn.springboot.modules.auth.dto.request.RegisterRequest;
import vn.springboot.modules.auth.dto.response.AuthResponse;
import vn.springboot.modules.user.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    UserResponse getCurrentUser();
}
