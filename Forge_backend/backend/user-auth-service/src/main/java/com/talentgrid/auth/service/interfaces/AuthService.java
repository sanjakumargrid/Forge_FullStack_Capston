package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.request.LoginRequest;
import com.talentgrid.auth.dto.request.RegisterRequest;
import com.talentgrid.auth.dto.response.LoginResponse;
import com.talentgrid.auth.dto.response.RegisterResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    LoginResponse login(
            LoginRequest request,
            HttpServletResponse response
    );

    LoginResponse refreshToken(String refreshToken, HttpServletResponse response);

    RegisterResponse register(RegisterRequest request);
}