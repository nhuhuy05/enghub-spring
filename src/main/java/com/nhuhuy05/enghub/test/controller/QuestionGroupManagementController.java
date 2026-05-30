package com.nhuhuy05.enghub.test.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.ai.dto.AiGenerationRequest;
import com.nhuhuy05.enghub.ai.service.QuestionGroupAiService;
import com.nhuhuy05.enghub.test.dto.*;
import com.nhuhuy05.enghub.test.service.QuestionGroupReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionGroupManagementController {
    QuestionGroupReviewService questionGroupReviewService;
    QuestionGroupAiService questionGroupAiService;

    @GetMapping("/question-groups/{groupId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> getQuestionGroup(@PathVariable Long groupId) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupReviewService.getQuestionGroup(groupId))
                .build();
    }

    @PatchMapping("/question-groups/{groupId}/review-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> updateReviewStatus(
            @PathVariable Long groupId,
            @RequestBody @Valid ReviewStatusUpdateRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupReviewService.updateReviewStatus(groupId, userEmail, request))
                .build();
    }

    @PatchMapping("/question-groups/{groupId}/images")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> updateImages(
            @PathVariable Long groupId,
            @RequestBody @Valid QuestionGroupImagesUpdateRequest request
    ) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupReviewService.updateImages(groupId, request))
                .build();
    }

    @PatchMapping("/question-groups/{groupId}/audio")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> updateAudio(
            @PathVariable Long groupId,
            @RequestBody @Valid QuestionGroupAudioUpdateRequest request
    ) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupReviewService.updateAudio(groupId, request))
                .build();
    }

    @PatchMapping("/question-groups/{groupId}/transcript")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> updateTranscript(
            @PathVariable Long groupId,
            @RequestBody QuestionGroupTranscriptUpdateRequest request
    ) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupReviewService.updateTranscript(groupId, request))
                .build();
    }

    @PostMapping("/question-groups/{groupId}/generate-transcript")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> generateTranscript(@PathVariable Long groupId) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupAiService.generateTranscript(groupId))
                .build();
    }

    @PostMapping("/question-groups/{groupId}/generate-question-translation")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> generateQuestionTranslation(@PathVariable Long groupId) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupAiService.generateQuestionTranslation(groupId))
                .build();
    }

    @PostMapping("/question-groups/{groupId}/generate-explanations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> generateExplanations(@PathVariable Long groupId) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupAiService.generateExplanations(groupId))
                .build();
    }

    @PostMapping("/question-groups/{groupId}/generate-ai-support")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> generateAiSupport(
            @PathVariable Long groupId,
            @RequestBody(required = false) AiGenerationRequest request
    ) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupAiService.generateAiSupport(groupId, request))
                .build();
    }

    @PatchMapping("/question-groups/{groupId}/passages")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> updatePassages(
            @PathVariable Long groupId,
            @RequestBody @Valid QuestionGroupPassagesUpdateRequest request
    ) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupReviewService.updatePassages(groupId, request))
                .build();
    }

    @PatchMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody QuestionUpdateRequest request
    ) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupReviewService.updateQuestion(questionId, request))
                .build();
    }

    @PatchMapping("/answers/{answerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<QuestionGroupDetailResponse> updateAnswer(
            @PathVariable Long answerId,
            @RequestBody AnswerUpdateRequest request
    ) {
        return ApiResponse.<QuestionGroupDetailResponse>builder()
                .result(questionGroupReviewService.updateAnswer(answerId, request))
                .build();
    }
}
