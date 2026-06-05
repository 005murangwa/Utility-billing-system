package com.ubs.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Boolean emailVerified;
    private Boolean firstLogin;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
