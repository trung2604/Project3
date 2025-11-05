package com.project3.userservice.service;

import com.project3.userservice.dto.CreateUserRequestDTO;
import com.project3.userservice.dto.LoginRequestDTO;
import com.project3.userservice.dto.RegisterUserRequestDTO;
import com.project3.userservice.dto.LoginResponseDTO;
import com.project3.userservice.dto.PagedUserResponseDTO;
import com.project3.userservice.dto.UpdateUserRequestDTO;
import com.project3.userservice.dto.UpdateAvatarRequestDTO;
import com.project3.userservice.dto.ChangePasswordRequestDTO;
import com.project3.userservice.dto.identity.OAuthCodeExchangeRequest;
import com.project3.userservice.dto.identity.TokenExchangeResponse;
import com.project3.userservice.dto.UserResponseDTO;
import com.project3.userservice.entity.User;

public interface IUserService {
    UserResponseDTO createUser(CreateUserRequestDTO request);
    UserResponseDTO registerUser(RegisterUserRequestDTO request);
    LoginResponseDTO login(LoginRequestDTO request);
    UserResponseDTO updateUser(String userId, UpdateUserRequestDTO request);
    void deleteUser(String userId);
    UserResponseDTO getUserById(String userId);
    UserResponseDTO getUserByEmail(String email);
    PagedUserResponseDTO getAllUsers(Integer page, Integer size, String search, User.UserRole role, User.UserStatus status);
    UserResponseDTO toggleUserStatus(String userId, User.UserStatus status);
    UserResponseDTO syncEmailVerification(String userId);
    UserResponseDTO updateUserAvatar(String userId, UpdateAvatarRequestDTO request);
    UserResponseDTO updateMyProfile(String userId, UpdateUserRequestDTO request);
    void changeMyPassword(String userId, ChangePasswordRequestDTO request);
    TokenExchangeResponse exchangeAuthorizationCode(OAuthCodeExchangeRequest request);
}
