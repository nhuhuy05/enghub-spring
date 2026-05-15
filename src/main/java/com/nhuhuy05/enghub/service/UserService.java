package com.nhuhuy05.enghub.service;

import com.nhuhuy05.enghub.dto.request.UserCreationRequest;
import com.nhuhuy05.enghub.dto.request.UserUpdateRequest;
import com.nhuhuy05.enghub.entity.Role;
import com.nhuhuy05.enghub.dto.response.UserResponse;
import com.nhuhuy05.enghub.entity.User;
import com.nhuhuy05.enghub.enums.SystemRole;
import com.nhuhuy05.enghub.exception.AppException;
import com.nhuhuy05.enghub.exception.ErrorCode;
import com.nhuhuy05.enghub.mapper.UserMapper;
import com.nhuhuy05.enghub.repository.RoleRepository;
import com.nhuhuy05.enghub.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request){
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(resolveOrCreateRole(SystemRole.STUDENT)));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse getMyInfo(){
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByEmail(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isOwner = user.getEmail().equals(auth.getName());

        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        userMapper.updateUser(user, request); // mapper nên ignore password
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null) {
            Set<String> roleNames = new HashSet<>(request.getRoles());
            var roles = roleRepository.findByNameIn(roleNames);
            user.setRoles(new HashSet<>(roles));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public void deleteUser(Long userId){
        userRepository.deleteById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers(){
        log.info("In method get Users");
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(Long id){
        log.info("In method get user by Id");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    private Role resolveOrCreateRole(SystemRole roleName) {
        return roleRepository.findByName(roleName.name())
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(roleName.name())
                                .description(roleName.name())
                                .build()
                ));
    }

}
