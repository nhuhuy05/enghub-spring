package com.nhuhuy05.enghub.user.mapper;

import com.nhuhuy05.enghub.user.dto.RoleRequest;
import com.nhuhuy05.enghub.user.dto.RoleResponse;
import com.nhuhuy05.enghub.user.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}



