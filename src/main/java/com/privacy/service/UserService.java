package com.privacy.service;

import com.privacy.dto.CreateUserRequest;
import com.privacy.dto.RegisterRequest;
import com.privacy.dto.UserResponse;
import com.privacy.entity.User;
import com.privacy.exception.BadRequestException;
import com.privacy.exception.ConflictException;
import com.privacy.exception.ResourceNotFoundException;
import com.privacy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email address is already registered");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.USER)
                .enabled(true)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse createUserByAdmin(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email address is already registered");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .enabled(true)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUserByAdmin(Long id, String currentAdminUsername) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Safeguard 1: Admin cannot delete their own account
        if (target.getUsername().equalsIgnoreCase(currentAdminUsername)) {
            throw new BadRequestException("You cannot delete your own administrative account");
        }

        // Safeguard 2: Last remaining ADMIN cannot be deleted
        if (target.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot delete the last remaining administrator account");
            }
        }

        // Cascade delete removes all associated records via JPA CascadeType.ALL & orphanRemoval
        userRepository.delete(target);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, User.Role newRole, String currentAdminUsername) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Safeguard: Last remaining ADMIN cannot be demoted
        if (target.getRole() == User.Role.ADMIN && newRole == User.Role.USER) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot demote the last remaining administrator account");
            }
        }

        target.setRole(newRole);
        return UserResponse.from(userRepository.save(target));
    }

    @Transactional
    public UserResponse toggleUserStatus(Long id, String currentAdminUsername) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Safeguard: Cannot deactivate oneself
        if (target.isEnabled() && target.getUsername().equalsIgnoreCase(currentAdminUsername)) {
            throw new BadRequestException("You cannot deactivate your own administrative account");
        }

        // Safeguard: Cannot deactivate the last remaining active ADMIN
        if (target.isEnabled() && target.getRole() == User.Role.ADMIN) {
            long activeAdmins = userRepository.countByRoleAndEnabledTrue(User.Role.ADMIN);
            if (activeAdmins <= 1) {
                throw new BadRequestException("Cannot deactivate the last remaining active administrator account");
            }
        }

        target.setEnabled(!target.isEnabled());
        return UserResponse.from(userRepository.save(target));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsersFiltered(String search, String role) {
        return userRepository.findAll().stream()
                .filter(u -> {
                    if (role != null && !role.isBlank() && !role.equalsIgnoreCase("ALL")) {
                        if (!u.getRole().name().equalsIgnoreCase(role.trim())) return false;
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.trim().toLowerCase();
                        boolean matchU = u.getUsername().toLowerCase().contains(s);
                        boolean matchE = u.getEmail().toLowerCase().contains(s);
                        boolean matchN = u.getFullName() != null && u.getFullName().toLowerCase().contains(s);
                        if (!matchU && !matchE && !matchN) return false;
                    }
                    return true;
                })
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntityByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user;
    }
}
