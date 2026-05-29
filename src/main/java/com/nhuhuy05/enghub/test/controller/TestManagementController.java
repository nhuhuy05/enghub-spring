package com.nhuhuy05.enghub.test.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.media.dto.MediaAssetResponse;
import com.nhuhuy05.enghub.media.service.MediaAssetService;
import com.nhuhuy05.enghub.test.dto.AudioRangeResponse;
import com.nhuhuy05.enghub.test.dto.AudioRangeUpdateRequest;
import com.nhuhuy05.enghub.test.dto.PublishResponse;
import com.nhuhuy05.enghub.test.dto.QuestionGroupListItemResponse;
import com.nhuhuy05.enghub.test.dto.TestCreationRequest;
import com.nhuhuy05.enghub.test.dto.TestImportResponse;
import com.nhuhuy05.enghub.test.dto.TestPartResponse;
import com.nhuhuy05.enghub.test.dto.TestPreviewResponse;
import com.nhuhuy05.enghub.test.dto.TestPreviewContentResponse;
import com.nhuhuy05.enghub.test.dto.TestResponse;
import com.nhuhuy05.enghub.test.service.QuestionGroupReviewService;
import com.nhuhuy05.enghub.test.service.TestImportService;
import com.nhuhuy05.enghub.test.service.TestManagementService;
import com.nhuhuy05.enghub.test.service.TestWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/tests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestManagementController {
    TestManagementService testManagementService;
    TestImportService testImportService;
    TestWorkflowService testWorkflowService;
    QuestionGroupReviewService questionGroupReviewService;
    MediaAssetService mediaAssetService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<TestResponse> createTest(@RequestBody @Valid TestCreationRequest request) {
        return ApiResponse.<TestResponse>builder()
                .result(testManagementService.createTest(request))
                .build();
    }

    @GetMapping("/{testId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<TestResponse> getTestById(@PathVariable Long testId) {
        return ApiResponse.<TestResponse>builder()
                .result(testManagementService.getTestById(testId))
                .build();
    }

    @GetMapping("/{testId}/media")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<MediaAssetResponse>> getMedia(@PathVariable Long testId) {
        return ApiResponse.<List<MediaAssetResponse>>builder()
                .result(mediaAssetService.getMedia(testId))
                .build();
    }

    @PostMapping("/{testId}/parts/init")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<TestPartResponse>> initDefaultParts(@PathVariable Long testId) {
        return ApiResponse.<List<TestPartResponse>>builder()
                .result(testManagementService.initDefaultParts(testId))
                .build();
    }

    @PostMapping("/{testId}/media")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<MediaAssetResponse> uploadMedia(
            @PathVariable Long testId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("label") String label,
            @RequestParam(name = "mediaType", required = false) String mediaType,
            @RequestParam(name = "type", required = false) String type
    ) {
        String resolvedMediaType = mediaType == null ? type : mediaType;
        return ApiResponse.<MediaAssetResponse>builder()
                .result(mediaAssetService.uploadMedia(testId, file, label, resolvedMediaType))
                .build();
    }

    @PutMapping("/{testId}/media/{mediaAssetId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<MediaAssetResponse> replaceMedia(
            @PathVariable Long testId,
            @PathVariable Long mediaAssetId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.<MediaAssetResponse>builder()
                .result(mediaAssetService.replaceMedia(testId, mediaAssetId, file))
                .build();
    }

    @DeleteMapping("/{testId}/media/{mediaAssetId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<String> deleteMedia(@PathVariable Long testId, @PathVariable Long mediaAssetId) {
        mediaAssetService.deleteMedia(testId, mediaAssetId);
        return ApiResponse.<String>builder()
                .result("Media asset has been deleted")
                .build();
    }

    @PostMapping("/{testId}/import")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<TestImportResponse> importQuestions(
            @PathVariable Long testId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "replace", defaultValue = "false") boolean replace
    ) {
        return ApiResponse.<TestImportResponse>builder()
                .result(testImportService.importQuestions(testId, file, replace))
                .build();
    }

    @PatchMapping("/{testId}/audio-ranges")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<AudioRangeResponse>> updateAudioRanges(
            @PathVariable Long testId,
            @RequestBody @Valid List<AudioRangeUpdateRequest> requests
    ) {
        return ApiResponse.<List<AudioRangeResponse>>builder()
                .result(testWorkflowService.updateAudioRanges(testId, requests))
                .build();
    }

    @GetMapping("/{testId}/preview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<TestPreviewResponse> preview(@PathVariable Long testId) {
        return ApiResponse.<TestPreviewResponse>builder()
                .result(testWorkflowService.preview(testId))
                .build();
    }

    @GetMapping("/{testId}/preview-content")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<TestPreviewContentResponse> previewContent(@PathVariable Long testId) {
        return ApiResponse.<TestPreviewContentResponse>builder()
                .result(questionGroupReviewService.getPreviewContent(testId))
                .build();
    }

    @GetMapping("/{testId}/question-groups")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<QuestionGroupListItemResponse>> getQuestionGroups(@PathVariable Long testId) {
        return ApiResponse.<List<QuestionGroupListItemResponse>>builder()
                .result(questionGroupReviewService.getQuestionGroups(testId))
                .build();
    }

    @PatchMapping("/{testId}/publish")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<PublishResponse> publish(@PathVariable Long testId) {
        return ApiResponse.<PublishResponse>builder()
                .result(testWorkflowService.publish(testId))
                .build();
    }

    @PatchMapping("/{testId}/unpublish")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<PublishResponse> unpublish(@PathVariable Long testId) {
        return ApiResponse.<PublishResponse>builder()
                .result(testWorkflowService.unpublish(testId))
                .build();
    }
}
