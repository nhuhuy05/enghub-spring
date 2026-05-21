package com.nhuhuy05.enghub.test.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.test.dto.TestCollectionRequest;
import com.nhuhuy05.enghub.test.dto.TestCollectionResponse;
import com.nhuhuy05.enghub.test.dto.TestResponse;
import com.nhuhuy05.enghub.test.service.TestManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/test-collections")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestCollectionController {
    TestManagementService testManagementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<TestCollectionResponse> createCollection(@RequestBody @Valid TestCollectionRequest request) {
        return ApiResponse.<TestCollectionResponse>builder()
                .result(testManagementService.createCollection(request))
                .build();
    }

    @GetMapping
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<TestCollectionResponse>> getCollections() {
        return ApiResponse.<List<TestCollectionResponse>>builder()
                .result(testManagementService.getCollections())
                .build();
    }

    @GetMapping("/{collectionId}/tests")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<TestResponse>> getTestsByCollection(@PathVariable Long collectionId) {
        return ApiResponse.<List<TestResponse>>builder()
                .result(testManagementService.getTestsByCollection(collectionId))
                .build();
    }
}
