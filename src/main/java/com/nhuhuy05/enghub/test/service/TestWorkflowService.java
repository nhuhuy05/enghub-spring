package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudioRange;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRangeRepository;
import com.nhuhuy05.enghub.reading.repository.PassageRepository;
import com.nhuhuy05.enghub.test.dto.AudioRangeResponse;
import com.nhuhuy05.enghub.test.dto.AudioRangeUpdateRequest;
import com.nhuhuy05.enghub.test.dto.PublishResponse;
import com.nhuhuy05.enghub.test.dto.TestPreviewResponse;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.Test;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestWorkflowService {
    TestRepository testRepository;
    QuestionRepository questionRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionGroupAudioRangeRepository questionGroupAudioRangeRepository;
    PassageRepository passageRepository;

    @Transactional
    public List<AudioRangeResponse> updateAudioRanges(Long testId, List<AudioRangeUpdateRequest> requests) {
        testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        return requests.stream()
                .map(request -> updateAudioRange(testId, request))
                .toList();
    }

    @Transactional(readOnly = true)
    public TestPreviewResponse preview(Long testId) {
        testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        return buildPreview(testId);
    }

    @Transactional
    public PublishResponse publish(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        TestPreviewResponse preview = buildPreview(testId);
        if (!preview.isPublishable()) {
            return PublishResponse.builder()
                    .success(false)
                    .published(test.getPublished())
                    .errors(preview.getErrors())
                    .build();
        }

        test.setPublished(true);
        testRepository.save(test);

        return PublishResponse.builder()
                .success(true)
                .published(true)
                .errors(List.of())
                .build();
    }

    private AudioRangeResponse updateAudioRange(Long testId, AudioRangeUpdateRequest request) {
        if (request.getEndMs() != null && request.getEndMs() <= request.getStartMs()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        QuestionGroup questionGroup = questionGroupRepository
                .findByTestPartTestIdAndTestPartPartNumberAndOrderIndex(
                        testId,
                        request.getPartNumber(),
                        request.getGroupOrder()
                )
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_GROUP_NOT_EXISTED));

        QuestionGroupAudioRange range = questionGroupAudioRangeRepository
                .findByQuestionGroupIdAndOrderIndex(questionGroup.getId(), 0)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_GROUP_NOT_EXISTED));

        range.setStartMs(request.getStartMs());
        range.setEndMs(request.getEndMs());

        return toAudioRangeResponse(questionGroupAudioRangeRepository.save(range));
    }

    private TestPreviewResponse buildPreview(Long testId) {
        long questionCount = questionRepository.countByTestId(testId);
        long invalidCorrectAnswerCount = questionRepository.countQuestionsWithoutExactlyOneCorrectAnswer(testId);
        long partOneMissingImageCount = questionGroupRepository.countPartOneGroupsWithoutImage(testId);
        long listeningMissingAudioRangeCount = questionGroupAudioRangeRepository.countListeningGroupsWithoutValidAudioRange(testId);
        long readingMissingPassageCount = passageRepository.countReadingGroupsWithoutPassage(testId);

        List<String> errors = new ArrayList<>();
        if (questionCount != 200) {
            errors.add("Test must have exactly 200 questions, current count is " + questionCount);
        }
        if (invalidCorrectAnswerCount > 0) {
            errors.add(invalidCorrectAnswerCount + " questions do not have exactly one correct answer");
        }
        if (partOneMissingImageCount > 0) {
            errors.add("Part 1 still has " + partOneMissingImageCount + " groups without an image");
        }
        if (listeningMissingAudioRangeCount > 0) {
            errors.add("Part 1-4 still has " + listeningMissingAudioRangeCount + " groups without a valid audio range");
        }

        return TestPreviewResponse.builder()
                .testId(testId)
                .questionCount(questionCount)
                .invalidCorrectAnswerCount(invalidCorrectAnswerCount)
                .partOneMissingImageCount(partOneMissingImageCount)
                .listeningMissingAudioRangeCount(listeningMissingAudioRangeCount)
                .readingMissingPassageCount(readingMissingPassageCount)
                .publishable(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private AudioRangeResponse toAudioRangeResponse(QuestionGroupAudioRange range) {
        return AudioRangeResponse.builder()
                .id(range.getId())
                .questionGroupId(range.getQuestionGroup().getId())
                .partNumber(range.getQuestionGroup().getTestPart().getPartNumber())
                .groupOrder(range.getQuestionGroup().getOrderIndex())
                .mediaAssetId(range.getMediaAsset().getId())
                .startMs(range.getStartMs())
                .endMs(range.getEndMs())
                .build();
    }
}
