package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.UserMapper;
import com.ubs.billing.dto.request.CreateStaffRequest;
import com.ubs.billing.dto.request.UpdateUserRolesRequest;
import com.ubs.billing.dto.response.UserResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.Role;
import com.ubs.billing.entity.User;
import com.ubs.billing.entity.UserRole;
import com.ubs.billing.exception.BadRequestException;
import com.ubs.billing.exception.ConflictException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.RoleRepository;
import com.ubs.billing.repository.UserRepository;
import com.ubs.billing.security.CustomUserDetails;
import com.ubs.billing.util.AuditEntityNames;
import com.ubs.billing.util.AuditValueFormatter;
import com.ubs.billing.util.Constants;
import com.ubs.billing.util.PhoneUtils;
import com.ubs.billing.util.TemporaryPasswordGenerator;
import com.ubs.billing.util.UsernameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> STAFF_ROLES = Set.of(
            Constants.ROLE_OPERATOR,
            Constants.ROLE_FINANCE,
            Constants.ROLE_ADMIN
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        return userRepository.findByIdWithRoles(userDetails.getUser().getId())
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findByIdWithRoles(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> userRepository.findByIdWithRoles(user.getId()).orElse(user))
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String phoneNumber = PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ConflictException("Phone number already exists");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        if (!STAFF_ROLES.contains(role.getName())) {
            throw new BadRequestException("Only staff roles can be assigned by an administrator");
        }

        String temporaryPassword = TemporaryPasswordGenerator.generate();
        String username = resolveUniqueUsername(email);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(temporaryPassword))
                .fullName(request.getFullName().trim())
                .phoneNumber(phoneNumber)
                .emailVerified(true)
                .enabled(true)
                .firstLogin(true)
                .build();

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .build();
        user.addUserRole(userRole);

        User savedUser = userRepository.save(user);
        emailService.sendStaffWelcomeEmail(savedUser, temporaryPassword, role.getName());
        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityNames.USER,
                savedUser.getId(),
                null,
                AuditValueFormatter.formatRoles(Set.of(role.getName())));

        return UserMapper.toResponse(userRepository.findByIdWithRoles(savedUser.getId()).orElse(savedUser));
    }

    @Transactional
    public void createCustomerUserIfAbsent(String fullName, String email, String phoneNumber) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedPhone = PhoneUtils.normalizeRwandaPhone(phoneNumber);

        if (userRepository.existsByEmail(normalizedEmail)) {
            return;
        }
        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new ConflictException("Phone number already exists on another user account");
        }

        Role customerRole = roleRepository.findByName(Constants.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found: " + Constants.ROLE_CUSTOMER));

        String temporaryPassword = TemporaryPasswordGenerator.generate();
        String username = resolveUniqueUsername(normalizedEmail);

        User user = User.builder()
                .username(username)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(temporaryPassword))
                .fullName(fullName.trim())
                .phoneNumber(normalizedPhone)
                .emailVerified(true)
                .enabled(true)
                .firstLogin(true)
                .build();

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(customerRole)
                .build();
        user.addUserRole(userRole);

        User savedUser = userRepository.save(user);
        emailService.sendCustomerWelcomeEmail(savedUser, temporaryPassword);
        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityNames.USER,
                savedUser.getId(),
                null,
                AuditValueFormatter.formatRoles(Set.of(customerRole.getName())));
    }

    @Transactional
    public UserResponse updateUserRoles(Long id, UpdateUserRolesRequest request) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Set<String> requestedRoles = new LinkedHashSet<>(request.getRoles());
        if (requestedRoles.stream().anyMatch(role -> !STAFF_ROLES.contains(role))) {
            throw new BadRequestException("Only staff roles can be assigned by an administrator");
        }

        Set<String> currentRoles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (currentRoles.equals(requestedRoles)) {
            return UserMapper.toResponse(user);
        }

        user.getUserRoles().clear();

        for (String roleName : requestedRoles) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            UserRole userRole = UserRole.builder()
                    .user(user)
                    .role(role)
                    .build();
            user.addUserRole(userRole);
        }

        User savedUser = userRepository.save(user);
        emailService.sendRoleChangeEmail(savedUser, requestedRoles);
        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityNames.USER,
                savedUser.getId(),
                AuditValueFormatter.formatRoles(currentRoles),
                AuditValueFormatter.formatRoles(requestedRoles));

        return UserMapper.toResponse(userRepository.findByIdWithRoles(savedUser.getId()).orElse(savedUser));
    }

    private String resolveUniqueUsername(String email) {
        String baseUsername = UsernameGenerator.fromEmail(email);
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = UsernameGenerator.withSuffix(baseUsername, suffix++);
        }
        return username;
    }

    private CustomUserDetails getAuthenticatedUserDetails() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails;
        }
        throw new ResourceNotFoundException("Authenticated user not found");
    }

    @Transactional(readOnly = true)
    public User getAuthenticatedUserEntity() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        return userRepository.findByIdWithRoles(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
