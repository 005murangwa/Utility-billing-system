package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.UserResponse;
import com.ubs.billing.entity.User;
import com.ubs.billing.entity.UserRole;

import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        Set<String> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getEnabled())
                .emailVerified(user.getEmailVerified())
                .firstLogin(user.getFirstLogin())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
