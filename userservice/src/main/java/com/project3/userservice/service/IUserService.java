package com.project3.userservice.service;

import com.project3.userservice.dto.CreateUserRequestDTO;
import com.project3.userservice.dto.LoginRequestDTO;
import com.project3.userservice.dto.LoginResponseDTO;
import com.project3.userservice.dto.PagedUserResponseDTO;
import com.project3.userservice.dto.UpdateUserRequestDTO;
import com.project3.userservice.dto.UserResponseDTO;
import com.project3.userservice.entity.User;

public interface IUserService {
    UserResponseDTO createUser(CreateUserRequestDTO request);
    LoginResponseDTO login(LoginRequestDTO request);
    UserResponseDTO updateUser(String userId, UpdateUserRequestDTO request);
    void deleteUser(String userId);
    UserResponseDTO getUserById(String userId);
    UserResponseDTO getUserByEmail(String email);
    PagedUserResponseDTO getAllUsers(Integer page, Integer size, String search, User.UserRole role, User.UserStatus status);
    UserResponseDTO toggleUserStatus(String userId, User.UserStatus status);
}
