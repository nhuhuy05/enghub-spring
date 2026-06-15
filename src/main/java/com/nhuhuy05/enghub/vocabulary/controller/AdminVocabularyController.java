package com.nhuhuy05.enghub.vocabulary.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.vocabulary.dto.*;
import com.nhuhuy05.enghub.vocabulary.service.VocabularyEnrichmentService;
import com.nhuhuy05.enghub.vocabulary.service.VocabularyImportService;
import com.nhuhuy05.enghub.vocabulary.service.VocabularyService;
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
@RequestMapping("/admin/vocabulary")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminVocabularyController {
    VocabularyService vocabularyService;
    VocabularyImportService vocabularyImportService;
    VocabularyEnrichmentService vocabularyEnrichmentService;

    @PostMapping("/topics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyTopicResponse> createTopic(@RequestBody @Valid VocabularyTopicRequest request) {
        return ApiResponse.<VocabularyTopicResponse>builder()
                .result(vocabularyService.createTopic(request))
                .build();
    }

    @GetMapping("/topics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<VocabularyTopicResponse>> getTopics() {
        return ApiResponse.<List<VocabularyTopicResponse>>builder()
                .result(vocabularyService.getTopics())
                .build();
    }

    @PutMapping("/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyTopicResponse> updateTopic(
            @PathVariable Long topicId,
            @RequestBody @Valid VocabularyTopicRequest request
    ) {
        return ApiResponse.<VocabularyTopicResponse>builder()
                .result(vocabularyService.updateTopic(topicId, request))
                .build();
    }

    @DeleteMapping("/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<String> deleteTopic(@PathVariable Long topicId) {
        vocabularyService.deleteTopic(topicId);
        return ApiResponse.<String>builder()
                .result("Vocabulary topic has been deleted")
                .build();
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyLookupResponse> lookup(@RequestParam String word) {
        return ApiResponse.<VocabularyLookupResponse>builder()
                .result(vocabularyService.lookup(word))
                .build();
    }

    @PostMapping("/topics/{topicId}/import")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyImportResponse> importToTopic(
            @PathVariable Long topicId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "replace", defaultValue = "false") boolean replace
    ) {
        return ApiResponse.<VocabularyImportResponse>builder()
                .result(vocabularyImportService.importToTopic(topicId, file, replace))
                .build();
    }

    @PostMapping("/topics/{topicId}/enrich")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyEnrichResponse> enrichTopic(
            @PathVariable Long topicId,
            @RequestBody(required = false) VocabularyEnrichRequest request
    ) {
        return ApiResponse.<VocabularyEnrichResponse>builder()
                .result(vocabularyEnrichmentService.enrichTopic(topicId, request))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyResponse> createVocabulary(@RequestBody @Valid VocabularyRequest request) {
        return ApiResponse.<VocabularyResponse>builder()
                .result(vocabularyService.createVocabulary(request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<VocabularyResponse>> searchVocabulary(
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.<List<VocabularyResponse>>builder()
                .result(vocabularyService.searchVocabulary(topicId, keyword))
                .build();
    }

    @GetMapping("/{vocabularyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyResponse> getVocabulary(@PathVariable Long vocabularyId) {
        return ApiResponse.<VocabularyResponse>builder()
                .result(vocabularyService.getVocabulary(vocabularyId, null))
                .build();
    }

    @PutMapping("/{vocabularyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyResponse> updateVocabulary(
            @PathVariable Long vocabularyId,
            @RequestBody @Valid VocabularyRequest request
    ) {
        return ApiResponse.<VocabularyResponse>builder()
                .result(vocabularyService.updateVocabulary(vocabularyId, request))
                .build();
    }

    @DeleteMapping("/{vocabularyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<String> deleteVocabulary(@PathVariable Long vocabularyId) {
        vocabularyService.deleteVocabulary(vocabularyId);
        return ApiResponse.<String>builder()
                .result("Vocabulary has been deleted")
                .build();
    }

    @PostMapping("/{vocabularyId}/enrich")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyEnrichResponse> enrichVocabulary(
            @PathVariable Long vocabularyId,
            @RequestBody(required = false) VocabularyEnrichRequest request
    ) {
        return ApiResponse.<VocabularyEnrichResponse>builder()
                .result(vocabularyEnrichmentService.enrichVocabulary(vocabularyId, request))
                .build();
    }

    @PostMapping("/{vocabularyId}/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyResponse> attachTopic(@PathVariable Long vocabularyId, @PathVariable Long topicId) {
        return ApiResponse.<VocabularyResponse>builder()
                .result(vocabularyService.attachTopic(vocabularyId, topicId))
                .build();
    }

    @DeleteMapping("/{vocabularyId}/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VocabularyResponse> detachTopic(@PathVariable Long vocabularyId, @PathVariable Long topicId) {
        return ApiResponse.<VocabularyResponse>builder()
                .result(vocabularyService.detachTopic(vocabularyId, topicId))
                .build();
    }
}
