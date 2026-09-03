package com.avsmc.procurement.identity.service;

import com.avsmc.procurement.identity.dto.*;
import com.avsmc.procurement.identity.entity.*;
import com.avsmc.procurement.identity.repository.*;
import com.avsmc.procurement.security.JwtTokenProvider;
import com.avsmc.procurement.security.SecurityUtils;
import com.avsmc.procurement.shared.exception.BusinessRuleException;
import com.avsmc.procurement.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setLockedUntil(Instant.now().plusSeconds(900));
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new LockedException("Account is locked");
        }

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        Organisation org = organisationRepository.findById(user.getOrganisationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", user.getOrganisationId()));

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

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .build();

        List<Role> roles = roleRepository.findAllById(request.getRoleIds());
        user.setRoles(new java.util.HashSet<>(roles));

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
