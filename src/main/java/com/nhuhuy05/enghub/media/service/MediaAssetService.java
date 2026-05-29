package com.nhuhuy05.enghub.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.config.CloudinaryProperties;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRangeRepository;
import com.nhuhuy05.enghub.media.dto.MediaAssetResponse;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.media.repository.MediaAssetRepository;
import com.nhuhuy05.enghub.reading.repository.PassageRepository;
import com.nhuhuy05.enghub.test.entity.Test;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MediaAssetService {
    Cloudinary cloudinary;
    CloudinaryProperties cloudinaryProperties;
    MediaAssetRepository mediaAssetRepository;
    TestRepository testRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionGroupAudioRangeRepository questionGroupAudioRangeRepository;
    PassageRepository passageRepository;

    @Transactional
    public MediaAssetResponse uploadMedia(Long testId, MultipartFile file, String label, String mediaType) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        String normalizedType = normalizeMediaType(mediaType);
        validateFile(file, normalizedType);

        String normalizedLabel = label == null ? "" : label.trim();
        if (normalizedLabel.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (mediaAssetRepository.existsByTestIdAndLabelAndMediaType(testId, normalizedLabel, normalizedType)) {
            throw new AppException(ErrorCode.MEDIA_ASSET_EXISTED);
        }

        String publicId = buildPublicId(testId, normalizedType, normalizedLabel);
        Map uploadResult = uploadToCloudinary(file, publicId);

        MediaAsset mediaAsset = MediaAsset.builder()
                .test(test)
                .label(normalizedLabel)
                .mediaType(normalizedType)
                .cloudinaryPublicId(String.valueOf(uploadResult.get("public_id")))
                .url(String.valueOf(uploadResult.get("secure_url")))
                .durationMs(resolveDurationMs(uploadResult))
                .originalFilename(file.getOriginalFilename())
                .build();

        return toResponse(mediaAssetRepository.save(mediaAsset));
    }

    @Transactional
    public MediaAssetResponse replaceMedia(Long testId, Long mediaAssetId, MultipartFile file) {
        MediaAsset mediaAsset = mediaAssetRepository.findByIdAndTestId(mediaAssetId, testId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED));

        validateFile(file, mediaAsset.getMediaType());

        Map uploadResult = uploadToCloudinary(file, mediaAsset.getCloudinaryPublicId(), true);
        mediaAsset.setUrl(String.valueOf(uploadResult.get("secure_url")));
        mediaAsset.setDurationMs(resolveDurationMs(uploadResult));
        mediaAsset.setOriginalFilename(file.getOriginalFilename());

        return toResponse(mediaAssetRepository.save(mediaAsset));
    }

    @Transactional
    public void deleteMedia(Long testId, Long mediaAssetId) {
        MediaAsset mediaAsset = mediaAssetRepository.findByIdAndTestId(mediaAssetId, testId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED));

        ensureMediaAssetNotInUse(mediaAssetId);
        mediaAssetRepository.delete(mediaAsset);
        mediaAssetRepository.flush();
        destroyCloudinaryAsset(mediaAsset);
    }

    private void ensureMediaAssetNotInUse(Long mediaAssetId) {
        if (questionGroupRepository.existsByMediaAssetId(mediaAssetId)
                || passageRepository.existsByMediaAssetId(mediaAssetId)
                || questionGroupAudioRangeRepository.existsByMediaAssetId(mediaAssetId)) {
            throw new AppException(ErrorCode.MEDIA_ASSET_IN_USE);
        }
    }

    private Map uploadToCloudinary(MultipartFile file, String publicId) {
        return uploadToCloudinary(file, publicId, false);
    }

    private Map uploadToCloudinary(MultipartFile file, String publicId, boolean overwrite) {
        try {
            return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto",
                    "public_id", publicId,
                    "overwrite", overwrite,
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            log.error("Cloudinary upload failed for publicId={}", publicId, exception);
            throw new AppException(ErrorCode.CLOUDINARY_UPLOAD_FAILED);
        }
    }

    private void destroyCloudinaryAsset(MediaAsset mediaAsset) {
        try {
            cloudinary.uploader().destroy(mediaAsset.getCloudinaryPublicId(), ObjectUtils.asMap(
                    "resource_type", cloudinaryResourceType(mediaAsset.getMediaType()),
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            log.error("Cloudinary destroy failed for publicId={}", mediaAsset.getCloudinaryPublicId(), exception);
            throw new AppException(ErrorCode.CLOUDINARY_UPLOAD_FAILED);
        }
    }

    private String cloudinaryResourceType(String mediaType) {
        return "audio".equals(mediaType) ? "video" : "image";
    }

    private String buildPublicId(Long testId, String mediaType, String label) {
        return String.format("%s/tests/%d/%s/%s",
                trimSlashes(cloudinaryProperties.getFolderRoot()),
                testId,
                mediaType,
                sanitizeLabel(label));
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String normalized = mediaType.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("image") && !normalized.equals("audio")) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        return normalized;
    }

    private void validateFile(MultipartFile file, String mediaType) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        if (mediaType.equals("image") && !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        if (mediaType.equals("audio") && !contentType.startsWith("audio/")) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }
    }

    private Integer resolveDurationMs(Map uploadResult) {
        Object duration = uploadResult.get("duration");
        if (duration instanceof Number number) {
            return (int) Math.round(number.doubleValue() * 1000);
        }
        return null;
    }

    private String sanitizeLabel(String label) {
        return label.trim()
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
    }

    private String trimSlashes(String value) {
        if (value == null || value.isBlank()) {
            return "enghub";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private MediaAssetResponse toResponse(MediaAsset mediaAsset) {
        return MediaAssetResponse.builder()
                .id(mediaAsset.getId())
                .testId(mediaAsset.getTest().getId())
                .label(mediaAsset.getLabel())
                .mediaType(mediaAsset.getMediaType())
                .cloudinaryPublicId(mediaAsset.getCloudinaryPublicId())
                .url(mediaAsset.getUrl())
                .durationMs(mediaAsset.getDurationMs())
                .originalFilename(mediaAsset.getOriginalFilename())
                .createdAt(mediaAsset.getCreatedAt())
                .build();
    }
}
