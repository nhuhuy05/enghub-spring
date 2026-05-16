package com.nhuhuy05.enghub.test.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.test.dto.AttemptResponse;
import com.nhuhuy05.enghub.test.dto.SaveAnswerRequest;
import com.nhuhuy05.enghub.test.dto.StartAttemptRequest;
import com.nhuhuy05.enghub.test.dto.UserAnswerResponse;
import com.nhuhuy05.enghub.test.service.TestAttemptService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attempts")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestAttemptController {
    TestAttemptService testAttemptService;

    @PostMapping
    ApiResponse<AttemptResponse> startAttempt(@RequestBody StartAttemptRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<AttemptResponse>builder()
                .result(testAttemptService.startAttempt(userEmail, request.getTestId(), request.getMode()))
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
}
