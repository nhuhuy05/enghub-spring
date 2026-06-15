package com.nhuhuy05.enghub.reading.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.reading.dto.*;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonStatus;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import com.nhuhuy05.enghub.reading.service.ReadingLessonService;
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
@RequestMapping("/admin/reading-lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminReadingLessonController {
    ReadingLessonService readingLessonService;

    @GetMapping("/part7-candidates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<ReadingPart7CandidateResponse>> getPart7Candidates(
            @RequestParam(name = "test_id", required = false) Long testId
    ) {
        return ApiResponse.<List<ReadingPart7CandidateResponse>>builder()
                .result(readingLessonService.getPart7Candidates(testId))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<ReadingLessonListItemResponse>> getLessons(
            @RequestParam(name = "status", required = false) ReadingLessonStatus status,
            @RequestParam(name = "reading_type", required = false) ReadingLessonType readingType
    ) {
        return ApiResponse.<List<ReadingLessonListItemResponse>>builder()
                .result(readingLessonService.getAdminLessons(status, readingType))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<ReadingLessonResponse> createLesson(@RequestBody @Valid ReadingLessonCreateRequest request) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.createFromQuestionGroup(request))
                .build();
    }

    @GetMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<ReadingLessonResponse> getLesson(@PathVariable Long lessonId) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.getAdminLesson(lessonId))
                .build();
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<ReadingLessonResponse> updateLesson(
            @PathVariable Long lessonId,
            @RequestBody @Valid ReadingLessonUpdateRequest request
    ) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.updateLesson(lessonId, request))
                .build();
    }

    @PatchMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<ReadingLessonResponse> patchLesson(
            @PathVariable Long lessonId,
            @RequestBody @Valid ReadingLessonUpdateRequest request
    ) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.updateLesson(lessonId, request))
                .build();
    }

    @PatchMapping("/{lessonId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<ReadingLessonResponse> updateStatus(
            @PathVariable Long lessonId,
            @RequestBody @Valid ReadingLessonStatusUpdateRequest request
    ) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.updateStatus(lessonId, request.getStatus()))
                .build();
    }

    @PostMapping("/{lessonId}/generate-translation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<ReadingLessonResponse> generateTranslation(
            @PathVariable Long lessonId,
            @RequestBody(required = false) ReadingAiGenerationRequest request
    ) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.generateTranslation(lessonId, request))
                .build();
    }

    @PostMapping("/{lessonId}/generate-vocabulary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<ReadingLessonResponse> generateVocabulary(
            @PathVariable Long lessonId,
            @RequestBody(required = false) ReadingAiGenerationRequest request
    ) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.generateVocabulary(lessonId, request))
                .build();
    }

    @PostMapping("/{lessonId}/generate-ai-support")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<ReadingLessonResponse> generateAiSupport(
            @PathVariable Long lessonId,
            @RequestBody(required = false) ReadingAiGenerationRequest request
    ) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.generateAiSupport(lessonId, request))
                .build();
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<String> deleteLesson(@PathVariable Long lessonId) {
        readingLessonService.deleteLesson(lessonId);
        return ApiResponse.<String>builder()
                .result("Reading lesson has been deleted")
                .build();
    }
}
