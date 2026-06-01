package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupTranscriptLine;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupTranscriptLineRepository;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.media.repository.MediaAssetRepository;
import com.nhuhuy05.enghub.reading.entity.QuestionGroupPassage;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.dto.*;
import com.nhuhuy05.enghub.test.entity.Answer;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.QuestionGroupImage;
import com.nhuhuy05.enghub.test.entity.Test;
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.repository.*;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionGroupReviewService {
    private static final Pattern SPEAKER_LABEL_PATTERN = Pattern.compile(
            "[ \\t]+(?=(?:[A-Z][A-Za-z0-9]*(?:[- ][A-Z]?[A-Za-z0-9]+){0,3}|M-[A-Za-z]+|W-[A-Za-z]+):\\s)"
    );
    private static final Pattern TOEIC_DIRECTION_LINE_PATTERN = Pattern.compile(
            "(?im)^\\s*(?:Number\\s+\\d+\\.?|Questions?\\s+\\d+(?:\\s*(?:-|through|to)\\s*\\d+)?\\s+refer\\s+to\\s+the\\s+following\\s+.*\\.?|Look\\s+at\\s+the\\s+picture\\s+.*test\\s+book\\.?)\\s*$"
    );
    private static final Pattern TOEIC_DIRECTION_PREFIX_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:Number\\s+\\d+\\.\\s*|Questions?\\s+\\d+(?:\\s*(?:-|through|to)\\s*\\d+)?\\s+refer\\s+to\\s+the\\s+following\\s+[^.]*\\.\\s*|Look\\s+at\\s+the\\s+picture\\s+[^.]*\\.\\s*)+"
    );

    TestRepository testRepository;
    TestPartRepository testPartRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionGroupImageRepository questionGroupImageRepository;
    QuestionGroupAudioRepository questionGroupAudioRepository;
    QuestionGroupTranscriptLineRepository questionGroupTranscriptLineRepository;
    QuestionGroupPassageRepository questionGroupPassageRepository;
    QuestionRepository questionRepository;
    AnswerRepository answerRepository;
    MediaAssetRepository mediaAssetRepository;
    UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<QuestionGroupListItemResponse> getQuestionGroups(Long testId) {
        testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        return questionGroupRepository.findAllByTestPartTestIdOrderByTestPartPartNumberAscOrderIndexAsc(testId).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionGroupListItemResponse> getQuestionGroups(Long testId, Integer partNumber) {
        testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));
        if (partNumber == null || partNumber < 1 || partNumber > 7) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        return questionGroupRepository.findAllByTestIdAndPartNumbersOrderByPartAndOrder(testId, List.of(partNumber)).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionGroupDetailResponse getQuestionGroup(Long groupId) {
        QuestionGroup questionGroup = getGroup(groupId);
        return toDetail(questionGroup);
    }

    @Transactional(readOnly = true)
    public TestPreviewContentResponse getPreviewContent(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        Map<Integer, List<QuestionGroup>> groupsByPart = questionGroupRepository
                .findAllByTestPartTestIdOrderByTestPartPartNumberAscOrderIndexAsc(testId).stream()
                .collect(Collectors.groupingBy(group -> group.getTestPart().getPartNumber()));

        List<TestPreviewContentResponse.PartResponse> parts = testPartRepository.findAllByTestIdOrderByPartNumberAsc(testId).stream()
                .map(part -> TestPreviewContentResponse.PartResponse.builder()
                        .partNumber(part.getPartNumber())
                        .title(part.getTitle())
                        .groups(groupsByPart.getOrDefault(part.getPartNumber(), List.of()).stream()
                                .map(this::toDetail)
                                .toList())
                        .build())
                .toList();

        return TestPreviewContentResponse.builder()
                .testId(test.getId())
                .title(test.getTitle())
                .description(test.getDescription())
                .durationMinutes(test.getDurationMinutes())
                .parts(parts)
                .build();
    }

    @Transactional
    public QuestionGroupDetailResponse updateReviewStatus(Long groupId, String userEmail, ReviewStatusUpdateRequest request) {
        QuestionGroup questionGroup = getGroup(groupId);
        String reviewStatus = normalizeReviewStatus(request.getReviewStatus());
        questionGroup.setReviewStatus(reviewStatus);
        if ("reviewed".equals(reviewStatus)) {
            questionGroup.setReviewedAt(LocalDateTime.now());
            userRepository.findByEmail(userEmail).ifPresent(user -> questionGroup.setReviewedBy(user.getId()));
        } else {
            questionGroup.setReviewedAt(null);
            questionGroup.setReviewedBy(null);
        }
        return toDetail(questionGroupRepository.save(questionGroup));
    }

    @Transactional
    public QuestionGroupDetailResponse updateImages(Long groupId, QuestionGroupImagesUpdateRequest request) {
        QuestionGroup questionGroup = getGroup(groupId);
        Long testId = questionGroup.getTestPart().getTest().getId();

        questionGroupImageRepository.deleteAllByQuestionGroupId(groupId);
        questionGroupImageRepository.flush();
        int fallbackOrder = 0;
        for (QuestionGroupImagesUpdateRequest.Item item : request.getImages()) {
            MediaAsset media = resolveMedia(testId, item.getMediaAssetId(), "image");
            questionGroupImageRepository.save(QuestionGroupImage.builder()
                    .questionGroup(questionGroup)
                    .mediaAsset(media)
                    .orderIndex(item.getOrderIndex() == null ? fallbackOrder : item.getOrderIndex())
                    .build());
            fallbackOrder++;
        }
        markNeedsReview(questionGroup);
        return toDetail(questionGroup);
    }

    @Transactional
    public QuestionGroupDetailResponse updateAudio(Long groupId, QuestionGroupAudioUpdateRequest request) {
        QuestionGroup questionGroup = getGroup(groupId);
        Long testId = questionGroup.getTestPart().getTest().getId();

        QuestionGroupAudio audio = questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(groupId, 0)
                .orElseGet(() -> QuestionGroupAudio.builder()
                        .questionGroup(questionGroup)
                        .startMs(0)
                        .orderIndex(0)
                        .build());

        if (request.getMediaAssetId() != null) {
            audio.setMediaAsset(resolveMedia(testId, request.getMediaAssetId(), "audio"));
        }
        if (audio.getMediaAsset() == null) {
            throw new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED);
        }

        Integer startMs = request.getStartMs() == null ? audio.getStartMs() : request.getStartMs();
        Integer endMs = request.getEndMs() == null ? audio.getEndMs() : request.getEndMs();
        validateTimeRange(startMs, endMs);

        audio.setStartMs(startMs == null ? 0 : startMs);
        audio.setEndMs(endMs);
        audio.setTranscriptEn(request.getTranscriptEn());
        audio.setTranscriptVi(request.getTranscriptVi());
        questionGroupAudioRepository.save(audio);

        markNeedsReview(questionGroup);
        return toDetail(questionGroup);
    }

    @Transactional
    public QuestionGroupDetailResponse updateTranscript(Long groupId, QuestionGroupTranscriptUpdateRequest request) {
        QuestionGroup questionGroup = getGroup(groupId);
        QuestionGroupAudio audio = questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(groupId, 0)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_GROUP_NOT_EXISTED));
        audio.setTranscriptEn(request.getTranscriptEn());
        audio.setTranscriptVi(request.getTranscriptVi());
        questionGroupAudioRepository.save(audio);

        markNeedsReview(questionGroup);
        return toDetail(questionGroup);
    }

    @Transactional
    public QuestionGroupDetailResponse updateTranscriptLines(Long groupId, QuestionGroupTranscriptLinesUpdateRequest request) {
        QuestionGroup questionGroup = getGroup(groupId);
        validateListeningPart(questionGroup);
        QuestionGroupAudio audio = questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(groupId, 0)
                .orElseThrow(() -> new AppException(ErrorCode.AI_AUDIO_NOT_EXISTED));

        validateTranscriptLineRequest(request);
        questionGroupTranscriptLineRepository.deleteAllByQuestionGroupAudioId(audio.getId());
        questionGroupTranscriptLineRepository.flush();

        for (QuestionGroupTranscriptLinesUpdateRequest.Line line : request.getLines()) {
            questionGroupTranscriptLineRepository.save(QuestionGroupTranscriptLine.builder()
                    .questionGroupAudio(audio)
                    .speaker(blankToNull(line.getSpeaker()))
                    .textEn(line.getTextEn().trim())
                    .textVi(blankToNull(line.getTextVi()))
                    .startMs(line.getStartMs())
                    .endMs(line.getEndMs())
                    .orderIndex(line.getOrderIndex())
                    .build());
        }

        markNeedsReview(questionGroup);
        return toDetail(questionGroup);
    }

    @Transactional
    public QuestionGroupDetailResponse updatePassages(Long groupId, QuestionGroupPassagesUpdateRequest request) {
        QuestionGroup questionGroup = getGroup(groupId);
        Long testId = questionGroup.getTestPart().getTest().getId();

        questionGroupPassageRepository.deleteAllByQuestionGroupId(groupId);
        questionGroupPassageRepository.flush();
        int fallbackOrder = 0;
        for (QuestionGroupPassagesUpdateRequest.Item item : request.getPassages()) {
            MediaAsset media = item.getMediaAssetId() == null ? null : resolveMedia(testId, item.getMediaAssetId(), "image");
            questionGroupPassageRepository.save(QuestionGroupPassage.builder()
                    .questionGroup(questionGroup)
                    .mediaAsset(media)
                    .title(item.getTitle())
                    .passageType(item.getPassageType())
                    .contentFormat(item.getContentFormat())
                    .contentEn(item.getContentEn())
                    .contentVi(item.getContentVi())
                    .vocabHints(item.getVocabHints())
                    .orderIndex(item.getOrderIndex() == null ? fallbackOrder : item.getOrderIndex())
                    .build());
            fallbackOrder++;
        }
        markNeedsReview(questionGroup);
        return toDetail(questionGroup);
    }

    @Transactional
    public QuestionGroupDetailResponse updateQuestion(Long questionId, QuestionUpdateRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_EXISTED));
        question.setQuestionTextEn(request.getQuestionTextEn());
        question.setQuestionTextVi(request.getQuestionTextVi());
        question.setExplanationVi(request.getExplanationVi());
        questionRepository.save(question);

        QuestionGroup questionGroup = question.getQuestionGroup();
        markNeedsReview(questionGroup);
        return toDetail(questionGroup);
    }

    @Transactional
    public QuestionGroupDetailResponse updateAnswer(Long answerId, AnswerUpdateRequest request) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_EXISTED));
        answer.setAnswerTextEn(request.getAnswerTextEn());
        answer.setAnswerTextVi(request.getAnswerTextVi());

        if (request.getCorrect() != null) {
            if (request.getCorrect()) {
                List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(answer.getQuestion().getId());
                answers.forEach(existing -> existing.setCorrect(Objects.equals(existing.getId(), answer.getId())));
                answerRepository.saveAll(answers);
            } else {
                answer.setCorrect(false);
                answerRepository.save(answer);
            }
        } else {
            answerRepository.save(answer);
        }

        QuestionGroup questionGroup = answer.getQuestion().getQuestionGroup();
        markNeedsReview(questionGroup);
        return toDetail(questionGroup);
    }

    private QuestionGroup getGroup(Long groupId) {
        return questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_GROUP_NOT_EXISTED));
    }

    private QuestionGroupListItemResponse toListItem(QuestionGroup group) {
        List<Question> questions = questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(group.getId());
        QuestionGroupAudio audio = questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(group.getId(), 0)
                .orElse(null);
        long transcriptLineCount = audio == null ? 0 : questionGroupTranscriptLineRepository.countByQuestionGroupAudioId(audio.getId());
        return QuestionGroupListItemResponse.builder()
                .id(group.getId())
                .partNumber(group.getTestPart().getPartNumber())
                .groupOrder(group.getOrderIndex())
                .questionNumbers(questions.stream().map(Question::getQuestionNumber).toList())
                .reviewStatus(group.getReviewStatus())
                .missingFlags(missingFlags(group, questions))
                .hasAudio(audio != null)
                .audioUrl(audio == null ? null : audio.getMediaAsset().getUrl())
                .transcriptLineCount(transcriptLineCount)
                .hasTranscriptLines(transcriptLineCount > 0)
                .build();
    }

    private QuestionGroupDetailResponse toDetail(QuestionGroup group) {
        return QuestionGroupDetailResponse.builder()
                .id(group.getId())
                .partNumber(group.getTestPart().getPartNumber())
                .groupOrder(group.getOrderIndex())
                .reviewStatus(group.getReviewStatus())
                .images(groupImages(group.getId()))
                .audio(groupAudio(group.getId()))
                .passages(groupPassages(group.getId()))
                .questions(groupQuestions(group.getId()))
                .build();
    }

    private List<QuestionGroupDetailResponse.GroupImageResponse> groupImages(Long groupId) {
        return questionGroupImageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId).stream()
                .map(image -> QuestionGroupDetailResponse.GroupImageResponse.builder()
                        .id(image.getId())
                        .mediaAssetId(image.getMediaAsset().getId())
                        .label(image.getMediaAsset().getLabel())
                        .url(image.getMediaAsset().getUrl())
                        .orderIndex(image.getOrderIndex())
                        .build())
                .toList();
    }

    private QuestionGroupDetailResponse.GroupAudioResponse groupAudio(Long groupId) {
        return questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(groupId, 0)
                .map(audio -> QuestionGroupDetailResponse.GroupAudioResponse.builder()
                        .id(audio.getId())
                        .mediaAssetId(audio.getMediaAsset().getId())
                        .label(audio.getMediaAsset().getLabel())
                        .url(audio.getMediaAsset().getUrl())
                        .startMs(audio.getStartMs())
                        .endMs(audio.getEndMs())
                        .transcriptEn(normalizeSpeakerLines(audio.getTranscriptEn()))
                        .transcriptVi(normalizeSpeakerLines(audio.getTranscriptVi()))
                        .transcriptLines(groupTranscriptLines(audio.getId()))
                        .build())
                .orElse(null);
    }

    private List<QuestionGroupTranscriptLineResponse> groupTranscriptLines(Long audioId) {
        return questionGroupTranscriptLineRepository.findAllByQuestionGroupAudioIdOrderByOrderIndexAsc(audioId).stream()
                .map(this::toTranscriptLineResponse)
                .toList();
    }

    private QuestionGroupTranscriptLineResponse toTranscriptLineResponse(QuestionGroupTranscriptLine line) {
        return QuestionGroupTranscriptLineResponse.builder()
                .id(line.getId())
                .speaker(line.getSpeaker())
                .textEn(line.getTextEn())
                .textVi(line.getTextVi())
                .startMs(line.getStartMs())
                .endMs(line.getEndMs())
                .orderIndex(line.getOrderIndex())
                .build();
    }

    private List<QuestionGroupDetailResponse.GroupPassageResponse> groupPassages(Long groupId) {
        return questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId).stream()
                .map(passage -> QuestionGroupDetailResponse.GroupPassageResponse.builder()
                        .id(passage.getId())
                        .mediaAssetId(passage.getMediaAsset() == null ? null : passage.getMediaAsset().getId())
                        .label(passage.getMediaAsset() == null ? null : passage.getMediaAsset().getLabel())
                        .url(passage.getMediaAsset() == null ? null : passage.getMediaAsset().getUrl())
                        .title(passage.getTitle())
                        .passageType(passage.getPassageType())
                        .contentFormat(passage.getContentFormat())
                        .contentEn(passage.getContentEn())
                        .contentVi(passage.getContentVi())
                        .vocabHints(passage.getVocabHints())
                        .orderIndex(passage.getOrderIndex())
                        .build())
                .toList();
    }

    private List<QuestionGroupDetailResponse.GroupQuestionResponse> groupQuestions(Long groupId) {
        return questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(groupId).stream()
                .map(question -> QuestionGroupDetailResponse.GroupQuestionResponse.builder()
                        .id(question.getId())
                        .questionNumber(question.getQuestionNumber())
                        .questionTextEn(question.getQuestionTextEn())
                        .questionTextVi(question.getQuestionTextVi())
                        .explanationVi(question.getExplanationVi())
                        .answers(groupAnswers(question.getId()))
                        .build())
                .toList();
    }

    private List<QuestionGroupDetailResponse.GroupAnswerResponse> groupAnswers(Long questionId) {
        List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(questionId);
        return answers.stream()
                .map(answer -> QuestionGroupDetailResponse.GroupAnswerResponse.builder()
                        .id(answer.getId())
                        .label(answerLabel(answers, answer))
                        .answerTextEn(answer.getAnswerTextEn())
                        .answerTextVi(answer.getAnswerTextVi())
                        .correct(answer.isCorrect())
                        .build())
                .toList();
    }

    private String answerLabel(List<Answer> answers, Answer answer) {
        int index = answers.indexOf(answer);
        return switch (index) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";
            default -> String.valueOf(index + 1);
        };
    }

    private List<String> missingFlags(QuestionGroup group, List<Question> questions) {
        List<String> flags = new ArrayList<>();
        int part = group.getTestPart().getPartNumber();
        if (questions.isEmpty()) {
            flags.add("missing_questions");
        }
        if (part == 1 && questionGroupImageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(group.getId()).isEmpty()) {
            flags.add("missing_image");
        }
        if (part >= 1 && part <= 4 && questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(group.getId(), 0).isEmpty()) {
            flags.add("missing_audio");
        }
        if ((part == 6 || part == 7) && questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(group.getId()).isEmpty()) {
            flags.add("missing_passage");
        }
        return flags;
    }

    private MediaAsset resolveMedia(Long testId, Long mediaAssetId, String mediaType) {
        MediaAsset media = mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED));
        if (!media.getTest().getId().equals(testId) || !media.getMediaType().equals(mediaType)) {
            throw new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED);
        }
        return media;
    }

    private void validateTimeRange(Integer startMs, Integer endMs) {
        if (startMs != null && startMs < 0) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        if (endMs != null && startMs != null && endMs <= startMs) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private void validateListeningPart(QuestionGroup group) {
        int part = group.getTestPart().getPartNumber();
        if (part < 1 || part > 4) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private void validateTranscriptLineRequest(QuestionGroupTranscriptLinesUpdateRequest request) {
        if (request == null || request.getLines() == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        Map<Integer, Long> orderCounts = request.getLines().stream()
                .filter(line -> line != null && line.getOrderIndex() != null)
                .collect(Collectors.groupingBy(
                        QuestionGroupTranscriptLinesUpdateRequest.Line::getOrderIndex,
                        Collectors.counting()
                ));
        if (orderCounts.values().stream().anyMatch(count -> count > 1)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        for (QuestionGroupTranscriptLinesUpdateRequest.Line line : request.getLines()) {
            if (line == null || isBlank(line.getTextEn()) || line.getOrderIndex() == null || line.getOrderIndex() < 0) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            if (line.getSpeaker() != null && line.getSpeaker().length() > 100) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            if (line.getStartMs() != null && line.getStartMs() < 0) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            if (line.getEndMs() != null && line.getEndMs() < 0) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            if (line.getStartMs() != null && line.getEndMs() != null && line.getEndMs() <= line.getStartMs()) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void markNeedsReview(QuestionGroup group) {
        group.setReviewStatus("needs_review");
        group.setReviewedAt(null);
        group.setReviewedBy(null);
        questionGroupRepository.save(group);
    }

    private String normalizeReviewStatus(String reviewStatus) {
        String normalized = reviewStatus == null ? "" : reviewStatus.trim().toLowerCase();
        if (!normalized.equals("needs_review") && !normalized.equals("reviewed")) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return normalized;
    }

    private String normalizeSpeakerLines(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim()
                .replaceAll("[ \\t]*\\n+[ \\t]*", "\n");
        normalized = TOEIC_DIRECTION_LINE_PATTERN.matcher(normalized).replaceAll("");
        normalized = TOEIC_DIRECTION_PREFIX_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized
                .replaceAll("[ \\t]*\\n+[ \\t]*", "\n")
                .trim();
        return SPEAKER_LABEL_PATTERN.matcher(normalized).replaceAll("\n").trim();
    }
}
