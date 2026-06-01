package com.nhuhuy05.enghub.reading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhuhuy05.enghub.ai.service.GeminiClientService;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.media.repository.MediaAssetRepository;
import com.nhuhuy05.enghub.reading.dto.*;
import com.nhuhuy05.enghub.reading.entity.QuestionGroupPassage;
import com.nhuhuy05.enghub.reading.entity.ReadingLesson;
import com.nhuhuy05.enghub.reading.entity.ReadingVocabularyHint;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonStatus;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.reading.repository.ReadingLessonRepository;
import com.nhuhuy05.enghub.reading.repository.ReadingVocabularyHintRepository;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingLessonService {
    ReadingLessonRepository readingLessonRepository;
    ReadingVocabularyHintRepository readingVocabularyHintRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionRepository questionRepository;
    QuestionGroupPassageRepository questionGroupPassageRepository;
    MediaAssetRepository mediaAssetRepository;
    GeminiClientService geminiClientService;
    ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ReadingPart7CandidateResponse> getPart7Candidates(Long testId) {
        return questionGroupRepository.findAllPart7Candidates(testId).stream()
                .map(this::toCandidateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingLessonListItemResponse> getAdminLessons(
            ReadingLessonStatus status,
            ReadingLessonType readingType
    ) {
        List<ReadingLesson> lessons;
        if (status != null && readingType != null) {
            lessons = readingLessonRepository.findAllByStatusAndReadingTypeOrderByUpdatedAtDesc(status, readingType);
        } else if (status != null) {
            lessons = readingLessonRepository.findAllByStatusOrderByUpdatedAtDesc(status);
        } else if (readingType != null) {
            lessons = readingLessonRepository.findAllByReadingTypeOrderByUpdatedAtDesc(readingType);
        } else {
            lessons = readingLessonRepository.findAllByOrderByUpdatedAtDesc();
        }
        return lessons.stream().map(this::toListItemResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingLessonListItemResponse> getPublishedLessons(ReadingLessonType readingType) {
        List<ReadingLesson> lessons = readingType == null
                ? readingLessonRepository.findAllByStatusOrderByUpdatedAtDesc(ReadingLessonStatus.PUBLISHED)
                : readingLessonRepository.findAllByStatusAndReadingTypeOrderByUpdatedAtDesc(
                        ReadingLessonStatus.PUBLISHED,
                        readingType
                );
        return lessons.stream()
                .map(this::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReadingLessonResponse getAdminLesson(Long lessonId) {
        return toResponse(getLesson(lessonId));
    }

    @Transactional(readOnly = true)
    public ReadingLessonResponse getPublishedLesson(Long lessonId) {
        ReadingLesson lesson = readingLessonRepository.findById(lessonId)
                .filter(existing -> existing.getStatus() == ReadingLessonStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.READING_LESSON_NOT_EXISTED));
        return toResponse(lesson);
    }

    @Transactional
    public ReadingLessonResponse createFromQuestionGroup(ReadingLessonCreateRequest request) {
        QuestionGroup group = getPart7Group(request.getQuestionGroupId());
        if (readingLessonRepository.existsByQuestionGroupId(group.getId())) {
            throw new AppException(ErrorCode.READING_LESSON_EXISTED);
        }

        List<QuestionGroupPassage> passages = passages(group.getId());
        ReadingLesson lesson = ReadingLesson.builder()
                .questionGroup(group)
                .title(firstNonBlank(request.getTitle(), firstPassageTitle(passages), group.getTitle(), group.getTestPart().getTest().getTitle()))
                .titleVi(blankToNull(request.getTitleVi()))
                .readingType(request.getReadingType() == null ? inferReadingType(passages.size()) : request.getReadingType())
                .status(request.getStatus() == null ? ReadingLessonStatus.DRAFT : request.getStatus())
                .difficulty(blankToNull(request.getDifficulty()))
                .build();

        validateBeforePublish(lesson, passages);
        return toResponse(readingLessonRepository.save(lesson));
    }

    @Transactional
    public ReadingLessonResponse updateLesson(Long lessonId, ReadingLessonUpdateRequest request) {
        ReadingLesson lesson = getLesson(lessonId);
        if (request.getTitle() != null) {
            lesson.setTitle(requireText(request.getTitle()));
        }
        if (request.getTitleVi() != null) {
            lesson.setTitleVi(blankToNull(request.getTitleVi()));
        }
        if (request.getReadingType() != null) {
            lesson.setReadingType(request.getReadingType());
        }
        if (request.getStatus() != null) {
            lesson.setStatus(request.getStatus());
        }
        if (request.getDifficulty() != null) {
            lesson.setDifficulty(blankToNull(request.getDifficulty()));
        }

        if (request.getPassages() != null) {
            replacePassages(lesson, request.getPassages());
        }
        if (request.getVocabularyHints() != null) {
            replaceVocabularyHints(lesson, request.getVocabularyHints());
        }

        validateBeforePublish(lesson, passages(lesson.getQuestionGroup().getId()));
        return toResponse(readingLessonRepository.save(lesson));
    }

    @Transactional
    public ReadingLessonResponse updateStatus(Long lessonId, ReadingLessonStatus status) {
        if (status == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        ReadingLesson lesson = getLesson(lessonId);
        lesson.setStatus(status);
        validateBeforePublish(lesson, passages(lesson.getQuestionGroup().getId()));
        return toResponse(readingLessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        ReadingLesson lesson = getLesson(lessonId);
        readingLessonRepository.delete(lesson);
    }

    @Transactional
    public ReadingLessonResponse generateTranslation(Long lessonId, ReadingAiGenerationRequest request) {
        ReadingLesson lesson = getLesson(lessonId);
        boolean overwrite = request != null && request.overwriteEnabled();
        JsonNode result = geminiClientService.generateReadingTranslation(readingInput(lesson), visualAssets(lesson));
        JsonNode passageNodes = result.path("passages");
        if (!passageNodes.isArray()) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
        }

        String titleVi = textOrNull(result, "title_vi");
        if (!isBlank(titleVi) && (overwrite || isBlank(lesson.getTitleVi()))) {
            lesson.setTitleVi(titleVi.trim());
            readingLessonRepository.save(lesson);
        }

        Map<Long, QuestionGroupPassage> passagesById = passages(lesson.getQuestionGroup().getId()).stream()
                .collect(Collectors.toMap(QuestionGroupPassage::getId, Function.identity()));
        for (JsonNode passageNode : passageNodes) {
            Long passageId = longOrNull(passageNode, "passage_id");
            QuestionGroupPassage passage = passageId == null ? null : passagesById.get(passageId);
            if (passage == null) {
                continue;
            }
            boolean changed = false;
            String contentEn = textOrNull(passageNode, "content_en");
            if (!isBlank(contentEn) && (overwrite || isBlank(passage.getContentEn()))) {
                passage.setContentEn(contentEn);
                changed = true;
            }
            String contentVi = textOrNull(passageNode, "content_vi");
            if (!isBlank(contentVi) && (overwrite || isBlank(passage.getContentVi()))) {
                passage.setContentVi(contentVi);
                changed = true;
            }
            if (changed) {
                questionGroupPassageRepository.save(passage);
            }
        }
        return toResponse(lesson);
    }

    @Transactional
    public ReadingLessonResponse generateVocabulary(Long lessonId, ReadingAiGenerationRequest request) {
        ReadingLesson lesson = getLesson(lessonId);
        boolean overwrite = request != null && request.overwriteEnabled();
        JsonNode result = geminiClientService.generateReadingVocabulary(readingInput(lesson));
        JsonNode hintNodes = result.path("vocabulary_hints");
        if (!hintNodes.isArray()) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
        }

        if (overwrite) {
            readingVocabularyHintRepository.deleteAllByReadingLessonId(lessonId);
            readingVocabularyHintRepository.flush();
        }

        Set<String> existingWords = readingVocabularyHintRepository.findAllByReadingLessonIdOrderByOrderIndexAscIdAsc(lessonId).stream()
                .map(hint -> hint.getWord().trim().toLowerCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, QuestionGroupPassage> passagesById = passages(lesson.getQuestionGroup().getId()).stream()
                .collect(Collectors.toMap(QuestionGroupPassage::getId, Function.identity()));

        int orderIndex = existingWords.size();
        for (JsonNode hintNode : hintNodes) {
            String word = textOrNull(hintNode, "word");
            String meaningVi = textOrNull(hintNode, "meaning_vi");
            if (isBlank(word) || isBlank(meaningVi)) {
                continue;
            }
            String normalizedWord = word.trim().toLowerCase();
            if (!existingWords.add(normalizedWord)) {
                continue;
            }

            Long passageId = longOrNull(hintNode, "passage_id");
            ReadingVocabularyHint hint = ReadingVocabularyHint.builder()
                    .readingLesson(lesson)
                    .passage(passageId == null ? null : passagesById.get(passageId))
                    .word(word.trim())
                    .partOfSpeech(blankToNull(textOrNull(hintNode, "part_of_speech")))
                    .meaningVi(meaningVi.trim())
                    .orderIndex(orderIndex++)
                    .build();
            readingVocabularyHintRepository.save(hint);
        }
        return toResponse(lesson);
    }

    @Transactional
    public ReadingLessonResponse generateAiSupport(Long lessonId, ReadingAiGenerationRequest request) {
        generateTranslation(lessonId, request);
        return generateVocabulary(lessonId, request);
    }

    private void replacePassages(ReadingLesson lesson, List<ReadingLessonUpdateRequest.PassageItem> passageItems) {
        if (passageItems == null || passageItems.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        QuestionGroup group = lesson.getQuestionGroup();
        Long testId = group.getTestPart().getTest().getId();

        readingVocabularyHintRepository.deleteAllByReadingLessonId(lesson.getId());
        readingVocabularyHintRepository.flush();
        questionGroupPassageRepository.deleteAllByQuestionGroupId(group.getId());
        questionGroupPassageRepository.flush();

        int fallbackOrder = 0;
        for (ReadingLessonUpdateRequest.PassageItem item : passageItems) {
            int orderIndex = item.getOrderIndex() == null ? fallbackOrder : item.getOrderIndex();
            MediaAsset media = item.getMediaAssetId() == null ? null : resolveImage(testId, item.getMediaAssetId());
            questionGroupPassageRepository.save(QuestionGroupPassage.builder()
                    .questionGroup(group)
                    .mediaAsset(media)
                    .title(blankToNull(item.getTitle()))
                    .passageType(blankToNull(item.getPassageType()))
                    .contentFormat(blankToNull(item.getContentFormat()))
                    .contentEn(blankToNull(item.getContentEn()))
                    .contentVi(blankToNull(item.getContentVi()))
                    .orderIndex(orderIndex)
                    .build());
            fallbackOrder++;
        }
        questionGroupPassageRepository.flush();
    }

    private void replaceVocabularyHints(ReadingLesson lesson, List<ReadingLessonUpdateRequest.VocabularyHintItem> hintItems) {
        readingVocabularyHintRepository.deleteAllByReadingLessonId(lesson.getId());
        readingVocabularyHintRepository.flush();
        if (hintItems == null) {
            return;
        }

        Map<Long, QuestionGroupPassage> passagesById = passages(lesson.getQuestionGroup().getId()).stream()
                .collect(Collectors.toMap(QuestionGroupPassage::getId, Function.identity()));
        Map<Integer, QuestionGroupPassage> passagesByOrder = passagesById.values().stream()
                .collect(Collectors.toMap(QuestionGroupPassage::getOrderIndex, Function.identity(), (first, ignored) -> first));

        int fallbackOrder = 0;
        for (ReadingLessonUpdateRequest.VocabularyHintItem item : hintItems) {
            if (isBlank(item.getWord()) || isBlank(item.getMeaningVi())) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            QuestionGroupPassage passage = null;
            if (item.getPassageId() != null) {
                passage = passagesById.get(item.getPassageId());
                if (passage == null) {
                    throw new AppException(ErrorCode.PASSAGE_NOT_EXISTED);
                }
            } else if (item.getPassageOrderIndex() != null) {
                passage = passagesByOrder.get(item.getPassageOrderIndex());
            }

            readingVocabularyHintRepository.save(ReadingVocabularyHint.builder()
                    .readingLesson(lesson)
                    .passage(passage)
                    .word(item.getWord().trim())
                    .partOfSpeech(blankToNull(item.getPartOfSpeech()))
                    .meaningVi(item.getMeaningVi().trim())
                    .orderIndex(item.getOrderIndex() == null ? fallbackOrder : item.getOrderIndex())
                    .build());
            fallbackOrder++;
        }
    }

    private ObjectNode readingInput(ReadingLesson lesson) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("lesson_id", lesson.getId());
        root.put("title", lesson.getTitle());
        root.put("title_vi", emptyIfNull(lesson.getTitleVi()));
        root.put("reading_type", lesson.getReadingType().name());

        ArrayNode passageArray = root.putArray("passages");
        for (QuestionGroupPassage passage : passages(lesson.getQuestionGroup().getId())) {
            ObjectNode passageNode = passageArray.addObject();
            passageNode.put("passage_id", passage.getId());
            passageNode.put("order_index", passage.getOrderIndex());
            passageNode.put("title", emptyIfNull(passage.getTitle()));
            passageNode.put("passage_type", emptyIfNull(passage.getPassageType()));
            passageNode.put("content_en", emptyIfNull(passage.getContentEn()));
            passageNode.put("content_vi", emptyIfNull(passage.getContentVi()));
            passageNode.put("media_asset_id", passage.getMediaAsset() == null ? null : passage.getMediaAsset().getId());
            passageNode.put("media_label", passage.getMediaAsset() == null ? "" : emptyIfNull(passage.getMediaAsset().getLabel()));
            passageNode.put("visual_asset_order", visualAssetOrder(passage));
        }
        return root;
    }

    private ReadingPart7CandidateResponse toCandidateResponse(QuestionGroup group) {
        List<QuestionGroupPassage> passages = passages(group.getId());
        ReadingLesson existingLesson = readingLessonRepository.findByQuestionGroupId(group.getId()).orElse(null);
        return ReadingPart7CandidateResponse.builder()
                .questionGroupId(group.getId())
                .testId(group.getTestPart().getTest().getId())
                .testTitle(group.getTestPart().getTest().getTitle())
                .groupOrder(group.getOrderIndex())
                .questionNumbers(questionNumbers(group.getId()))
                .passageCount(passages.size())
                .suggestedReadingType(inferReadingType(passages.size()))
                .existingLessonId(existingLesson == null ? null : existingLesson.getId())
                .title(firstNonBlank(firstPassageTitle(passages), group.getTitle()))
                .build();
    }

    private ReadingLessonListItemResponse toListItemResponse(ReadingLesson lesson) {
        Long lessonId = lesson.getId();
        Long groupId = lesson.getQuestionGroup().getId();
        return ReadingLessonListItemResponse.builder()
                .id(lessonId)
                .questionGroupId(groupId)
                .testId(lesson.getQuestionGroup().getTestPart().getTest().getId())
                .testTitle(lesson.getQuestionGroup().getTestPart().getTest().getTitle())
                .groupOrder(lesson.getQuestionGroup().getOrderIndex())
                .title(lesson.getTitle())
                .titleVi(lesson.getTitleVi())
                .readingType(lesson.getReadingType())
                .status(lesson.getStatus())
                .difficulty(lesson.getDifficulty())
                .passageCount(passages(groupId).size())
                .vocabularyCount(readingVocabularyHintRepository.findAllByReadingLessonIdOrderByOrderIndexAscIdAsc(lessonId).size())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    private ReadingLessonResponse toResponse(ReadingLesson lesson) {
        Long groupId = lesson.getQuestionGroup().getId();
        return ReadingLessonResponse.builder()
                .id(lesson.getId())
                .questionGroupId(groupId)
                .testId(lesson.getQuestionGroup().getTestPart().getTest().getId())
                .testTitle(lesson.getQuestionGroup().getTestPart().getTest().getTitle())
                .groupOrder(lesson.getQuestionGroup().getOrderIndex())
                .title(lesson.getTitle())
                .titleVi(lesson.getTitleVi())
                .readingType(lesson.getReadingType())
                .status(lesson.getStatus())
                .difficulty(lesson.getDifficulty())
                .passages(passages(groupId).stream().map(this::toPassageResponse).toList())
                .vocabularyHints(readingVocabularyHintRepository.findAllByReadingLessonIdOrderByOrderIndexAscIdAsc(lesson.getId()).stream()
                        .map(this::toVocabularyHintResponse)
                        .toList())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    private PassageResponse toPassageResponse(QuestionGroupPassage passage) {
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
                .mediaLabel(passage.getMediaAsset() == null ? null : passage.getMediaAsset().getLabel())
                .mediaUrl(passage.getMediaAsset() == null ? null : passage.getMediaAsset().getUrl())
                .orderIndex(passage.getOrderIndex())
                .build();
    }

    private ReadingVocabularyHintResponse toVocabularyHintResponse(ReadingVocabularyHint hint) {
        return ReadingVocabularyHintResponse.builder()
                .id(hint.getId())
                .passageId(hint.getPassage() == null ? null : hint.getPassage().getId())
                .passageOrderIndex(hint.getPassage() == null ? null : hint.getPassage().getOrderIndex())
                .word(hint.getWord())
                .partOfSpeech(hint.getPartOfSpeech())
                .meaningVi(hint.getMeaningVi())
                .orderIndex(hint.getOrderIndex())
                .build();
    }

    private List<MediaAsset> visualAssets(ReadingLesson lesson) {
        return passages(lesson.getQuestionGroup().getId()).stream()
                .map(QuestionGroupPassage::getMediaAsset)
                .filter(asset -> asset != null && "image".equals(asset.getMediaType()))
                .collect(Collectors.toMap(
                        MediaAsset::getId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ))
                .values().stream()
                .toList();
    }

    private Integer visualAssetOrder(QuestionGroupPassage passage) {
        MediaAsset mediaAsset = passage.getMediaAsset();
        if (mediaAsset == null || !"image".equals(mediaAsset.getMediaType())) {
            return null;
        }
        List<MediaAsset> assets = visualAssets(passage.getQuestionGroup().getId());
        for (int i = 0; i < assets.size(); i++) {
            if (assets.get(i).getId().equals(mediaAsset.getId())) {
                return i;
            }
        }
        return null;
    }

    private List<MediaAsset> visualAssets(Long questionGroupId) {
        return passages(questionGroupId).stream()
                .map(QuestionGroupPassage::getMediaAsset)
                .filter(asset -> asset != null && "image".equals(asset.getMediaType()))
                .collect(Collectors.toMap(
                        MediaAsset::getId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ))
                .values().stream()
                .toList();
    }

    private ReadingLesson getLesson(Long lessonId) {
        return readingLessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.READING_LESSON_NOT_EXISTED));
    }

    private QuestionGroup getPart7Group(Long groupId) {
        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_GROUP_NOT_EXISTED));
        if (!Objects.equals(group.getTestPart().getPartNumber(), 7)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return group;
    }

    private List<QuestionGroupPassage> passages(Long groupId) {
        return questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId);
    }

    private List<Integer> questionNumbers(Long groupId) {
        return questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(groupId).stream()
                .map(Question::getQuestionNumber)
                .toList();
    }

    private MediaAsset resolveImage(Long testId, Long mediaAssetId) {
        MediaAsset media = mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED));
        if (!media.getTest().getId().equals(testId) || !"image".equals(media.getMediaType())) {
            throw new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED);
        }
        return media;
    }

    private void validateBeforePublish(ReadingLesson lesson, List<QuestionGroupPassage> passages) {
        if (lesson.getStatus() != ReadingLessonStatus.PUBLISHED) {
            return;
        }
        if (isBlank(lesson.getTitle()) || passages.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        for (QuestionGroupPassage passage : passages) {
            if (isBlank(passage.getContentEn()) || isBlank(passage.getContentVi())) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }
    }

    private ReadingLessonType inferReadingType(int passageCount) {
        if (passageCount >= 3) {
            return ReadingLessonType.TRIPLE;
        }
        if (passageCount == 2) {
            return ReadingLessonType.DOUBLE;
        }
        return ReadingLessonType.SINGLE;
    }

    private String firstPassageTitle(List<QuestionGroupPassage> passages) {
        return passages.stream()
                .map(QuestionGroupPassage::getTitle)
                .filter(value -> !isBlank(value))
                .findFirst()
                .orElse(null);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String requireText(String value) {
        if (isBlank(value)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return value.trim();
    }

    private Long longOrNull(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull() || !value.canConvertToLong()) {
            return null;
        }
        return value.asLong();
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
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
}
