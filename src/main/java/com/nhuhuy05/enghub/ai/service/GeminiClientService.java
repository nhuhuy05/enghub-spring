package com.nhuhuy05.enghub.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhuhuy05.enghub.ai.dto.GeminiTranscriptResult;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.config.GeminiProperties;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GeminiClientService {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    GeminiProperties properties;
    ObjectMapper objectMapper;
    RestClient.Builder restClientBuilder;

    public GeminiTranscriptResult generateTranscript(MediaAsset mediaAsset, int partNumber) {
        ensureEnabled();
        UploadedFile uploadedFile = uploadMedia(mediaAsset, detectAudioMimeType(mediaAsset), ErrorCode.AUDIO_DOWNLOAD_FAILED);
        try {
            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("file_data", Map.of(
                    "mime_type", uploadedFile.mimeType(),
                    "file_uri", uploadedFile.uri()
            )));
            parts.add(Map.of("text", transcriptPrompt(partNumber)));

            JsonNode json = readJsonFromGeneratedText(generateContent(parts));
            return new GeminiTranscriptResult(
                    textOrNull(json, "transcript_en"),
                    textOrNull(json, "transcript_vi"),
                    transcriptAnswers(json)
            );
        } finally {
            if (properties.isDeleteFileAfterUse()) {
                deleteFile(uploadedFile.name());
            }
        }
    }

    public JsonNode generateQuestionTranslation(JsonNode input) {
        return readJsonFromGeneratedText(generateContent(List.of(Map.of("text", questionTranslationPrompt(input)))));
    }

    public JsonNode generateExplanations(JsonNode input) {
        return readJsonFromGeneratedText(generateContent(List.of(Map.of("text", explanationPrompt(input)))));
    }

    public JsonNode generateExplanations(JsonNode input, List<MediaAsset> visualAssets) {
        ensureEnabled();
        List<UploadedFile> uploadedFiles = uploadVisualAssets(visualAssets);
        try {
            List<Map<String, Object>> parts = new ArrayList<>();
            for (UploadedFile uploadedFile : uploadedFiles) {
                parts.add(Map.of("file_data", Map.of(
                        "mime_type", uploadedFile.mimeType(),
                        "file_uri", uploadedFile.uri()
                )));
            }
            parts.add(Map.of("text", explanationPrompt(input)));
            return readJsonFromGeneratedText(generateContent(parts));
        } finally {
            if (properties.isDeleteFileAfterUse()) {
                uploadedFiles.forEach(file -> deleteFile(file.name()));
            }
        }
    }

    private List<UploadedFile> uploadVisualAssets(List<MediaAsset> visualAssets) {
        if (visualAssets == null || visualAssets.isEmpty()) {
            return List.of();
        }
        List<UploadedFile> uploadedFiles = new ArrayList<>();
        try {
            for (MediaAsset visualAsset : visualAssets) {
                uploadedFiles.add(uploadMedia(visualAsset, detectImageMimeType(visualAsset), ErrorCode.VISUAL_DOWNLOAD_FAILED));
            }
            return uploadedFiles;
        } catch (RuntimeException exception) {
            if (properties.isDeleteFileAfterUse()) {
                uploadedFiles.forEach(file -> deleteFile(file.name()));
            }
            throw exception;
        }
    }

    private UploadedFile uploadMedia(MediaAsset mediaAsset, String fallbackMimeType, ErrorCode downloadErrorCode) {
        DownloadedMedia downloadedMedia = downloadMedia(mediaAsset.getUrl(), downloadErrorCode);
        String mimeType = usableMimeType(downloadedMedia.contentType(), fallbackMimeType);
        return uploadFile(downloadedMedia.bytes(), displayName(mediaAsset), mimeType);
    }

    private DownloadedMedia downloadMedia(String url, ErrorCode errorCode) {
        try {
            ResponseEntity<byte[]> response = restClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] bytes = response.getBody();
            if (bytes == null || bytes.length == 0) {
                throw new AppException(errorCode);
            }
            MediaType contentType = response.getHeaders().getContentType();
            return new DownloadedMedia(bytes, contentType == null ? null : contentType.toString());
        } catch (RestClientResponseException exception) {
            log.warn("Media download failed. status={}, body={}",
                    exception.getStatusCode(),
                    shorten(exception.getResponseBodyAsString()));
            throw new AppException(errorCode);
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Media download failed. message={}", exception.getMessage());
            throw new AppException(errorCode);
        }
    }

    private UploadedFile uploadFile(byte[] bytes, String displayName, String mimeType) {
        try {
            RestClient restClient = restClientBuilder.build();
            String startUrl = BASE_URL + "/upload/v1beta/files";

            ResponseEntity<String> startResponse = restClient.post()
                    .uri(startUrl)
                    .header("x-goog-api-key", properties.getApiKey())
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", String.valueOf(bytes.length))
                    .header("X-Goog-Upload-Header-Content-Type", mimeType)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("file", Map.of("display_name", displayName)))
                    .retrieve()
                    .toEntity(String.class);

            String uploadUrl = startResponse.getHeaders().getFirst("X-Goog-Upload-URL");
            if (isBlank(uploadUrl)) {
                log.warn("Gemini upload start response did not include upload URL. status={}, body={}",
                        startResponse.getStatusCode(),
                        shorten(startResponse.getBody()));
                throw new AppException(ErrorCode.GEMINI_UPLOAD_FAILED);
            }

            String uploadResponse = restClient.post()
                    .uri(uploadUrl)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                    .header("X-Goog-Upload-Offset", "0")
                    .header("X-Goog-Upload-Command", "upload, finalize")
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(bytes)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(uploadResponse);
            JsonNode file = root.has("file") ? root.get("file") : root;
            String name = textOrNull(file, "name");
            String uri = textOrNull(file, "uri");
            String responseMimeType = textOrNull(file, "mimeType");
            if (isBlank(responseMimeType)) {
                responseMimeType = textOrNull(file, "mime_type");
            }
            if (isBlank(name) || isBlank(uri)) {
                throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
            }
            return new UploadedFile(name, uri, isBlank(responseMimeType) ? mimeType : responseMimeType);
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Gemini file upload failed. status={}, body={}",
                    exception.getStatusCode(),
                    shorten(exception.getResponseBodyAsString()));
            throw new AppException(ErrorCode.GEMINI_UPLOAD_FAILED);
        } catch (Exception exception) {
            log.warn("Gemini file upload failed. message={}", exception.getMessage());
            throw new AppException(ErrorCode.GEMINI_UPLOAD_FAILED);
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

    private JsonNode readJsonFromGeneratedText(String generatedText) {
        try {
            return objectMapper.readTree(stripCodeFence(generatedText));
        } catch (Exception exception) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
        }
    }

    private void deleteFile(String fileName) {
        if (isBlank(fileName)) {
            return;
        }
        try {
            restClientBuilder.build()
                    .delete()
                    .uri(BASE_URL + "/v1beta/" + fileName)
                    .header("x-goog-api-key", properties.getApiKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ignored) {
            // Uploaded Gemini files expire automatically; deletion failure should not block saving generated content.
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

    private List<GeminiTranscriptResult.AnswerLine> transcriptAnswers(JsonNode json) {
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

    private String transcriptPrompt(int partNumber) {
        String answerRule = switch (partNumber) {
            case 1 -> """
                    - This is TOEIC Part 1. Extract the four spoken answer choices A, B, C, D into answers.
                    - Each answer_text_en must contain only the spoken statement for that label, without "(A)" prefix.
                    - Each answer_text_vi must be the Vietnamese translation of that statement.
                    """;
            case 2 -> """
                    - This is TOEIC Part 2. Extract the three spoken answer choices A, B, C into answers.
                    - Each answer_text_en must contain only the spoken response for that label, without "(A)" prefix.
                    - Each answer_text_vi must be the Vietnamese translation of that response.
                    """;
            default -> """
                    - This is not TOEIC Part 1 or Part 2. Return an empty answers array.
                    """;
        };

        return """
                You are helping create a TOEIC listening test.

                Generate a transcript from this audio and translate it into Vietnamese.

                Return JSON only:
                {
                  "transcript_en": "...",
                  "transcript_vi": "...",
                  "answers": [
                    {
                      "label": "A",
                      "answer_text_en": "...",
                      "answer_text_vi": "..."
                    }
                  ]
                }

                Rules:
                - Do not invent content that is not in the audio.
                - Keep speaker labels if there are multiple speakers.
                - Use natural Vietnamese.
                - If part of the audio is unclear, write [unclear] for that part.
                - If an answer choice is unclear, keep the label and write [unclear] for that answer text.
                """ + answerRule;
    }

    private String questionTranslationPrompt(JsonNode input) {
        return """
                You are helping prepare a TOEIC test for Vietnamese learners.

                Translate question_text_en and answer_text_en into Vietnamese.
                Do not write explanations in this task.
                Do not change correct answers.
                If an English field is blank, return a blank Vietnamese field.
                Do not invent missing text.

                Return JSON only in this exact shape:
                {
                  "questions": [
                    {
                      "question_id": 1,
                      "question_text_vi": "...",
                      "answers": [
                        {"answer_id": 1, "answer_text_vi": "..."}
                      ]
                    }
                  ]
                }

                Input JSON:
                """ + input.toString();
    }

    private String explanationPrompt(JsonNode input) {
        return """
                You are helping prepare TOEIC answer explanations for Vietnamese learners.

                Write explanation_vi for each question in Vietnamese.
                Use the provided correct answer only. Do not change or guess the correct answer.
                Use transcript_en or passage content if provided.
                If images are attached, use them as visual context. The input JSON lists visual assets in the same order as attached files.
                For Part 3/4 graphic questions, use both the transcript and the attached chart/table/map/image.
                For Part 1, use the attached image and answer options.
                For Part 6/7 with passage images, use attached passage images when passage text is missing.
                If there is not enough context, write a short note that the teacher should review the explanation.

                Return JSON only in this exact shape:
                {
                  "questions": [
                    {
                      "question_id": 1,
                      "explanation_vi": "..."
                    }
                  ]
                }

                Input JSON:
                """ + input.toString();
    }

    private String detectAudioMimeType(MediaAsset mediaAsset) {
        String filename = mediaIdentity(mediaAsset);
        if (filename.endsWith(".mp3")) return "audio/mpeg";
        if (filename.endsWith(".wav")) return "audio/wav";
        if (filename.endsWith(".m4a")) return "audio/mp4";
        if (filename.endsWith(".aac")) return "audio/aac";
        if (filename.endsWith(".ogg")) return "audio/ogg";
        if (filename.endsWith(".flac")) return "audio/flac";
        return "audio/mpeg";
    }

    private String detectImageMimeType(MediaAsset mediaAsset) {
        String filename = mediaIdentity(mediaAsset);
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".webp")) return "image/webp";
        if (filename.endsWith(".heic")) return "image/heic";
        if (filename.endsWith(".heif")) return "image/heif";
        if (filename.endsWith(".png")) return "image/png";
        return "image/png";
    }

    private String mediaIdentity(MediaAsset mediaAsset) {
        return (emptyIfNull(mediaAsset.getOriginalFilename()) + " "
                + emptyIfNull(mediaAsset.getLabel()) + " "
                + emptyIfNull(mediaAsset.getUrl())).toLowerCase();
    }

    private String displayName(MediaAsset mediaAsset) {
        if (!isBlank(mediaAsset.getOriginalFilename())) {
            return mediaAsset.getOriginalFilename();
        }
        if (!isBlank(mediaAsset.getLabel())) {
            return mediaAsset.getLabel();
        }
        return "audio-" + mediaAsset.getId();
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

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String usableMimeType(String contentType, String fallbackMimeType) {
        if (isBlank(contentType)) {
            return fallbackMimeType;
        }
        String normalized = contentType.split(";", 2)[0].trim().toLowerCase();
        if (normalized.equals("application/octet-stream") || normalized.equals("binary/octet-stream")) {
            return fallbackMimeType;
        }
        return normalized;
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

    private record UploadedFile(String name, String uri, String mimeType) {
    }

    private record DownloadedMedia(byte[] bytes, String contentType) {
    }
}
