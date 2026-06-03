package com.nhuhuy05.enghub.test.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.entity.UserAnswer;
import com.nhuhuy05.enghub.test.repository.AnswerRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestAttemptRepository;
import com.nhuhuy05.enghub.test.repository.UserAnswerRepository;
import com.nhuhuy05.enghub.user.entity.User;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionChatServiceTest {
    private static final String USER_EMAIL = "student@example.com";

    @Mock UserRepository userRepository;
    @Mock TestAttemptRepository testAttemptRepository;
    @Mock QuestionRepository questionRepository;
    @Mock AnswerRepository answerRepository;
    @Mock UserAnswerRepository userAnswerRepository;
    @Mock QuestionGroupAudioRepository questionGroupAudioRepository;
    @Mock QuestionGroupPassageRepository questionGroupPassageRepository;
    @Mock GeminiClientService geminiClientService;

    QuestionChatService questionChatService;
    User user;
    com.nhuhuy05.enghub.test.entity.Test test;
    TestAttempt practiceAttempt;
    Question question;
    Answer answerA;
    Answer answerB;

    @BeforeEach
    void setUp() {
        questionChatService = new QuestionChatService(
                userRepository,
                testAttemptRepository,
                questionRepository,
                answerRepository,
                userAnswerRepository,
                questionGroupAudioRepository,
                questionGroupPassageRepository,
                geminiClientService,
                new ObjectMapper()
        );
        user = User.builder()
                .id(10L)
                .email(USER_EMAIL)
                .build();
        test = com.nhuhuy05.enghub.test.entity.Test.builder()
                .id(20L)
                .title("ETS Practice")
                .published(true)
                .durationMinutes(120)
                .totalQuestions(200)
                .build();
        practiceAttempt = attempt(30L, AttemptMode.PRACTICE, AttemptStatus.IN_PROGRESS, "5");
        question = question(41L, 5);
        question.setQuestionTextEn("Where should the sentence be inserted?");
        question.setQuestionTextVi("Cau nay nen duoc chen vao dau?");
        question.setExplanationVi("Vi cau nay noi ve lich hop.");
        answerA = answer(101L, question, true);
        answerB = answer(102L, question, false);
    }

    @Test
    void buildChatInputRejectsNonPracticeAttempt() {
        TestAttempt mockAttempt = attempt(30L, AttemptMode.MOCK, AttemptStatus.IN_PROGRESS, "5");
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(mockAttempt));

        assertThatThrownBy(() -> questionChatService.buildChatInput(USER_EMAIL, 30L, 41L, request()))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ATTEMPT_INVALID_STATE);
    }

    @Test
    void buildChatInputRejectsQuestionOutsideSelectedParts() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(practiceAttempt));
        when(questionRepository.existsByIdAndTestIdAndPartNumbers(41L, 20L, List.of(5))).thenReturn(false);

        assertThatThrownBy(() -> questionChatService.buildChatInput(USER_EMAIL, 30L, 41L, request()))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUESTION_NOT_EXISTED);
    }

    @Test
    void unansweredQuestionContextDoesNotExposeCorrectAnswer() {
        givenValidQuestionContext();
        when(userAnswerRepository.findByAttemptIdAndQuestionId(30L, 41L)).thenReturn(Optional.empty());

        QuestionChatService.ChatInput input = questionChatService.buildChatInput(USER_EMAIL, 30L, 41L, request());
        JsonNode context = input.context();

        assertThat(context.path("answered").asBoolean()).isFalse();
        assertThat(context.has("correct_answer_id")).isFalse();
        assertThat(context.path("answers").get(0).has("is_correct")).isFalse();
    }

    @Test
    void answeredQuestionContextIncludesSelectedAndCorrectAnswer() {
        UserAnswer userAnswer = UserAnswer.builder()
                .attempt(practiceAttempt)
                .question(question)
                .questionId(question.getId())
                .selectedAnswerId(answerB.getId())
                .isCorrect(false)
                .answeredAt(LocalDateTime.now())
                .build();
        givenValidQuestionContext();
        when(userAnswerRepository.findByAttemptIdAndQuestionId(30L, 41L)).thenReturn(Optional.of(userAnswer));

        QuestionChatService.ChatInput input = questionChatService.buildChatInput(USER_EMAIL, 30L, 41L, request());
        JsonNode context = input.context();

        assertThat(context.path("answered").asBoolean()).isTrue();
        assertThat(context.path("selected_answer_label").asText()).isEqualTo("B");
        assertThat(context.path("correct_answer_label").asText()).isEqualTo("A");
        assertThat(context.path("explanation_vi").asText()).isEqualTo("Vi cau nay noi ve lich hop.");
        assertThat(context.path("answers").get(0).path("is_correct").asBoolean()).isTrue();
    }

    private void givenValidQuestionContext() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(practiceAttempt));
        when(questionRepository.existsByIdAndTestIdAndPartNumbers(41L, 20L, List.of(5))).thenReturn(true);
        when(questionRepository.findById(41L)).thenReturn(Optional.of(question));
        when(answerRepository.findAllByQuestionIdOrderByIdAsc(41L)).thenReturn(List.of(answerA, answerB));
    }

    private QuestionChatRequest request() {
        return QuestionChatRequest.builder()
                .message("Tai sao cau nay dung?")
                .build();
    }

    private TestAttempt attempt(Long id, AttemptMode mode, AttemptStatus status, String selectedPartNumbers) {
        return TestAttempt.builder()
                .id(id)
                .user(user)
                .test(test)
                .mode(mode)
                .status(status)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .selectedPartNumbers(selectedPartNumbers)
                .build();
    }

    private Question question(Long id, int partNumber) {
        TestPart testPart = TestPart.builder()
                .id((long) partNumber)
                .test(test)
                .partNumber(partNumber)
                .title("Part " + partNumber)
                .build();
        QuestionGroup group = QuestionGroup.builder()
                .id(1000L + id)
                .testPart(testPart)
                .orderIndex(1)
                .build();
        return Question.builder()
                .id(id)
                .questionGroup(group)
                .questionNumber(id.intValue())
                .build();
    }

    private Answer answer(Long id, Question question, boolean correct) {
        return Answer.builder()
                .id(id)
                .question(question)
                .answerTextEn("Answer " + id)
                .answerTextVi("Dap an " + id)
                .isCorrect(correct)
                .build();
    }
}
