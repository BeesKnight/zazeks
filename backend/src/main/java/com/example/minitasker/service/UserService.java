package com.example.minitasker.service;

import com.example.minitasker.dto.PageResponse;
import com.example.minitasker.dto.auth.UpdateUserRequest;
import com.example.minitasker.dto.auth.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {
    PageResponse<UserResponse> listUsers(Pageable pageable);
    UserResponse getUser(Long id);
    UserResponse updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
}
