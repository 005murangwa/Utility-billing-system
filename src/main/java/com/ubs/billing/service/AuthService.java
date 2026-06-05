package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.UserMapper;
import com.ubs.billing.dto.request.ChangeTemporaryPasswordRequest;
import com.ubs.billing.dto.request.LoginRequest;
import com.ubs.billing.dto.request.LogoutRequest;
import com.ubs.billing.dto.request.RefreshTokenRequest;
import com.ubs.billing.dto.request.RegisterRequest;
import com.ubs.billing.dto.request.ResendOtpRequest;
import com.ubs.billing.dto.request.VerifyOtpRequest;
import com.ubs.billing.dto.response.AuthResponse;
import com.ubs.billing.dto.response.MessageResponse;
import com.ubs.billing.dto.response.RegisterResponse;
import com.ubs.billing.dto.response.UserResponse;
import com.ubs.billing.entity.OtpType;
import com.ubs.billing.entity.RefreshToken;
import com.ubs.billing.entity.Role;
import com.ubs.billing.entity.User;
import com.ubs.billing.entity.UserRole;
import com.ubs.billing.exception.AccountNotVerifiedException;
import com.ubs.billing.exception.BadRequestException;
import com.ubs.billing.exception.ConflictException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.exception.UnauthorizedException;
import com.ubs.billing.repository.RoleRepository;
import com.ubs.billing.repository.UserRepository;
import com.ubs.billing.security.CustomUserDetails;
import com.ubs.billing.security.JwtService;
import com.ubs.billing.util.Constants;
import com.ubs.billing.util.PhoneUtils;
import com.ubs.billing.util.UsernameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserService userService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String phoneNumber = PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ConflictException("Phone number already exists");
        }

        String username = resolveUsername(request, email);

        Role customerRole = roleRepository.findByName(Constants.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found: " + Constants.ROLE_CUSTOMER));

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phoneNumber(phoneNumber)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .emailVerified(false)
                .enabled(false)
                .firstLogin(false)
                .build();

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(customerRole)
                .build();
        user.addUserRole(userRole);

        User savedUser = userRepository.save(user);
        otpService.createAndSendOtp(savedUser, OtpType.EMAIL_VERIFICATION);

        return RegisterResponse.builder()
                .email(savedUser.getEmail())
                .verificationRequired(true)
                .message("Registration successful. Please verify your email using the OTP sent to your inbox.")
                .build();
    }

    @Transactional
    public MessageResponse verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Account is already verified");
        }

        otpService.verifyOtp(user, request.getOtp(), OtpType.EMAIL_VERIFICATION);

        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Email verified successfully. Your account is now active. You may log in.")
                .build();
    }

    @Transactional
    public MessageResponse resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Account is already verified");
        }

        otpService.createAndSendOtp(user, OtpType.EMAIL_VERIFICATION);

        return MessageResponse.builder()
                .message("A new OTP has been sent to your email address.")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = resolveLoginIdentifier(request);
        User user = findUserByEmailOrUsername(identifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AccountNotVerifiedException("Please verify your email before logging in");
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadRequestException("Account is disabled");
        }

        User userWithRoles = userRepository.findByIdWithRoles(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(new CustomUserDetails(userWithRoles), userWithRoles);
    }

    @Transactional
    public AuthResponse changeTemporaryPassword(ChangeTemporaryPasswordRequest request) {
        User user = userService.getAuthenticatedUserEntity();

        if (!Boolean.TRUE.equals(user.getFirstLogin())) {
            throw new BadRequestException("Password change is not required for this account");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);
        User savedUser = userRepository.save(user);

        User userWithRoles = userRepository.findByIdWithRoles(savedUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(new CustomUserDetails(userWithRoles), userWithRoles);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        refreshTokenService.revokeToken(request.getRefreshToken());

        User user = userRepository.findByIdWithRoles(refreshToken.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AccountNotVerifiedException("Please verify your email before logging in");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        return buildAuthResponse(userDetails, user);
    }

    @Transactional
    public MessageResponse logout(String accessToken, LogoutRequest request) {
        if (!StringUtils.hasText(accessToken)) {
            throw new UnauthorizedException("Access token is required for logout");
        }

        if (!jwtService.isAccessToken(accessToken)) {
            throw new UnauthorizedException("Invalid access token");
        }

        jwtBlacklistService.blacklistToken(accessToken);

        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            refreshTokenService.revokeToken(request.getRefreshToken());
        }

        jwtBlacklistService.purgeExpiredEntries();

        return MessageResponse.builder()
                .message("Logged out successfully")
                .build();
    }

    private AuthResponse buildAuthResponse(CustomUserDetails userDetails, User user) {
        boolean passwordChangeRequired = Boolean.TRUE.equals(user.getFirstLogin());
        String accessToken = jwtService.generateAccessToken(userDetails, passwordChangeRequired);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        UserResponse userResponse = UserMapper.toResponse(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .refreshExpiresIn(refreshTokenService.getRefreshExpirationMs())
                .passwordChangeRequired(passwordChangeRequired)
                .user(userResponse)
                .build();
    }

    private String resolveUsername(RegisterRequest request, String email) {
        if (StringUtils.hasText(request.getUsername())) {
            String username = request.getUsername().trim();
            if (userRepository.existsByUsername(username)) {
                throw new ConflictException("Username already exists");
            }
            return username;
        }

        String baseUsername = UsernameGenerator.fromEmail(email);
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = UsernameGenerator.withSuffix(baseUsername, suffix++);
        }
        return username;
    }

    private String resolveLoginIdentifier(LoginRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            String value = request.getEmail().trim();
            return value.contains("@") ? value.toLowerCase() : value;
        }
        if (StringUtils.hasText(request.getUsername())) {
            return request.getUsername().trim();
        }
        throw new BadRequestException("Email is required");
    }

    private java.util.Optional<User> findUserByEmailOrUsername(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier);
        }
        return userRepository.findByUsername(identifier);
    }
}
