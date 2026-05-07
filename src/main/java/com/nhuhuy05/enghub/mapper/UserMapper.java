package com.nhuhuy05.enghub.mapper;

import com.nhuhuy05.enghub.dto.request.UserCreationRequest;
import com.nhuhuy05.enghub.dto.request.UserUpdateRequest;
import com.nhuhuy05.enghub.dto.response.UserResponse;
import com.nhuhuy05.enghub.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
