package com.nhuhuy05.enghub.user.service;

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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    public RoleResponse create(RoleRequest request){
        var role = roleMapper.toRole(request);

        Set<String> permissionNames = request.getPermissions() == null ? Set.of() : request.getPermissions();
        var permissions = permissionRepository.findByNameIn(permissionNames);
        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAll(){
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    public void delete(String role){
        roleRepository.deleteByName(role);
    }
}



