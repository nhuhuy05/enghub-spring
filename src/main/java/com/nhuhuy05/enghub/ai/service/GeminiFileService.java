package com.nhuhuy05.enghub.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhuhuy05.enghub.ai.dto.GeminiUploadedFile;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GeminiFileService {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    GeminiProperties properties;
    ObjectMapper objectMapper;
    RestClient.Builder restClientBuilder;

    public GeminiUploadedFile uploadAudio(MediaAsset mediaAsset) {
        return uploadMedia(mediaAsset, detectAudioMimeType(mediaAsset), ErrorCode.AUDIO_DOWNLOAD_FAILED);
    }

    public List<GeminiUploadedFile> uploadVisualAssets(List<MediaAsset> visualAssets) {
        if (visualAssets == null || visualAssets.isEmpty()) {
            return List.of();
        }
        List<GeminiUploadedFile> uploadedFiles = new ArrayList<>();
        try {
            for (MediaAsset visualAsset : visualAssets) {
                uploadedFiles.add(uploadMedia(visualAsset, detectImageMimeType(visualAsset), ErrorCode.VISUAL_DOWNLOAD_FAILED));
            }
            return uploadedFiles;
        } catch (RuntimeException exception) {
            deleteFilesIfEnabled(uploadedFiles);
            throw exception;
        }
    }

    public void deleteFilesIfEnabled(List<GeminiUploadedFile> uploadedFiles) {
        if (!properties.isDeleteFileAfterUse() || uploadedFiles == null) {
            return;
        }
        uploadedFiles.forEach(file -> deleteFile(file.name()));
    }

    public void deleteFileIfEnabled(GeminiUploadedFile uploadedFile) {
        if (properties.isDeleteFileAfterUse() && uploadedFile != null) {
            deleteFile(uploadedFile.name());
        }
    }

    private GeminiUploadedFile uploadMedia(MediaAsset mediaAsset, String fallbackMimeType, ErrorCode downloadErrorCode) {
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

    private GeminiUploadedFile uploadFile(byte[] bytes, String displayName, String mimeType) {
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
            return new GeminiUploadedFile(name, uri, isBlank(responseMimeType) ? mimeType : responseMimeType);
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

    private record DownloadedMedia(byte[] bytes, String contentType) {
    }
}
