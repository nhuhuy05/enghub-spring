package com.nhuhuy05.enghub.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhuhuy05.enghub.ai.dto.AiGenerationRequest;
import com.nhuhuy05.enghub.ai.dto.GeminiTranscriptResult;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.reading.entity.QuestionGroupPassage;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.dto.QuestionGroupDetailResponse;
import com.nhuhuy05.enghub.test.entity.Answer;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.QuestionGroupImage;
import com.nhuhuy05.enghub.test.repository.AnswerRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupImageRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.service.QuestionGroupReviewService;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionGroupAiService {
    GeminiClientService geminiClientService;
    QuestionGroupReviewService questionGroupReviewService;
    QuestionGroupRepository questionGroupRepository;
    QuestionGroupImageRepository questionGroupImageRepository;
    QuestionGroupAudioRepository questionGroupAudioRepository;
    QuestionGroupPassageRepository questionGroupPassageRepository;
    QuestionRepository questionRepository;
    AnswerRepository answerRepository;
    ObjectMapper objectMapper;

    @Transactional
    public QuestionGroupDetailResponse generateTranscript(Long groupId) {
        generateTranscriptStep(groupId, true, false);
        return questionGroupReviewService.getQuestionGroup(groupId);
    }

    private void generateTranscriptStep(Long groupId, boolean overwrite, boolean skipMissingAudio) {
        QuestionGroup group = getGroup(groupId);
        QuestionGroupAudio audio = questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(groupId, 0)
                .orElse(null);
        if (audio == null) {
            if (skipMissingAudio) {
                return;
            }
            throw new AppException(ErrorCode.AI_AUDIO_NOT_EXISTED);
        }

        if (!overwrite && !isBlank(audio.getTranscriptEn()) && !isBlank(audio.getTranscriptVi())
                && !needsListeningAnswerText(group)) {
            return;
        }

        GeminiTranscriptResult result = geminiClientService.generateTranscript(
                audio.getMediaAsset(),
                group.getTestPart().getPartNumber()
        );
        if (overwrite || isBlank(audio.getTranscriptEn())) {
            audio.setTranscriptEn(result.transcriptEn());
        }
        if (overwrite || isBlank(audio.getTranscriptVi())) {
            audio.setTranscriptVi(result.transcriptVi());
        }
        questionGroupAudioRepository.save(audio);
        applyListeningAnswerTexts(group, result, overwrite);
        markNeedsReview(group);
    }

    @Transactional
    public QuestionGroupDetailResponse generateQuestionTranslation(Long groupId) {
        generateQuestionTranslationStep(groupId, true);
        return questionGroupReviewService.getQuestionGroup(groupId);
    }

    private void generateQuestionTranslationStep(Long groupId, boolean overwrite) {
        QuestionGroup group = getGroup(groupId);
        List<Question> questions = questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(groupId);
        if (questions.isEmpty()) {
            return;
        }
        if (!overwrite && !needsQuestionTranslation(questions)) {
            return;
        }

        JsonNode result = geminiClientService.generateQuestionTranslation(questionInput(group, questions, false));
        JsonNode questionNodes = result.path("questions");
        if (!questionNodes.isArray()) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
        }

        Map<Long, Question> questionsById = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, Answer> answersById = answersById(questions);
        for (JsonNode questionNode : questionNodes) {
            Long questionId = longOrNull(questionNode, "question_id");
            Question question = questionId == null ? null : questionsById.get(questionId);
            if (question == null) {
                continue;
            }
            String questionTextVi = textOrNull(questionNode, "question_text_vi");
            if (overwrite || isBlank(question.getQuestionTextVi())) {
                question.setQuestionTextVi(questionTextVi);
                questionRepository.save(question);
            }

            JsonNode answerNodes = questionNode.path("answers");
            if (answerNodes.isArray()) {
                for (JsonNode answerNode : answerNodes) {
                    Long answerId = longOrNull(answerNode, "answer_id");
                    Answer answer = answerId == null ? null : answersById.get(answerId);
                    if (answer == null) {
                        continue;
                    }
                    String answerTextVi = textOrNull(answerNode, "answer_text_vi");
                    if (overwrite || isBlank(answer.getAnswerTextVi())) {
                        answer.setAnswerTextVi(answerTextVi);
                        answerRepository.save(answer);
                    }
                }
            }
        }

        markNeedsReview(group);
    }

    @Transactional
    public QuestionGroupDetailResponse generateExplanations(Long groupId) {
        generateExplanationsStep(groupId, true);
        return questionGroupReviewService.getQuestionGroup(groupId);
    }

    private void generateExplanationsStep(Long groupId, boolean overwrite) {
        QuestionGroup group = getGroup(groupId);
        List<Question> questions = questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(groupId);
        if (questions.isEmpty()) {
            return;
        }
        if (!overwrite && !needsExplanations(questions)) {
            return;
        }
        validateExplanationContext(group, questions);

        JsonNode result = geminiClientService.generateExplanations(
                questionInput(group, questions, true),
                visualAssets(groupId)
        );
        JsonNode questionNodes = result.path("questions");
        if (!questionNodes.isArray()) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE);
        }

        Map<Long, Question> questionsById = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        for (JsonNode questionNode : questionNodes) {
            Long questionId = longOrNull(questionNode, "question_id");
            Question question = questionId == null ? null : questionsById.get(questionId);
            if (question == null) {
                continue;
            }
            String explanationVi = textOrNull(questionNode, "explanation_vi");
            if (overwrite || isBlank(question.getExplanationVi())) {
                question.setExplanationVi(explanationVi);
                questionRepository.save(question);
            }
        }

        markNeedsReview(group);
    }

    @Transactional
    public QuestionGroupDetailResponse generateAiSupport(Long groupId, AiGenerationRequest request) {
        boolean overwrite = request != null && request.overwriteEnabled();
        if (request == null || request.transcriptEnabled()) {
            generateTranscriptStep(groupId, overwrite, true);
        }
        if (request == null || request.questionTranslationEnabled()) {
            generateQuestionTranslationStep(groupId, overwrite);
        }
        if (request == null || request.explanationEnabled()) {
            generateExplanationsStep(groupId, overwrite);
        }
        return questionGroupReviewService.getQuestionGroup(groupId);
    }

    private ObjectNode questionInput(QuestionGroup group, List<Question> questions, boolean includeContext) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("group_id", group.getId());
        root.put("part_number", group.getTestPart().getPartNumber());
        root.put("group_order", group.getOrderIndex());

        if (includeContext) {
            addContext(root, group.getId());
        }

        ArrayNode questionArray = root.putArray("questions");
        for (Question question : questions) {
            ObjectNode questionNode = questionArray.addObject();
            questionNode.put("question_id", question.getId());
            questionNode.put("question_number", question.getQuestionNumber());
            questionNode.put("question_text_en", emptyIfNull(question.getQuestionTextEn()));
            questionNode.put("question_text_vi", emptyIfNull(question.getQuestionTextVi()));
            questionNode.put("explanation_vi", emptyIfNull(question.getExplanationVi()));

            ArrayNode answerArray = questionNode.putArray("answers");
            List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(question.getId());
            for (int i = 0; i < answers.size(); i++) {
                Answer answer = answers.get(i);
                ObjectNode answerNode = answerArray.addObject();
                answerNode.put("answer_id", answer.getId());
                answerNode.put("label", answerLabel(i));
                answerNode.put("answer_text_en", emptyIfNull(answer.getAnswerTextEn()));
                answerNode.put("answer_text_vi", emptyIfNull(answer.getAnswerTextVi()));
                answerNode.put("is_correct", answer.isCorrect());
            }
        }
        return root;
    }

    private void addContext(ObjectNode root, Long groupId) {
        questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(groupId, 0).ifPresent(audio -> {
            ObjectNode audioNode = root.putObject("audio");
            audioNode.put("transcript_en", emptyIfNull(audio.getTranscriptEn()));
            audioNode.put("transcript_vi", emptyIfNull(audio.getTranscriptVi()));
        });

        List<QuestionGroupPassage> passages = questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId);
        ArrayNode passageArray = root.putArray("passages");
        for (QuestionGroupPassage passage : passages) {
            ObjectNode passageNode = passageArray.addObject();
            passageNode.put("passage_id", passage.getId());
            passageNode.put("title", emptyIfNull(passage.getTitle()));
            passageNode.put("media_asset_id", passage.getMediaAsset() == null ? null : passage.getMediaAsset().getId());
            passageNode.put("media_label", passage.getMediaAsset() == null ? "" : emptyIfNull(passage.getMediaAsset().getLabel()));
            passageNode.put("content_en", emptyIfNull(passage.getContentEn()));
            passageNode.put("content_vi", emptyIfNull(passage.getContentVi()));
        }

        List<QuestionGroupImage> images = questionGroupImageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId);
        ArrayNode imageArray = root.putArray("images");
        for (QuestionGroupImage image : images) {
            ObjectNode imageNode = imageArray.addObject();
            imageNode.put("media_asset_id", image.getMediaAsset().getId());
            imageNode.put("label", emptyIfNull(image.getMediaAsset().getLabel()));
            imageNode.put("order_index", image.getOrderIndex());
        }
    }

    private QuestionGroup getGroup(Long groupId) {
        return questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_GROUP_NOT_EXISTED));
    }

    private Map<Long, Answer> answersById(List<Question> questions) {
        return questions.stream()
                .flatMap(question -> answerRepository.findAllByQuestionIdOrderByIdAsc(question.getId()).stream())
                .collect(Collectors.toMap(Answer::getId, Function.identity()));
    }

    private List<MediaAsset> visualAssets(Long groupId) {
        Map<Long, MediaAsset> assetsById = new LinkedHashMap<>();
        questionGroupImageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId).stream()
                .map(QuestionGroupImage::getMediaAsset)
                .forEach(asset -> assetsById.putIfAbsent(asset.getId(), asset));

        questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId).stream()
                .map(QuestionGroupPassage::getMediaAsset)
                .filter(asset -> asset != null && "image".equals(asset.getMediaType()))
                .forEach(asset -> assetsById.putIfAbsent(asset.getId(), asset));

        return List.copyOf(assetsById.values());
    }

    private boolean needsQuestionTranslation(List<Question> questions) {
        for (Question question : questions) {
            if (!isBlank(question.getQuestionTextEn()) && isBlank(question.getQuestionTextVi())) {
                return true;
            }
            List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(question.getId());
            for (Answer answer : answers) {
                if (!isBlank(answer.getAnswerTextEn()) && isBlank(answer.getAnswerTextVi())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean needsListeningAnswerText(QuestionGroup group) {
        int part = group.getTestPart().getPartNumber();
        if (part != 1 && part != 2) {
            return false;
        }

        List<Question> questions = questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(group.getId());
        if (questions.isEmpty()) {
            return false;
        }

        return answerRepository.findAllByQuestionIdOrderByIdAsc(questions.get(0).getId()).stream()
                .anyMatch(answer -> isBlank(answer.getAnswerTextEn()) || isBlank(answer.getAnswerTextVi()));
    }

    private void applyListeningAnswerTexts(QuestionGroup group, GeminiTranscriptResult result, boolean overwrite) {
        int part = group.getTestPart().getPartNumber();
        if ((part != 1 && part != 2) || result.answers().isEmpty()) {
            return;
        }

        List<Question> questions = questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(group.getId());
        if (questions.isEmpty()) {
            return;
        }

        Map<String, GeminiTranscriptResult.AnswerLine> generatedAnswers = result.answers().stream()
                .filter(answer -> !isBlank(answer.label()))
                .collect(Collectors.toMap(
                        answer -> answer.label().trim().toUpperCase(),
                        Function.identity(),
                        (first, ignored) -> first
                ));

        List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(questions.get(0).getId());
        for (int i = 0; i < answers.size(); i++) {
            Answer answer = answers.get(i);
            GeminiTranscriptResult.AnswerLine generatedAnswer = generatedAnswers.get(answerLabel(i));
            if (generatedAnswer == null) {
                continue;
            }

            boolean changed = false;
            if (generatedAnswer.answerTextEn() != null && (overwrite || isBlank(answer.getAnswerTextEn()))) {
                answer.setAnswerTextEn(generatedAnswer.answerTextEn());
                changed = true;
            }
            if (generatedAnswer.answerTextVi() != null && (overwrite || isBlank(answer.getAnswerTextVi()))) {
                answer.setAnswerTextVi(generatedAnswer.answerTextVi());
                changed = true;
            }
            if (changed) {
                answerRepository.save(answer);
            }
        }
    }

    private boolean needsExplanations(List<Question> questions) {
        return questions.stream().anyMatch(question -> isBlank(question.getExplanationVi()));
    }

    private void validateExplanationContext(QuestionGroup group, List<Question> questions) {
        for (Question question : questions) {
            List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(question.getId());
            long correctCount = answers.stream().filter(Answer::isCorrect).count();
            if (answers.isEmpty() || correctCount != 1) {
                throw new AppException(ErrorCode.AI_MISSING_REQUIRED_CONTEXT);
            }
        }

        int part = group.getTestPart().getPartNumber();
        Long groupId = group.getId();
        boolean hasTranscript = questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(groupId, 0)
                .map(audio -> !isBlank(audio.getTranscriptEn()))
                .orElse(false);
        boolean hasGroupImage = !questionGroupImageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId).isEmpty();
        boolean hasPassageContext = questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId).stream()
                .anyMatch(passage -> !isBlank(passage.getContentEn()) || passage.getMediaAsset() != null);

        if (part == 1 && (!hasTranscript || !hasGroupImage)) {
            throw new AppException(ErrorCode.AI_MISSING_REQUIRED_CONTEXT);
        }
        if (part >= 2 && part <= 4 && !hasTranscript) {
            throw new AppException(ErrorCode.AI_MISSING_REQUIRED_CONTEXT);
        }
        if ((part == 6 || part == 7) && !hasPassageContext) {
            throw new AppException(ErrorCode.AI_MISSING_REQUIRED_CONTEXT);
        }
        if (part == 5 && questions.stream().anyMatch(question -> isBlank(question.getQuestionTextEn()))) {
            throw new AppException(ErrorCode.AI_MISSING_REQUIRED_CONTEXT);
        }
    }

    private void markNeedsReview(QuestionGroup group) {
        group.setReviewStatus("needs_review");
        group.setReviewedAt(null);
        group.setReviewedBy(null);
        questionGroupRepository.save(group);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String answerLabel(int index) {
        return switch (index) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";
            default -> String.valueOf(index + 1);
        };
    }
}
