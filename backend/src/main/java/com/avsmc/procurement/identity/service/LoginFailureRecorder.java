package com.avsmc.procurement.identity.service;

import com.avsmc.procurement.identity.entity.User;
import com.avsmc.procurement.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Persists failed-login counters in an independent transaction so they
 * survive the rollback of the caller's transaction (login throws
 * BadCredentialsException, which would otherwise discard the counter update
 * and the lockout with it).
 */
@Service
@RequiredArgsConstructor
public class LoginFailureRecorder {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(User user, int maxAttempts, long lockoutDurationMinutes) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= maxAttempts) {
            user.setLockedUntil(Instant.now().plusSeconds(lockoutDurationMinutes * 60));
        }
        userRepository.save(user);
    }
}
