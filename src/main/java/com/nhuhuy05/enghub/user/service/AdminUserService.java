package com.nhuhuy05.enghub.user.service;

import com.nhuhuy05.enghub.common.enums.SystemRole;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.common.response.PageResponse;
import com.nhuhuy05.enghub.user.dto.AdminUserCreateRequest;
import com.nhuhuy05.enghub.user.dto.AdminUserResponse;
import com.nhuhuy05.enghub.user.dto.AdminUserStatusUpdateRequest;
import com.nhuhuy05.enghub.user.dto.AdminUserUpdateRequest;
import com.nhuhuy05.enghub.user.dto.RoleResponse;
import com.nhuhuy05.enghub.user.entity.Role;
import com.nhuhuy05.enghub.user.entity.User;
import com.nhuhuy05.enghub.user.mapper.RoleMapper;
import com.nhuhuy05.enghub.user.repository.RoleRepository;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class AdminUserService {
    private static final int MAX_PAGE_SIZE = 100;

    UserRepository userRepository;
    RoleRepository roleRepository;
    RoleMapper roleMapper;
    PasswordEncoder passwordEncoder;

    public PageResponse<AdminUserResponse> getUsers(String keyword, String role, Boolean active, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Page<User> users = userRepository.findAll(
                userSpec(keyword, role, active),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return PageResponse.<AdminUserResponse>builder()
                .content(users.getContent().stream().map(this::toResponse).toList())
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .first(users.isFirst())
                .last(users.isLast())
                .build();
    }

    public AdminUserResponse getUser(Long userId) {
        return toResponse(getExistingUser(userId));
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(blankToNull(request.getFullName()))
                .phone(blankToNull(request.getPhone()))
                .avatarUrl(blankToNull(request.getAvatarUrl()))
                .active(request.getActive() == null ? true : request.getActive())
                .roles(resolveRolesOrDefault(request.getRoles()))
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = getExistingUser(userId);

        if (request.getEmail() != null) {
            String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (email.isBlank()) {
                throw new AppException(ErrorCode.EMAIL_IS_REQUIRED);
            }
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new AppException(ErrorCode.USER_EXISTED);
            }
            user.setEmail(email);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getFullName() != null) {
            user.setFullName(blankToNull(request.getFullName()));
        }
        if (request.getPhone() != null) {
            user.setPhone(blankToNull(request.getPhone()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        }
        if (request.getActive() != null) {
            validateSelfActiveChange(user, request.getActive());
            user.setActive(request.getActive());
        }
        if (request.getRoles() != null) {
            user.setRoles(resolveRoles(request.getRoles()));
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateStatus(Long userId, AdminUserStatusUpdateRequest request) {
        User user = getExistingUser(userId);
        validateSelfActiveChange(user, request.getActive());
        user.setActive(request.getActive());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getExistingUser(userId);
        if (isCurrentUser(user)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        userRepository.delete(user);
    }

    private Specification<User> userSpec(String keyword, String role, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern)
                ));
            }

            if (role != null && !role.isBlank()) {
                String roleName = normalizeRoleName(role);
                var roleJoin = root.join("roles", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(roleJoin.get("name"), roleName));
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private User getExistingUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Set<Role> resolveRolesOrDefault(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of(resolveOrCreateRole(SystemRole.STUDENT));
        }
        return resolveRoles(roleNames);
    }

    private Set<Role> resolveRoles(List<String> roleNames) {
        Set<String> normalizedRoleNames = roleNames.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(this::normalizeRoleName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedRoleNames.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Set<Role> roles = roleRepository.findByNameIn(normalizedRoleNames);
        if (roles.size() != normalizedRoleNames.size()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return new HashSet<>(roles);
    }

    private Role resolveOrCreateRole(SystemRole roleName) {
        return roleRepository.findByName(roleName.name())
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(roleName.name())
                        .description(roleName.name())
                        .build()));
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .active(user.getActive())
                .roles(user.getRoles() == null
                        ? Set.of()
                        : user.getRoles().stream()
                                .map(roleMapper::toRoleResponse)
                                .collect(Collectors.toCollection(LinkedHashSet::new)))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void validateSelfActiveChange(User user, Boolean active) {
        if (Boolean.FALSE.equals(active) && isCurrentUser(user)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private boolean isCurrentUser(User user) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return currentEmail != null && currentEmail.equalsIgnoreCase(user.getEmail());
    }

    private String normalizeRoleName(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return normalized;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
