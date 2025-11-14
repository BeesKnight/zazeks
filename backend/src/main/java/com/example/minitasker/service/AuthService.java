package com.example.minitasker.service;

import com.example.minitasker.dto.auth.AuthResponse;
import com.example.minitasker.dto.auth.LoginRequest;
import com.example.minitasker.dto.auth.RegisterRequest;
import com.example.minitasker.dto.auth.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser();
}
