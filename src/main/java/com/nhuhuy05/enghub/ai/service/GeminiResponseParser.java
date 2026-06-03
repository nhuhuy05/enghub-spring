package com.nhuhuy05.enghub.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhuhuy05.enghub.ai.dto.GeminiTranscriptResult;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GeminiResponseParser {
    ObjectMapper objectMapper;

    public JsonNode readJsonFromGeneratedText(String generatedText) {
        try {
            return objectMapper.readTree(stripCodeFence(generatedText));
        } catch (Exception exception) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
        }
    }

    public List<GeminiTranscriptResult.AnswerLine> transcriptAnswers(JsonNode json) {
        JsonNode answerNodes = json == null ? null : json.path("answers");
        if (answerNodes == null || !answerNodes.isArray()) {
            return List.of();
        }

        List<GeminiTranscriptResult.AnswerLine> answers = new ArrayList<>();
        for (JsonNode answerNode : answerNodes) {
            String label = textOrNull(answerNode, "label");
            if (isBlank(label)) {
                continue;
            }
            answers.add(new GeminiTranscriptResult.AnswerLine(
                    label.trim().toUpperCase(),
                    textOrNull(answerNode, "answer_text_en"),
                    textOrNull(answerNode, "answer_text_vi")
            ));
        }
        return answers;
    }

    public void emitStreamData(StringBuilder dataBuffer, Consumer<String> onDelta) {
        if (dataBuffer.isEmpty()) {
            return;
        }
        String payload = dataBuffer.toString();
        dataBuffer.setLength(0);
        if ("[DONE]".equals(payload)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode partsNode = root.path("candidates").path(0).path("content").path("parts");
            if (!partsNode.isArray()) {
                return;
            }
            for (JsonNode partNode : partsNode) {
                String text = textOrNull(partNode, "text");
                if (!isBlank(text)) {
                    onDelta.accept(text);
                }
            }
        } catch (Exception exception) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
        }
    }

    private String stripCodeFence(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.trim();
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
