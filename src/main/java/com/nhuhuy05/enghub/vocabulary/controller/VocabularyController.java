package com.nhuhuy05.enghub.vocabulary.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.vocabulary.dto.VocabularyResponse;
import com.nhuhuy05.enghub.vocabulary.dto.VocabularyReviewRequest;
import com.nhuhuy05.enghub.vocabulary.dto.VocabularyTopicResponse;
import com.nhuhuy05.enghub.vocabulary.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vocabulary")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VocabularyController {
    VocabularyService vocabularyService;

    @GetMapping("/topics")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<VocabularyTopicResponse>> getTopics() {
        return ApiResponse.<List<VocabularyTopicResponse>>builder()
                .result(vocabularyService.getTopics())
                .build();
    }

    @GetMapping("/topics/{topicId}/words")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<VocabularyResponse>> getTopicWords(@PathVariable Long topicId) {
        return ApiResponse.<List<VocabularyResponse>>builder()
                .result(vocabularyService.getTopicWords(topicId, currentUserEmail()))
                .build();
    }

    @GetMapping("/progress")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<VocabularyResponse>> getProgress() {
        return ApiResponse.<List<VocabularyResponse>>builder()
                .result(vocabularyService.getProgress(currentUserEmail()))
                .build();
    }

    @GetMapping("/due")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<VocabularyResponse>> getDue(@RequestParam(required = false) Long topicId) {
        return ApiResponse.<List<VocabularyResponse>>builder()
                .result(vocabularyService.getDue(currentUserEmail(), topicId))
                .build();
    }

    @GetMapping("/{vocabularyId}")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyResponse> getVocabulary(@PathVariable Long vocabularyId) {
        return ApiResponse.<VocabularyResponse>builder()
                .result(vocabularyService.getVocabulary(vocabularyId, currentUserEmail()))
                .build();
    }

    @PostMapping("/{vocabularyId}/learn")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyResponse> learn(@PathVariable Long vocabularyId) {
        return ApiResponse.<VocabularyResponse>builder()
                .result(vocabularyService.learn(currentUserEmail(), vocabularyId))
                .build();
    }

    @PostMapping("/{vocabularyId}/review")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyResponse> review(
            @PathVariable Long vocabularyId,
            @RequestBody @Valid VocabularyReviewRequest request
    ) {
        return ApiResponse.<VocabularyResponse>builder()
                .result(vocabularyService.review(currentUserEmail(), vocabularyId, request))
                .build();
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
