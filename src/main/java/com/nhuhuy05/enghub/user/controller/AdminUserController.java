package com.nhuhuy05.enghub.user.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.common.response.PageResponse;
import com.nhuhuy05.enghub.user.dto.AdminUserCreateRequest;
import com.nhuhuy05.enghub.user.dto.AdminUserResponse;
import com.nhuhuy05.enghub.user.dto.AdminUserStatusUpdateRequest;
import com.nhuhuy05.enghub.user.dto.AdminUserUpdateRequest;
import com.nhuhuy05.enghub.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserController {
    AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<AdminUserResponse>>builder()
                .result(adminUserService.getUsers(keyword, role, active, page, size))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<AdminUserResponse> createUser(@RequestBody @Valid AdminUserCreateRequest request) {
        return ApiResponse.<AdminUserResponse>builder()
                .result(adminUserService.createUser(request))
                .build();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<AdminUserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.<AdminUserResponse>builder()
                .result(adminUserService.getUser(userId))
                .build();
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<AdminUserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserUpdateRequest request
    ) {
        return ApiResponse.<AdminUserResponse>builder()
                .result(adminUserService.updateUser(userId, request))
                .build();
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<AdminUserResponse> updateStatus(
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserStatusUpdateRequest request
    ) {
        return ApiResponse.<AdminUserResponse>builder()
                .result(adminUserService.updateStatus(userId, request))
                .build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<String> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result("User has been deleted")
                .build();
    }
}
