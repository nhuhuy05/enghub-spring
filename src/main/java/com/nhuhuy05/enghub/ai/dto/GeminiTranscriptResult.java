package com.nhuhuy05.enghub.ai.dto;

import java.util.List;

public record GeminiTranscriptResult(
        String transcriptEn,
        String transcriptVi,
        List<AnswerLine> answers
) {
    public GeminiTranscriptResult {
        answers = answers == null ? List.of() : List.copyOf(answers);
    }

    public record AnswerLine(
            String label,
            String answerTextEn,
            String answerTextVi
    ) {
    }
}
