package com.avsmc.procurement.identity.service;

import com.avsmc.procurement.identity.dto.*;
import com.avsmc.procurement.identity.dto.UpdateUserRequest;
import com.avsmc.procurement.identity.entity.*;
import com.avsmc.procurement.identity.repository.*;
import com.avsmc.procurement.security.JwtTokenProvider;
import com.avsmc.procurement.security.SecurityUtils;
import com.avsmc.procurement.shared.exception.BusinessRuleException;
import com.avsmc.procurement.shared.exception.ResourceNotFoundException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityUtils securityUtils;
    private final LoginFailureRecorder loginFailureRecorder;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private long lockoutDurationMinutes;

    @Value("${app.security.password-min-length:8}")
    private int passwordMinLength;

    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check lockout before burning a bcrypt comparison, so locked accounts
        // cannot extend the lock window with further guesses.
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new LockedException("Account is locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Recorded in a REQUIRES_NEW transaction: the BadCredentialsException
            // below rolls back the caller's transaction, but the attempt counter
            // and lockout must persist.
            loginFailureRecorder.registerFailure(user, maxLoginAttempts, lockoutDurationMinutes);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UUID userId;
        try {
            userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        User user = userRepository.findByIdWithRoles(securityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", securityUtils.currentUserId()));
        return toUserDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already registered: " + request.getEmail());
        }
        if (request.getPassword() == null || request.getPassword().length() < passwordMinLength) {
            throw new BusinessRuleException("Password must be at least " + passwordMinLength + " characters");
        }

        UUID orgId = securityUtils.currentOrgId();
        organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", orgId));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .build();

        List<Role> roles = roleRepository.findAllById(request.getRoleIds());
        if (roles.size() != request.getRoleIds().size()) {
            throw new BusinessRuleException("One or more roles do not exist");
        }
        for (Role role : roles) {
            if (!orgId.equals(role.getOrganisationId())) {
                throw new BusinessRuleException("Role does not belong to your organisation: " + role.getCode());
            }
        }
        user.setRoles(new java.util.HashSet<>(roles));

        user.setOrganisationId(orgId);
        user = userRepository.save(user);
        return toUserDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toUserDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        return roleRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toRoleDto).collect(Collectors.toList());
    }

    @Transactional
    public AuthResponse loginWithOtp(UUID userId, UUID otpId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessRuleException("Email already registered: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        if (request.getMfaEnabled() != null) user.setMfaEnabled(request.getMfaEnabled());

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(request.getRoleIds());
            if (roles.size() != request.getRoleIds().size()) {
                throw new BusinessRuleException("One or more roles do not exist");
            }
            UUID orgId = user.getOrganisationId();
            for (Role role : roles) {
                if (!orgId.equals(role.getOrganisationId())) {
                    throw new BusinessRuleException("Role does not belong to user's organisation: " + role.getCode());
                }
            }
            user.setRoles(new java.util.HashSet<>(roles));
        }

        user = userRepository.save(user);
        return toUserDto(user);
    }

    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setIsActive(false);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Organisation org = organisationRepository.findById(user.getOrganisationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", user.getOrganisationId()));

        if (user.getRoles().isEmpty()) {
            throw new BadCredentialsException("User has no assigned role");
        }
        Role primaryRole = user.getRoles().iterator().next();
        List<String> permissions = primaryRole.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toList());

        String accessToken = jwtTokenProvider.generateToken(user.getId(), org.getId(), primaryRole.getCode());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .organisationId(org.getId())
                .organisationName(org.getName())
                .orgType(org.getOrgType())
                .roleCode(primaryRole.getCode())
                .roleName(primaryRole.getName())
                .permissions(permissions)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .mfaEnabled(user.getMfaEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .organisationId(user.getOrganisationId())
                .roles(user.getRoles().stream().map(this::toRoleDto).collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private RoleDto toRoleDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(Permission::getCode).collect(Collectors.toList()))
                .build();
    }
}
