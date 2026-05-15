package com.nhuhuy05.enghub.user.mapper;

import com.nhuhuy05.enghub.user.dto.PermissionRequest;
import com.nhuhuy05.enghub.user.dto.PermissionResponse;
import com.nhuhuy05.enghub.user.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    @Mapping(target = "id", ignore = true)
    Permission toPermission(PermissionRequest request);
    PermissionResponse toPermissionResponse(Permission permission);
}



