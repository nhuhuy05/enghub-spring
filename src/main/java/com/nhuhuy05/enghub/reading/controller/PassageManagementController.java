package com.nhuhuy05.enghub.reading.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.reading.dto.PassageMediaRequest;
import com.nhuhuy05.enghub.reading.dto.PassageRequest;
import com.nhuhuy05.enghub.reading.dto.PassageResponse;
import com.nhuhuy05.enghub.reading.service.PassageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/passages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PassageManagementController {
    PassageService passageService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<PassageResponse> createPassage(@RequestBody @Valid PassageRequest request) {
        return ApiResponse.<PassageResponse>builder()
                .result(passageService.createPassage(request))
                .build();
    }

    @PatchMapping("/{passageId}/media")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<PassageResponse> updatePassageMedia(
            @PathVariable Long passageId,
            @RequestBody @Valid PassageMediaRequest request
    ) {
        return ApiResponse.<PassageResponse>builder()
                .result(passageService.updatePassageMedia(passageId, request))
                .build();
    }
}
