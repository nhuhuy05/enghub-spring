package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.enums.AttemptMode;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.dto.AttemptContentResponse;
import com.nhuhuy05.enghub.test.dto.AttemptResponse;
import com.nhuhuy05.enghub.test.dto.AttemptResultResponse;
import com.nhuhuy05.enghub.test.dto.AttemptSummaryResponse;
import com.nhuhuy05.enghub.test.dto.UserAnswerResponse;
import com.nhuhuy05.enghub.test.entity.Answer;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.Test;
import com.nhuhuy05.enghub.test.entity.TestAttempt;
import com.nhuhuy05.enghub.test.entity.UserAnswer;
import com.nhuhuy05.enghub.test.repository.AnswerRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupImageRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestAttemptRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import com.nhuhuy05.enghub.test.repository.UserAnswerRepository;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestAttemptService {
    UserRepository userRepository;
    TestRepository testRepository;
    QuestionRepository questionRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionGroupImageRepository questionGroupImageRepository;
    QuestionGroupAudioRepository questionGroupAudioRepository;
    QuestionGroupPassageRepository questionGroupPassageRepository;
    AnswerRepository answerRepository;
    UserAnswerRepository userAnswerRepository;
    TestAttemptRepository testAttemptRepository;

    @Transactional
    public AttemptResponse startAttempt(String userEmail, Long testId, AttemptMode mode, List<Integer> partNumbers) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));
        if (!Boolean.TRUE.equals(test.getPublished())) {
            throw new AppException(ErrorCode.TEST_NOT_EXISTED);
        }

        AttemptMode attemptMode = normalizeMode(mode);
        List<Integer> selectedParts = normalizePartNumbers(partNumbers);
        String selectedPartNumbers = toPartString(selectedParts);

        TestAttempt attempt = TestAttempt.builder()
                .user(user)
                .test(test)
                .mode(attemptMode)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .selectedPartNumbers(selectedPartNumbers)
                .build();

        return toAttemptResponse(testAttemptRepository.save(attempt));
    }

    @Transactional
    public AttemptResponse getAttempt(String userEmail, Long attemptId) {
        return toAttemptResponse(resolveExpiredAttempt(getAttemptForUser(userEmail, attemptId)));
    }

    @Transactional
    public Page<AttemptSummaryResponse> getAttempts(
            String userEmail,
            AttemptStatus status,
            Long testId,
            int page,
            int size
    ) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return testAttemptRepository.findAttempts(user.getId(), status, testId, PageRequest.of(safePage, safeSize))
                .map(attempt -> toAttemptSummary(resolveExpiredAttempt(attempt)));
    }

    @Transactional
    public AttemptContentResponse getAttemptContent(String userEmail, Long attemptId) {
        TestAttempt attempt = resolveExpiredAttempt(getAttemptForUser(userEmail, attemptId));
        List<Integer> partNumbers = parsePartNumbers(attempt.getSelectedPartNumbers());
        Test test = attempt.getTest();

        Map<Long, UserAnswer> answersByQuestionId = userAnswerRepository.findAllByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(UserAnswer::getQuestionId, answer -> answer));

        return AttemptContentResponse.builder()
                .attempt(toAttemptResponse(attempt))
                .testId(test.getId())
                .title(test.getTitle())
                .description(test.getDescription())
                .durationMinutes(test.getDurationMinutes())
                .parts(contentParts(test.getId(), partNumbers, answersByQuestionId))
                .build();
    }

    @Transactional
    public UserAnswerResponse saveAnswer(String userEmail, Long attemptId, Long questionId, Long selectedAnswerId) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var attempt = testAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_EXISTED));
        attempt = resolveExpiredAttempt(attempt);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.ATTEMPT_INVALID_STATE);
        }
        List<Integer> partNumbers = parsePartNumbers(attempt.getSelectedPartNumbers());
        if (!questionRepository.existsByIdAndTestIdAndPartNumbers(questionId, attempt.getTest().getId(), partNumbers)) {
            throw new AppException(ErrorCode.QUESTION_NOT_EXISTED);
        }

        var question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_EXISTED));
        var selectedAnswer = selectedAnswerId == null ? null : answerRepository.findById(selectedAnswerId)
                .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_EXISTED));
        if (selectedAnswer != null && !selectedAnswer.getQuestion().getId().equals(questionId)) {
            throw new AppException(ErrorCode.ANSWER_NOT_BELONG_TO_QUESTION);
        }

        if (selectedAnswer == null) {
            userAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                    .ifPresent(userAnswerRepository::delete);
            return UserAnswerResponse.builder()
                    .attemptId(attemptId)
                    .questionId(questionId)
                    .selectedAnswerId(null)
                    .build();
        }

        TestAttempt currentAttempt = attempt;
        UserAnswer userAnswer = userAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> UserAnswer.builder()
                        .attempt(currentAttempt)
                        .question(question)
                        .questionId(question.getId())
                        .build());

        userAnswer.setSelectedAnswerId(selectedAnswer.getId());
        userAnswer.setCorrect(selectedAnswer != null && selectedAnswer.isCorrect());
        userAnswer.setAnsweredAt(LocalDateTime.now());
        UserAnswer savedAnswer = userAnswerRepository.save(userAnswer);
        return toUserAnswerResponse(savedAnswer, attempt.getMode() == AttemptMode.PRACTICE);
    }

    @Transactional
    public AttemptResponse submitAttempt(String userEmail, Long attemptId) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var attempt = testAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_EXISTED));
        attempt = resolveExpiredAttempt(attempt);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.ATTEMPT_INVALID_STATE);
        }

        return toAttemptResponse(finalizeAttempt(attempt, LocalDateTime.now()));
    }

    @Transactional
    public AttemptResultResponse getAttemptResult(String userEmail, Long attemptId) {
        TestAttempt attempt = resolveExpiredAttempt(getAttemptForUser(userEmail, attemptId));
        if (attempt.getMode() == AttemptMode.MOCK && attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new AppException(ErrorCode.ATTEMPT_INVALID_STATE);
        }

        List<Integer> partNumbers = parsePartNumbers(attempt.getSelectedPartNumbers());
        Map<Long, UserAnswer> answersByQuestionId = userAnswerRepository.findAllByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(UserAnswer::getQuestionId, answer -> answer));

        return AttemptResultResponse.builder()
                .attempt(toAttemptResponse(attempt))
                .parts(resultParts(attempt, partNumbers, answersByQuestionId))
                .build();
    }

    private AttemptResponse toAttemptResponse(TestAttempt attempt) {
        List<Integer> partNumbers = parsePartNumbers(attempt.getSelectedPartNumbers());
        ScoreBreakdown score = scoreAttempt(attempt, partNumbers);
        LocalDateTime expiresAt = expiresAt(attempt);
        Long remainingSeconds = remainingSeconds(attempt, expiresAt);
        boolean submitted = attempt.getStatus() == AttemptStatus.SUBMITTED;

        return AttemptResponse.builder()
                .id(attempt.getId())
                .testId(attempt.getTest().getId())
                .mode(attempt.getMode())
                .status(attempt.getStatus())
                .correctCount(score.correctCount())
                .listeningCorrect(score.listeningCorrect())
                .readingCorrect(score.readingCorrect())
                .answeredCount(score.answeredCount())
                .totalQuestions(score.totalQuestions())
                .totalScore(submitted ? score.totalScore() : null)
                .readingScore(submitted ? score.readingScore() : null)
                .listeningScore(submitted ? score.listeningScore() : null)
                .durationSeconds(attempt.getDurationSeconds())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .expiresAt(expiresAt)
                .remainingSeconds(remainingSeconds)
                .partNumbers(partNumbers)
                .build();
    }

    private AttemptSummaryResponse toAttemptSummary(TestAttempt attempt) {
        AttemptResponse response = toAttemptResponse(attempt);
        return AttemptSummaryResponse.builder()
                .id(response.getId())
                .testId(response.getTestId())
                .testTitle(attempt.getTest().getTitle())
                .mode(response.getMode())
                .status(response.getStatus())
                .correctCount(response.getCorrectCount())
                .listeningCorrect(response.getListeningCorrect())
                .readingCorrect(response.getReadingCorrect())
                .answeredCount(response.getAnsweredCount())
                .totalQuestions(response.getTotalQuestions())
                .totalScore(response.getTotalScore())
                .readingScore(response.getReadingScore())
                .listeningScore(response.getListeningScore())
                .durationSeconds(response.getDurationSeconds())
                .startedAt(response.getStartedAt())
                .submittedAt(response.getSubmittedAt())
                .expiresAt(response.getExpiresAt())
                .remainingSeconds(response.getRemainingSeconds())
                .partNumbers(response.getPartNumbers())
                .build();
    }

    private TestAttempt resolveExpiredAttempt(TestAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt;
        }
        if (attempt.getMode() == AttemptMode.PRACTICE) {
            return attempt;
        }

        LocalDateTime expiresAt = expiresAt(attempt);
        if (expiresAt == null || expiresAt.isAfter(LocalDateTime.now())) {
            return attempt;
        }

        return finalizeAttempt(attempt, expiresAt);
    }

    private TestAttempt finalizeAttempt(TestAttempt attempt, LocalDateTime submittedAt) {
        List<Integer> partNumbers = parsePartNumbers(attempt.getSelectedPartNumbers());
        ScoreBreakdown score = scoreAttempt(attempt, partNumbers);

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(submittedAt);
        attempt.setDurationSeconds(durationSeconds(attempt.getStartedAt(), submittedAt));
        attempt.setListeningScore(score.listeningScore());
        attempt.setReadingScore(score.readingScore());
        attempt.setTotalScore(score.totalScore());

        return testAttemptRepository.save(attempt);
    }

    private ScoreBreakdown scoreAttempt(TestAttempt attempt, List<Integer> partNumbers) {
        List<Question> questions = questionRepository.findAllByTestIdAndPartNumbersOrderByQuestionNumber(
                attempt.getTest().getId(),
                partNumbers
        );
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        List<UserAnswer> answers = questionIds.isEmpty()
                ? List.of()
                : userAnswerRepository.findAllByAttemptIdAndQuestionIdIn(attempt.getId(), questionIds);

        Map<Long, UserAnswer> answersByQuestionId = answers.stream()
                .collect(Collectors.toMap(UserAnswer::getQuestionId, answer -> answer));
        int listeningQuestions = 0;
        int readingQuestions = 0;
        int listeningCorrect = 0;
        int readingCorrect = 0;

        for (Question question : questions) {
            int partNumber = question.getQuestionGroup().getTestPart().getPartNumber();
            boolean listening = partNumber >= 1 && partNumber <= 4;
            if (listening) {
                listeningQuestions++;
            } else {
                readingQuestions++;
            }

            UserAnswer answer = answersByQuestionId.get(question.getId());
            if (answer == null || !answer.isCorrect()) {
                continue;
            }
            if (listening) {
                listeningCorrect++;
            } else {
                readingCorrect++;
            }
        }

        Integer listeningScore = listeningQuestions == 0 ? null : toToeicSectionScore(listeningCorrect, listeningQuestions);
        Integer readingScore = readingQuestions == 0 ? null : toToeicSectionScore(readingCorrect, readingQuestions);
        Integer totalScore = (listeningScore == null ? 0 : listeningScore) + (readingScore == null ? 0 : readingScore);

        return new ScoreBreakdown(
                questions.size(),
                answers.size(),
                listeningCorrect + readingCorrect,
                listeningCorrect,
                readingCorrect,
                listeningScore,
                readingScore,
                totalScore
        );
    }

    private int toToeicSectionScore(int correctCount, int questionCount) {
        if (questionCount <= 0) {
            return 0;
        }
        double normalizedCorrect = Math.min(100.0, Math.max(0.0, correctCount * 100.0 / questionCount));
        int rawScore = (int) Math.round(5 + normalizedCorrect * 4.9);
        int roundedToFive = (int) Math.round(rawScore / 5.0) * 5;
        return Math.min(495, Math.max(5, roundedToFive));
    }

    private LocalDateTime expiresAt(TestAttempt attempt) {
        if (attempt.getMode() == AttemptMode.PRACTICE) {
            return null;
        }
        Integer durationMinutes = attempt.getTest().getDurationMinutes();
        if (attempt.getStartedAt() == null || durationMinutes == null || durationMinutes <= 0) {
            return null;
        }
        return attempt.getStartedAt().plusMinutes(durationMinutes);
    }

    private Long remainingSeconds(TestAttempt attempt, LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return null;
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return 0L;
        }
        return Math.max(0, Duration.between(LocalDateTime.now(), expiresAt).getSeconds());
    }

    private int durationSeconds(LocalDateTime startedAt, LocalDateTime submittedAt) {
        if (startedAt == null || submittedAt == null) {
            return 0;
        }
        long seconds = Math.max(0, Duration.between(startedAt, submittedAt).getSeconds());
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }

    private UserAnswerResponse toUserAnswerResponse(UserAnswer userAnswer, boolean revealAnswer) {
        Question question = userAnswer.getQuestion();
        QuestionGroup questionGroup = question.getQuestionGroup();
        QuestionGroupAudio audio = revealAnswer && isListeningPart(questionGroup)
                ? questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(questionGroup.getId(), 0).orElse(null)
                : null;

        return UserAnswerResponse.builder()
                .attemptId(userAnswer.getAttempt().getId())
                .questionId(userAnswer.getQuestionId())
                .selectedAnswerId(userAnswer.getSelectedAnswerId())
                .correct(revealAnswer ? userAnswer.isCorrect() : null)
                .correctAnswerId(revealAnswer ? correctAnswerId(question) : null)
                .explanationVi(revealAnswer ? question.getExplanationVi() : null)
                .transcriptEn(audio == null ? null : audio.getTranscriptEn())
                .transcriptVi(audio == null ? null : audio.getTranscriptVi())
                .answeredAt(userAnswer.getAnsweredAt())
                .build();
    }

    private boolean isListeningPart(QuestionGroup questionGroup) {
        int partNumber = questionGroup.getTestPart().getPartNumber();
        return partNumber >= 1 && partNumber <= 4;
    }

    private TestAttempt getAttemptForUser(String userEmail, Long attemptId) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return testAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_EXISTED));
    }

    private AttemptMode normalizeMode(AttemptMode mode) {
        if (mode == null) {
            return AttemptMode.MOCK;
        }
        if (mode != AttemptMode.MOCK && mode != AttemptMode.PRACTICE) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return mode;
    }

    private List<Integer> normalizePartNumbers(List<Integer> partNumbers) {
        if (partNumbers == null || partNumbers.isEmpty()) {
            return List.of(1, 2, 3, 4, 5, 6, 7);
        }

        LinkedHashSet<Integer> distinctParts = new LinkedHashSet<>(partNumbers);
        if (distinctParts.isEmpty() || distinctParts.stream().anyMatch(part -> part == null || part < 1 || part > 7)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return distinctParts.stream().sorted().toList();
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

    private String toPartString(List<Integer> partNumbers) {
        return partNumbers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<AttemptContentResponse.Part> contentParts(
            Long testId,
            List<Integer> partNumbers,
            Map<Long, UserAnswer> answersByQuestionId
    ) {
        List<QuestionGroup> groups = questionGroupRepository.findAllByTestIdAndPartNumbersOrderByPartAndOrder(testId, partNumbers);
        Map<Integer, List<QuestionGroup>> groupsByPart = groups.stream()
                .collect(Collectors.groupingBy(
                        group -> group.getTestPart().getPartNumber(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return groupsByPart.entrySet().stream()
                .map(entry -> AttemptContentResponse.Part.builder()
                        .partNumber(entry.getKey())
                        .title(entry.getValue().get(0).getTestPart().getTitle())
                        .groups(entry.getValue().stream()
                                .map(group -> contentGroup(group, answersByQuestionId))
                                .toList())
                        .build())
                .toList();
    }

    private AttemptContentResponse.Group contentGroup(QuestionGroup group, Map<Long, UserAnswer> answersByQuestionId) {
        return AttemptContentResponse.Group.builder()
                .id(group.getId())
                .groupOrder(group.getOrderIndex())
                .images(contentImages(group.getId()))
                .audio(contentAudio(group.getId()))
                .passages(contentPassages(group.getId()))
                .questions(questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(group.getId()).stream()
                        .map(question -> contentQuestion(question, answersByQuestionId.get(question.getId())))
                        .toList())
                .build();
    }

    private AttemptContentResponse.QuestionItem contentQuestion(Question question, UserAnswer userAnswer) {
        List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(question.getId());
        return AttemptContentResponse.QuestionItem.builder()
                .id(question.getId())
                .questionNumber(question.getQuestionNumber())
                .questionTextEn(question.getQuestionTextEn())
                .questionTextVi(question.getQuestionTextVi())
                .selectedAnswerId(userAnswer == null ? null : userAnswer.getSelectedAnswerId())
                .answers(answers.stream()
                        .map(answer -> AttemptContentResponse.AnswerItem.builder()
                                .id(answer.getId())
                                .label(answerLabel(answers, answer))
                                .answerTextEn(answer.getAnswerTextEn())
                                .answerTextVi(answer.getAnswerTextVi())
                                .build())
                        .toList())
                .build();
    }

    private List<AttemptResultResponse.Part> resultParts(
            TestAttempt attempt,
            List<Integer> partNumbers,
            Map<Long, UserAnswer> answersByQuestionId
    ) {
        List<QuestionGroup> groups = questionGroupRepository.findAllByTestIdAndPartNumbersOrderByPartAndOrder(
                attempt.getTest().getId(),
                partNumbers
        );
        Map<Integer, List<QuestionGroup>> groupsByPart = groups.stream()
                .collect(Collectors.groupingBy(
                        group -> group.getTestPart().getPartNumber(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return groupsByPart.entrySet().stream()
                .map(entry -> AttemptResultResponse.Part.builder()
                        .partNumber(entry.getKey())
                        .title(entry.getValue().get(0).getTestPart().getTitle())
                        .groups(entry.getValue().stream()
                                .map(group -> resultGroup(attempt, group, answersByQuestionId))
                                .toList())
                        .build())
                .toList();
    }

    private AttemptResultResponse.Group resultGroup(
            TestAttempt attempt,
            QuestionGroup group,
            Map<Long, UserAnswer> answersByQuestionId
    ) {
        QuestionGroupAudio audio = questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(group.getId(), 0)
                .orElse(null);
        return AttemptResultResponse.Group.builder()
                .id(group.getId())
                .groupOrder(group.getOrderIndex())
                .images(contentImages(group.getId()))
                .audio(contentAudio(group.getId()))
                .passages(contentPassages(group.getId()))
                .transcriptEn(audio == null ? null : audio.getTranscriptEn())
                .transcriptVi(audio == null ? null : audio.getTranscriptVi())
                .questions(questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(group.getId()).stream()
                        .map(question -> resultQuestion(attempt, question, answersByQuestionId.get(question.getId())))
                        .toList())
                .build();
    }

    private AttemptResultResponse.QuestionResult resultQuestion(TestAttempt attempt, Question question, UserAnswer userAnswer) {
        boolean revealAnswer = attempt.getStatus() == AttemptStatus.SUBMITTED
                || (attempt.getMode() == AttemptMode.PRACTICE && userAnswer != null);
        List<Answer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(question.getId());
        return AttemptResultResponse.QuestionResult.builder()
                .id(question.getId())
                .questionNumber(question.getQuestionNumber())
                .questionTextEn(question.getQuestionTextEn())
                .questionTextVi(question.getQuestionTextVi())
                .selectedAnswerId(userAnswer == null ? null : userAnswer.getSelectedAnswerId())
                .correctAnswerId(revealAnswer ? correctAnswerId(question) : null)
                .correct(revealAnswer && userAnswer != null ? userAnswer.isCorrect() : null)
                .explanationVi(revealAnswer ? question.getExplanationVi() : null)
                .answers(answers.stream()
                        .map(answer -> AttemptResultResponse.AnswerResult.builder()
                                .id(answer.getId())
                                .label(answerLabel(answers, answer))
                                .answerTextEn(answer.getAnswerTextEn())
                                .answerTextVi(answer.getAnswerTextVi())
                                .correct(revealAnswer ? answer.isCorrect() : null)
                                .build())
                        .toList())
                .build();
    }

    private List<AttemptContentResponse.Image> contentImages(Long groupId) {
        return questionGroupImageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId).stream()
                .map(image -> AttemptContentResponse.Image.builder()
                        .id(image.getId())
                        .label(image.getMediaAsset().getLabel())
                        .url(image.getMediaAsset().getUrl())
                        .orderIndex(image.getOrderIndex())
                        .build())
                .toList();
    }

    private AttemptContentResponse.Audio contentAudio(Long groupId) {
        return questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(groupId, 0)
                .map(audio -> AttemptContentResponse.Audio.builder()
                        .id(audio.getId())
                        .label(audio.getMediaAsset().getLabel())
                        .url(audio.getMediaAsset().getUrl())
                        .startMs(audio.getStartMs())
                        .endMs(audio.getEndMs())
                        .build())
                .orElse(null);
    }

    private List<AttemptContentResponse.Passage> contentPassages(Long groupId) {
        return questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(groupId).stream()
                .map(passage -> AttemptContentResponse.Passage.builder()
                        .id(passage.getId())
                        .label(passage.getMediaAsset() == null ? null : passage.getMediaAsset().getLabel())
                        .url(passage.getMediaAsset() == null ? null : passage.getMediaAsset().getUrl())
                        .title(passage.getTitle())
                        .passageType(passage.getPassageType())
                        .contentFormat(passage.getContentFormat())
                        .contentEn(passage.getContentEn())
                        .contentVi(passage.getContentVi())
                        .orderIndex(passage.getOrderIndex())
                        .build())
                .toList();
    }

    private Long correctAnswerId(Question question) {
        return answerRepository.findAllByQuestionIdOrderByIdAsc(question.getId()).stream()
                .filter(Answer::isCorrect)
                .map(Answer::getId)
                .findFirst()
                .orElse(null);
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

    private record ScoreBreakdown(
            int totalQuestions,
            int answeredCount,
            int correctCount,
            int listeningCorrect,
            int readingCorrect,
            Integer listeningScore,
            Integer readingScore,
            Integer totalScore
    ) {
    }
}
