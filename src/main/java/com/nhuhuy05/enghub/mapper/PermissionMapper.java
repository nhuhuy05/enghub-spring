package com.nhuhuy05.enghub.mapper;

import com.nhuhuy05.enghub.dto.request.PermissionRequest;
import com.nhuhuy05.enghub.dto.response.PermissionResponse;
import com.nhuhuy05.enghub.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    @Mapping(target = "id", ignore = true)
    Permission toPermission(PermissionRequest request);
    PermissionResponse toPermissionResponse(Permission permission);
}
