package com.nhuhuy05.enghub.user.service;

import com.nhuhuy05.enghub.common.enums.SystemRole;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.user.dto.RoleRequest;
import com.nhuhuy05.enghub.user.dto.RoleResponse;
import com.nhuhuy05.enghub.user.mapper.RoleMapper;
import com.nhuhuy05.enghub.user.repository.PermissionRepository;
import com.nhuhuy05.enghub.user.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    private static final Set<String> ALLOWED_ROLE_NAMES = Set.of(SystemRole.ADMIN.name(), SystemRole.STUDENT.name());

    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    public RoleResponse create(RoleRequest request){
        String roleName = normalizeRoleName(request.getName());
        if (!ALLOWED_ROLE_NAMES.contains(roleName)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        var role = roleMapper.toRole(request);
        role.setName(roleName);

        Set<String> permissionNames = request.getPermissions() == null ? Set.of() : request.getPermissions();
        var permissions = permissionRepository.findByNameIn(permissionNames);
        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAll(){
        return roleRepository.findAll()
                .stream()
                .filter(role -> ALLOWED_ROLE_NAMES.contains(role.getName()))
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    public void delete(String role){
        String roleName = normalizeRoleName(role);
        if (ALLOWED_ROLE_NAMES.contains(roleName)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        roleRepository.deleteByName(roleName);
    }

    private String normalizeRoleName(String role) {
        if (role == null || role.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return normalized;
    }
}



