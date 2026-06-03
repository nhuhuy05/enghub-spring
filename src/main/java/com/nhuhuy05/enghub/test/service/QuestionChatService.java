package com.nhuhuy05.enghub.test.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhuhuy05.enghub.ai.service.GeminiClientService;
import com.nhuhuy05.enghub.common.enums.AttemptMode;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.dto.QuestionChatRequest;
import com.nhuhuy05.enghub.test.entity.Answer;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.TestAttempt;
import com.nhuhuy05.enghub.test.entity.UserAnswer;
import com.nhuhuy05.enghub.test.repository.AnswerRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestAttemptRepository;
import com.nhuhuy05.enghub.test.repository.UserAnswerRepository;
import com.nhuhuy05.enghub.user.entity.User;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionChatService {
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    UserRepository userRepository;
    TestAttemptRepository testAttemptRepository;
    QuestionRepository questionRepository;
    AnswerRepository answerRepository;
    UserAnswerRepository userAnswerRepository;
    QuestionGroupAudioRepository questionGroupAudioRepository;
    QuestionGroupPassageRepository questionGroupPassageRepository;
    GeminiClientService geminiClientService;
    ObjectMapper objectMapper;
    ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    void shutdownExecutor() {
        executorService.shutdownNow();
    }

    @Transactional(readOnly = true)
    public SseEmitter stream(String userEmail, Long attemptId, Long questionId, QuestionChatRequest request) {
        ChatInput chatInput = buildChatInput(userEmail, attemptId, questionId, request);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        executorService.submit(() -> {
            try {
                geminiClientService.streamPracticeQuestionChat(
                        chatInput.context(),
                        chatInput.message(),
                        chunk -> sendDelta(emitter, chunk)
                );
                emitter.send(SseEmitter.event().name("done").data(Map.of()));
                emitter.complete();
            } catch (Exception exception) {
                sendError(emitter, exception);
                emitter.completeWithError(exception);
            }
        });

        return emitter;
    }

    ChatInput buildChatInput(String userEmail, Long attemptId, Long questionId, QuestionChatRequest request) {
        String message = request == null ? null : request.getMessage();
        if (message == null || message.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        TestAttempt attempt = testAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_EXISTED));
        if (attempt.getMode() != AttemptMode.PRACTICE || attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.ATTEMPT_INVALID_STATE);
        }

        List<Integer> partNumbers = parsePartNumbers(attempt.getSelectedPartNumbers());
        if (!questionRepository.existsByIdAndTestIdAndPartNumbers(questionId, attempt.getTest().getId(), partNumbers)) {
            throw new AppException(ErrorCode.QUESTION_NOT_EXISTED);
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_EXISTED));
        List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(questionId);
        UserAnswer userAnswer = userAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElse(null);
        boolean answered = userAnswer != null && userAnswer.getSelectedAnswerId() != null;

        return new ChatInput(buildContext(attempt, question, answers, userAnswer, answered), message.trim());
    }

    private ObjectNode buildContext(
            TestAttempt attempt,
            Question question,
            List<Answer> answers,
            UserAnswer userAnswer,
            boolean answered
    ) {
        QuestionGroup group = question.getQuestionGroup();
        int partNumber = group.getTestPart().getPartNumber();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("attempt_id", attempt.getId());
        root.put("test_id", attempt.getTest().getId());
        root.put("practice_mode", true);
        root.put("answered", answered);
        root.put("part_number", partNumber);
        root.put("group_order", group.getOrderIndex());
        root.put("question_id", question.getId());
        root.put("question_number", question.getQuestionNumber());
        root.put("question_text_en", emptyIfNull(question.getQuestionTextEn()));
        root.put("question_text_vi", emptyIfNull(question.getQuestionTextVi()));

        ArrayNode answerArray = root.putArray("answers");
        for (int i = 0; i < answers.size(); i++) {
            Answer answer = answers.get(i);
            ObjectNode answerNode = answerArray.addObject();
            answerNode.put("answer_id", answer.getId());
            answerNode.put("label", answerLabel(i));
            answerNode.put("answer_text_en", emptyIfNull(answer.getAnswerTextEn()));
            answerNode.put("answer_text_vi", emptyIfNull(answer.getAnswerTextVi()));
            if (answered) {
                answerNode.put("is_correct", answer.isCorrect());
            }
        }

        if (answered) {
            Long selectedAnswerId = userAnswer.getSelectedAnswerId();
            root.put("selected_answer_id", selectedAnswerId);
            root.put("selected_answer_label", answerLabel(answers, selectedAnswerId));
            root.put("correct_answer_id", correctAnswerId(answers));
            root.put("correct_answer_label", answerLabel(answers, correctAnswerId(answers)));
            root.put("explanation_vi", emptyIfNull(question.getExplanationVi()));
        }

        addListeningContext(root, group, answered);
        addPassageContext(root, group, answered);
        return root;
    }

    private void addListeningContext(ObjectNode root, QuestionGroup group, boolean answered) {
        int partNumber = group.getTestPart().getPartNumber();
        if (!answered || partNumber < 1 || partNumber > 4) {
            return;
        }
        questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(group.getId(), 0).ifPresent(audio -> {
            ObjectNode audioNode = root.putObject("audio");
            audioNode.put("transcript_en", emptyIfNull(audio.getTranscriptEn()));
            audioNode.put("transcript_vi", emptyIfNull(audio.getTranscriptVi()));
        });
    }

    private void addPassageContext(ObjectNode root, QuestionGroup group, boolean answered) {
        int partNumber = group.getTestPart().getPartNumber();
        if (!answered || (partNumber != 6 && partNumber != 7)) {
            return;
        }

        ArrayNode passageArray = root.putArray("passages");
        questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(group.getId())
                .forEach(passage -> {
                    ObjectNode passageNode = passageArray.addObject();
                    passageNode.put("title", emptyIfNull(passage.getTitle()));
                    passageNode.put("content_en", emptyIfNull(passage.getContentEn()));
                    passageNode.put("content_vi", emptyIfNull(passage.getContentVi()));
                    passageNode.put("media_label", passage.getMediaAsset() == null ? "" : emptyIfNull(passage.getMediaAsset().getLabel()));
                    passageNode.put("order_index", passage.getOrderIndex());
                });
    }

    private List<Integer> parsePartNumbers(String partNumbers) {
        if (partNumbers == null || partNumbers.isBlank()) {
            return List.of(1, 2, 3, 4, 5, 6, 7);
        }
        return List.of(partNumbers.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::parseInt)
                .toList();
    }

    private Long correctAnswerId(List<Answer> answers) {
        return answers.stream()
                .filter(Answer::isCorrect)
                .map(Answer::getId)
                .findFirst()
                .orElse(null);
    }

    private String answerLabel(List<Answer> answers, Long answerId) {
        if (answerId == null) {
            return null;
        }
        for (int i = 0; i < answers.size(); i++) {
            if (answerId.equals(answers.get(i).getId())) {
                return answerLabel(i);
            }
        }
        return null;
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

    private void sendDelta(SseEmitter emitter, String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("delta").data(Map.of("text", chunk)));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to send chat stream chunk", exception);
        }
    }

    private void sendError(SseEmitter emitter, Exception exception) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of(
                    "message", exception instanceof AppException appException
                            ? appException.getErrorCode().getMessage()
                            : ErrorCode.GEMINI_GENERATION_FAILED.getMessage()
            )));
        } catch (IOException ignored) {
            // The client may have disconnected; completion handles cleanup.
        }
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    record ChatInput(JsonNode context, String message) {
    }
}
