package com.nhuhuy05.enghub.listening.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.listening.dto.ListeningDictationSessionResponse;
import com.nhuhuy05.enghub.listening.service.ListeningDictationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/listening")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListeningDictationController {
    ListeningDictationService listeningDictationService;

    @GetMapping("/tests/{testId}/parts/{partNumber}/dictation")
    ApiResponse<ListeningDictationSessionResponse> getDictationSession(
            @PathVariable Long testId,
            @PathVariable Integer partNumber
    ) {
        return ApiResponse.<ListeningDictationSessionResponse>builder()
                .result(listeningDictationService.getDictationSession(testId, partNumber))
                .build();
    }
}
