package com.nhuhuy05.enghub.test.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import com.nhuhuy05.enghub.test.dto.AttemptContentResponse;
import com.nhuhuy05.enghub.test.dto.AttemptResponse;
import com.nhuhuy05.enghub.test.dto.AttemptResultResponse;
import com.nhuhuy05.enghub.test.dto.AttemptSummaryResponse;
import com.nhuhuy05.enghub.test.dto.QuestionChatRequest;
import com.nhuhuy05.enghub.test.dto.SaveAnswerRequest;
import com.nhuhuy05.enghub.test.dto.StartAttemptRequest;
import com.nhuhuy05.enghub.test.dto.UserAnswerResponse;
import com.nhuhuy05.enghub.test.service.QuestionChatService;
import com.nhuhuy05.enghub.test.service.TestAttemptService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/attempts")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestAttemptController {
    TestAttemptService testAttemptService;
    QuestionChatService questionChatService;

    @PostMapping
    ApiResponse<AttemptResponse> startAttempt(@RequestBody StartAttemptRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<AttemptResponse>builder()
                .result(testAttemptService.startAttempt(
                        userEmail,
                        request.getTestId(),
                        request.getMode(),
                        request.getPartNumbers()
                ))
                .build();
    }

    @GetMapping("/{attemptId}")
    ApiResponse<AttemptResponse> getAttempt(@PathVariable Long attemptId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<AttemptResponse>builder()
                .result(testAttemptService.getAttempt(userEmail, attemptId))
                .build();
    }

    @GetMapping
    ApiResponse<Page<AttemptSummaryResponse>> getAttempts(
            @RequestParam(required = false) AttemptStatus status,
            @RequestParam(required = false) Long testId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<Page<AttemptSummaryResponse>>builder()
                .result(testAttemptService.getAttempts(userEmail, status, testId, page, size))
                .build();
    }

    @GetMapping("/{attemptId}/content")
    ApiResponse<AttemptContentResponse> getAttemptContent(@PathVariable Long attemptId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<AttemptContentResponse>builder()
                .result(testAttemptService.getAttemptContent(userEmail, attemptId))
                .build();
    }

    @PostMapping("/{attemptId}/answers")
    ApiResponse<UserAnswerResponse> saveAnswer(
            @PathVariable Long attemptId,
            @RequestBody SaveAnswerRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<UserAnswerResponse>builder()
                .result(testAttemptService.saveAnswer(userEmail, attemptId, request.getQuestionId(), request.getSelectedAnswerId()))
                .build();
    }

    @PostMapping("/{attemptId}/submit")
    ApiResponse<AttemptResponse> submitAttempt(@PathVariable Long attemptId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<AttemptResponse>builder()
                .result(testAttemptService.submitAttempt(userEmail, attemptId))
                .build();
    }

    @GetMapping("/{attemptId}/result")
    ApiResponse<AttemptResultResponse> getAttemptResult(@PathVariable Long attemptId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<AttemptResultResponse>builder()
                .result(testAttemptService.getAttemptResult(userEmail, attemptId))
                .build();
    }

    @PostMapping(
            value = "/{attemptId}/questions/{questionId}/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    SseEmitter streamQuestionChat(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @RequestBody QuestionChatRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return questionChatService.stream(userEmail, attemptId, questionId, request);
    }
}
