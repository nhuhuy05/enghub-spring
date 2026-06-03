package com.nhuhuy05.enghub.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhuhuy05.enghub.ai.dto.GeminiTranscriptResult;
import com.nhuhuy05.enghub.ai.dto.GeminiUploadedFile;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.config.GeminiProperties;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GeminiClientService {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    GeminiProperties properties;
    ObjectMapper objectMapper;
    RestClient.Builder restClientBuilder;
    GeminiPromptFactory promptFactory;
    GeminiResponseParser responseParser;
    GeminiFileService fileService;

    public GeminiTranscriptResult generateTranscript(MediaAsset mediaAsset, int partNumber) {
        ensureEnabled();
        GeminiUploadedFile uploadedFile = fileService.uploadAudio(mediaAsset);
        try {
            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("file_data", Map.of(
                    "mime_type", uploadedFile.mimeType(),
                    "file_uri", uploadedFile.uri()
            )));
            parts.add(Map.of("text", promptFactory.transcriptPrompt(partNumber)));

            JsonNode json = responseParser.readJsonFromGeneratedText(generateContent(parts));
            return new GeminiTranscriptResult(
                    textOrNull(json, "transcript_en"),
                    textOrNull(json, "transcript_vi"),
                    responseParser.transcriptAnswers(json)
            );
        } finally {
            fileService.deleteFileIfEnabled(uploadedFile);
        }
    }

    public JsonNode generateQuestionTranslation(JsonNode input) {
        return responseParser.readJsonFromGeneratedText(generateContent(List.of(Map.of("text", promptFactory.questionTranslationPrompt(input)))));
    }

    public JsonNode generateExplanations(JsonNode input) {
        return responseParser.readJsonFromGeneratedText(generateContent(List.of(Map.of("text", promptFactory.explanationPrompt(input)))));
    }

    public JsonNode translateVocabulary(JsonNode input) {
        return responseParser.readJsonFromGeneratedText(generateContent(List.of(Map.of("text", promptFactory.vocabularyTranslationPrompt(input)))));
    }

    public JsonNode generateReadingTranslation(JsonNode input) {
        return responseParser.readJsonFromGeneratedText(generateContent(List.of(Map.of("text", promptFactory.readingTranslationPrompt(input)))));
    }

    public JsonNode generateReadingTranslation(JsonNode input, List<MediaAsset> visualAssets) {
        ensureEnabled();
        List<GeminiUploadedFile> uploadedFiles = fileService.uploadVisualAssets(visualAssets);
        try {
            List<Map<String, Object>> parts = new ArrayList<>();
            for (GeminiUploadedFile uploadedFile : uploadedFiles) {
                parts.add(Map.of("file_data", Map.of(
                        "mime_type", uploadedFile.mimeType(),
                        "file_uri", uploadedFile.uri()
                )));
            }
            parts.add(Map.of("text", promptFactory.readingTranslationPrompt(input)));
            return responseParser.readJsonFromGeneratedText(generateContent(parts));
        } finally {
            fileService.deleteFilesIfEnabled(uploadedFiles);
        }
    }

    public JsonNode generateReadingVocabulary(JsonNode input) {
        return responseParser.readJsonFromGeneratedText(generateContent(List.of(Map.of("text", promptFactory.readingVocabularyPrompt(input)))));
    }

    public void streamPracticeQuestionChat(JsonNode context, String userMessage, Consumer<String> onDelta) {
        ensureEnabled();
        try {
            String url = BASE_URL + "/v1beta/" + modelResourceName() + ":streamGenerateContent?alt=sse";

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", promptFactory.practiceQuestionChatPrompt(context, userMessage)))
            )));
            request.put("generationConfig", Map.of("temperature", 0.2));

            restClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("x-goog-api-key", properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode().isError()) {
                            throw new AppException(ErrorCode.GEMINI_GENERATION_FAILED);
                        }

                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                                clientResponse.getBody(),
                                StandardCharsets.UTF_8
                        ))) {
                            StringBuilder dataBuffer = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isBlank()) {
                                    responseParser.emitStreamData(dataBuffer, onDelta);
                                    continue;
                                }
                                if (line.startsWith("data:")) {
                                    dataBuffer.append(line.substring(5).trim());
                                }
                            }
                            responseParser.emitStreamData(dataBuffer, onDelta);
                        }
                        return null;
                    });
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Gemini streaming generation failed. status={}, body={}",
                    exception.getStatusCode(),
                    shorten(exception.getResponseBodyAsString()));
            throw new AppException(ErrorCode.GEMINI_GENERATION_FAILED);
        } catch (Exception exception) {
            log.warn("Gemini streaming generation failed. message={}", exception.getMessage());
            throw new AppException(ErrorCode.GEMINI_GENERATION_FAILED);
        }
    }

    public JsonNode generateExplanations(JsonNode input, List<MediaAsset> visualAssets) {
        ensureEnabled();
        List<GeminiUploadedFile> uploadedFiles = fileService.uploadVisualAssets(visualAssets);
        try {
            List<Map<String, Object>> parts = new ArrayList<>();
            for (GeminiUploadedFile uploadedFile : uploadedFiles) {
                parts.add(Map.of("file_data", Map.of(
                        "mime_type", uploadedFile.mimeType(),
                        "file_uri", uploadedFile.uri()
                )));
            }
            parts.add(Map.of("text", promptFactory.explanationPrompt(input)));
            return responseParser.readJsonFromGeneratedText(generateContent(parts));
        } finally {
            fileService.deleteFilesIfEnabled(uploadedFiles);
        }
    }

    private String generateContent(List<Map<String, Object>> parts) {
        ensureEnabled();
        try {
            String url = BASE_URL + "/v1beta/" + modelResourceName() + ":generateContent";

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("contents", List.of(Map.of("role", "user", "parts", parts)));
            request.put("generationConfig", Map.of(
                    "temperature", 0.2,
                    "responseMimeType", "application/json"
            ));

            String response = restClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("x-goog-api-key", properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode partsNode = root.path("candidates").path(0).path("content").path("parts");
            if (!partsNode.isArray() || partsNode.isEmpty()) {
                throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
            }
            String text = textOrNull(partsNode.get(0), "text");
            if (isBlank(text)) {
                throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
            }
            return text;
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Gemini generation failed. status={}, body={}",
                    exception.getStatusCode(),
                    shorten(exception.getResponseBodyAsString()));
            throw new AppException(ErrorCode.GEMINI_GENERATION_FAILED);
        } catch (Exception exception) {
            log.warn("Gemini generation failed. message={}", exception.getMessage());
            throw new AppException(ErrorCode.GEMINI_GENERATION_FAILED);
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new AppException(ErrorCode.GEMINI_DISABLED);
        }
        if (isBlank(properties.getApiKey())) {
            throw new AppException(ErrorCode.GEMINI_API_KEY_MISSING);
        }
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

    private String modelResourceName() {
        String model = properties.getModel().trim();
        if (model.startsWith("models/")) {
            return model;
        }
        return "models/" + model;
    }

    private String shorten(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500) + "...";
    }

}
