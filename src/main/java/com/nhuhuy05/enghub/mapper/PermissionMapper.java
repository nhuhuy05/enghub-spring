package com.nhuhuy05.enghub.mapper;

import com.nhuhuy05.enghub.dto.request.PermissionRequest;
import com.nhuhuy05.enghub.dto.response.PermissionResponse;
import com.nhuhuy05.enghub.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);
    PermissionResponse toPermissionResponse(Permission permission);
}
