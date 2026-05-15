package com.nhuhuy05.enghub.user.mapper;

import com.nhuhuy05.enghub.user.dto.UserCreationRequest;
import com.nhuhuy05.enghub.user.dto.UserUpdateRequest;
import com.nhuhuy05.enghub.user.dto.UserResponse;
import com.nhuhuy05.enghub.user.entity.User;
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



