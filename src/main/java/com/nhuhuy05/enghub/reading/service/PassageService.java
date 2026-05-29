package com.nhuhuy05.enghub.reading.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.media.repository.MediaAssetRepository;
import com.nhuhuy05.enghub.reading.dto.PassageMediaRequest;
import com.nhuhuy05.enghub.reading.dto.PassageRequest;
import com.nhuhuy05.enghub.reading.dto.PassageResponse;
import com.nhuhuy05.enghub.reading.entity.Passage;
import com.nhuhuy05.enghub.reading.repository.PassageRepository;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PassageService {
    PassageRepository passageRepository;
    QuestionGroupRepository questionGroupRepository;
    MediaAssetRepository mediaAssetRepository;

    @Transactional
    public PassageResponse createPassage(PassageRequest request) {
        QuestionGroup questionGroup = questionGroupRepository
                .findByTestPartTestIdAndTestPartPartNumberAndOrderIndex(
                        request.getTestId(),
                        request.getPartNumber(),
                        request.getGroupOrder()
                )
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_GROUP_NOT_EXISTED));

        int orderIndex = request.getOrderIndex() == null ? 0 : request.getOrderIndex();
        passageRepository.findByQuestionGroupIdAndOrderIndex(questionGroup.getId(), orderIndex)
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.INVALID_KEY);
                });

        Passage passage = Passage.builder()
                .questionGroup(questionGroup)
                .title(request.getTitle())
                .passageType(request.getPassageType())
                .contentFormat(request.getContentFormat())
                .contentEn(request.getContentEn())
                .contentVi(request.getContentVi())
                .vocabHints(request.getVocabHints())
                .mediaAsset(resolveMedia(request.getMediaAssetId(), request.getTestId()))
                .orderIndex(orderIndex)
                .build();

        return toResponse(passageRepository.save(passage));
    }

    @Transactional
    public PassageResponse updatePassageMedia(Long passageId, PassageMediaRequest request) {
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new AppException(ErrorCode.PASSAGE_NOT_EXISTED));

        Long testId = passage.getQuestionGroup().getTestPart().getTest().getId();
        passage.setMediaAsset(resolveMedia(request.getMediaAssetId(), testId));
        return toResponse(passageRepository.save(passage));
    }

    private MediaAsset resolveMedia(Long mediaAssetId, Long expectedTestId) {
        if (mediaAssetId == null) {
            return null;
        }
        MediaAsset mediaAsset = mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED));
        if (!mediaAsset.getTest().getId().equals(expectedTestId)) {
            throw new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED);
        }
        return mediaAsset;
    }

    private PassageResponse toResponse(Passage passage) {
        return PassageResponse.builder()
                .id(passage.getId())
                .questionGroupId(passage.getQuestionGroup().getId())
                .partNumber(passage.getQuestionGroup().getTestPart().getPartNumber())
                .groupOrder(passage.getQuestionGroup().getOrderIndex())
                .title(passage.getTitle())
                .passageType(passage.getPassageType())
                .contentFormat(passage.getContentFormat())
                .contentEn(passage.getContentEn())
                .contentVi(passage.getContentVi())
                .vocabHints(passage.getVocabHints())
                .mediaAssetId(passage.getMediaAsset() == null ? null : passage.getMediaAsset().getId())
                .orderIndex(passage.getOrderIndex())
                .build();
    }
}
