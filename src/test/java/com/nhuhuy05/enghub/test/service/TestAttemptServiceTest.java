package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.enums.AttemptMode;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.dto.AttemptResponse;
import com.nhuhuy05.enghub.test.dto.UserAnswerResponse;
import com.nhuhuy05.enghub.test.entity.Answer;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.TestAttempt;
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.entity.UserAnswer;
import com.nhuhuy05.enghub.test.repository.AnswerRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupImageRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestAttemptRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import com.nhuhuy05.enghub.test.repository.UserAnswerRepository;
import com.nhuhuy05.enghub.user.entity.User;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TestAttemptServiceTest {
    private static final String USER_EMAIL = "student@example.com";

    @Mock UserRepository userRepository;
    @Mock TestRepository testRepository;
    @Mock QuestionRepository questionRepository;
    @Mock QuestionGroupRepository questionGroupRepository;
    @Mock QuestionGroupImageRepository questionGroupImageRepository;
    @Mock QuestionGroupAudioRepository questionGroupAudioRepository;
    @Mock QuestionGroupPassageRepository questionGroupPassageRepository;
    @Mock AnswerRepository answerRepository;
    @Mock UserAnswerRepository userAnswerRepository;
    @Mock TestAttemptRepository testAttemptRepository;

    @InjectMocks TestAttemptService testAttemptService;

    User user;
    com.nhuhuy05.enghub.test.entity.Test publishedTest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(10L)
                .email(USER_EMAIL)
                .build();
        publishedTest = testEntity(20L, true, 120);

        lenient().when(testAttemptRepository.save(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void startAttemptRejectsUnpublishedTest() {
        com.nhuhuy05.enghub.test.entity.Test draftTest = testEntity(20L, false, 120);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testRepository.findById(20L)).thenReturn(Optional.of(draftTest));

        assertThatThrownBy(() -> testAttemptService.startAttempt(USER_EMAIL, 20L, AttemptMode.MOCK, null))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.TEST_NOT_EXISTED);
    }

    @Test
    void startAttemptAlwaysCreatesNewAttempt() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testRepository.findById(20L)).thenReturn(Optional.of(publishedTest));
        when(questionRepository.findAllByTestIdAndPartNumbersOrderByQuestionNumber(eq(20L), anyList()))
                .thenReturn(List.of());

        AttemptResponse response = testAttemptService.startAttempt(USER_EMAIL, 20L, AttemptMode.PRACTICE, List.of(3, 1, 3));

        assertThat(response.getPartNumbers()).containsExactly(1, 3);
        assertThat(response.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        verify(testAttemptRepository).save(any(TestAttempt.class));
        verify(testAttemptRepository, never())
                .findFirstByUserIdAndTestIdAndModeAndStatusAndSelectedPartNumbersOrderByStartedAtDesc(
                        any(), any(), any(), any(), any()
                );
    }

    @Test
    void saveAnswerRejectsQuestionOutsideSelectedParts() {
        TestAttempt attempt = attempt(30L, AttemptMode.MOCK, AttemptStatus.IN_PROGRESS, "1");
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(attempt));
        when(questionRepository.existsByIdAndTestIdAndPartNumbers(99L, 20L, List.of(1))).thenReturn(false);

        assertThatThrownBy(() -> testAttemptService.saveAnswer(USER_EMAIL, 30L, 99L, 100L))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUESTION_NOT_EXISTED);
    }

    @Test
    void practiceSaveAnswerRevealsCorrectAnswerAndExplanation() {
        TestAttempt attempt = attempt(30L, AttemptMode.PRACTICE, AttemptStatus.IN_PROGRESS, "5");
        Question question = question(41L, 5);
        question.setExplanationVi("Giai thich");
        Answer answerA = answer(101L, question, true);
        Answer answerB = answer(102L, question, false);
        UserAnswer savedAnswer = UserAnswer.builder()
                .attempt(attempt)
                .question(question)
                .questionId(question.getId())
                .selectedAnswerId(answerA.getId())
                .isCorrect(true)
                .answeredAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(attempt));
        when(questionRepository.existsByIdAndTestIdAndPartNumbers(41L, 20L, List.of(5))).thenReturn(true);
        when(questionRepository.findById(41L)).thenReturn(Optional.of(question));
        when(answerRepository.findById(101L)).thenReturn(Optional.of(answerA));
        when(userAnswerRepository.findByAttemptIdAndQuestionId(30L, 41L)).thenReturn(Optional.empty());
        when(userAnswerRepository.save(any(UserAnswer.class))).thenReturn(savedAnswer);
        when(answerRepository.findAllByQuestionIdOrderByIdAsc(41L)).thenReturn(List.of(answerA, answerB));

        UserAnswerResponse response = testAttemptService.saveAnswer(USER_EMAIL, 30L, 41L, 101L);

        assertThat(response.getCorrect()).isTrue();
        assertThat(response.getCorrectAnswerId()).isEqualTo(101L);
        assertThat(response.getExplanationVi()).isEqualTo("Giai thich");
        assertThat(response.getTranscriptEn()).isNull();
        assertThat(response.getTranscriptVi()).isNull();
    }

    @Test
    void practiceSaveAnswerForListeningPartRevealsTranscript() {
        TestAttempt attempt = attempt(30L, AttemptMode.PRACTICE, AttemptStatus.IN_PROGRESS, "1");
        Question question = question(41L, 1);
        Answer answerA = answer(101L, question, true);
        UserAnswer savedAnswer = UserAnswer.builder()
                .attempt(attempt)
                .question(question)
                .questionId(question.getId())
                .selectedAnswerId(answerA.getId())
                .isCorrect(true)
                .answeredAt(LocalDateTime.now())
                .build();
        QuestionGroupAudio audio = QuestionGroupAudio.builder()
                .questionGroup(question.getQuestionGroup())
                .transcriptEn("A. The man is reading.")
                .transcriptVi("A. Nguoi dan ong dang doc.")
                .orderIndex(0)
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(attempt));
        when(questionRepository.existsByIdAndTestIdAndPartNumbers(41L, 20L, List.of(1))).thenReturn(true);
        when(questionRepository.findById(41L)).thenReturn(Optional.of(question));
        when(answerRepository.findById(101L)).thenReturn(Optional.of(answerA));
        when(userAnswerRepository.findByAttemptIdAndQuestionId(30L, 41L)).thenReturn(Optional.empty());
        when(userAnswerRepository.save(any(UserAnswer.class))).thenReturn(savedAnswer);
        when(answerRepository.findAllByQuestionIdOrderByIdAsc(41L)).thenReturn(List.of(answerA));
        when(questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(question.getQuestionGroup().getId(), 0))
                .thenReturn(Optional.of(audio));

        UserAnswerResponse response = testAttemptService.saveAnswer(USER_EMAIL, 30L, 41L, 101L);

        assertThat(response.getCorrect()).isTrue();
        assertThat(response.getCorrectAnswerId()).isEqualTo(101L);
        assertThat(response.getTranscriptEn()).isEqualTo("A. The man is reading.");
        assertThat(response.getTranscriptVi()).isEqualTo("A. Nguoi dan ong dang doc.");
    }

    @Test
    void submitAttemptCalculatesToeicScaledScoresAndCorrectCounts() {
        TestAttempt attempt = attempt(30L, AttemptMode.MOCK, AttemptStatus.IN_PROGRESS, "1,5");
        Question listeningQuestion = question(41L, 1);
        Question readingQuestion = question(42L, 5);
        UserAnswer listeningCorrect = userAnswer(attempt, listeningQuestion, true);
        UserAnswer readingWrong = userAnswer(attempt, readingQuestion, false);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByTestIdAndPartNumbersOrderByQuestionNumber(20L, List.of(1, 5)))
                .thenReturn(List.of(listeningQuestion, readingQuestion));
        when(userAnswerRepository.findAllByAttemptIdAndQuestionIdIn(30L, List.of(41L, 42L)))
                .thenReturn(List.of(listeningCorrect, readingWrong));

        AttemptResponse response = testAttemptService.submitAttempt(USER_EMAIL, 30L);

        assertThat(response.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(response.getCorrectCount()).isEqualTo(1);
        assertThat(response.getListeningCorrect()).isEqualTo(1);
        assertThat(response.getReadingCorrect()).isZero();
        assertThat(response.getListeningScore()).isEqualTo(495);
        assertThat(response.getReadingScore()).isEqualTo(5);
        assertThat(response.getTotalScore()).isEqualTo(500);
        verify(testAttemptRepository).save(attempt);
    }

    @Test
    void mockResultBeforeSubmitIsRejected() {
        TestAttempt attempt = attempt(30L, AttemptMode.MOCK, AttemptStatus.IN_PROGRESS, "1");
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> testAttemptService.getAttemptResult(USER_EMAIL, 30L))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ATTEMPT_INVALID_STATE);
    }

    @Test
    void getAttemptAutoSubmitsExpiredAttempt() {
        TestAttempt attempt = attempt(30L, AttemptMode.MOCK, AttemptStatus.IN_PROGRESS, "1");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(3));
        attempt.getTest().setDurationMinutes(1);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByTestIdAndPartNumbersOrderByQuestionNumber(20L, List.of(1))).thenReturn(List.of());

        AttemptResponse response = testAttemptService.getAttempt(USER_EMAIL, 30L);

        assertThat(response.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(response.getRemainingSeconds()).isZero();
        assertThat(response.getDurationSeconds()).isEqualTo(60);
        verify(testAttemptRepository).save(attempt);
    }

    @Test
    void getAttemptDoesNotAutoSubmitExpiredPracticeAttempt() {
        TestAttempt attempt = attempt(30L, AttemptMode.PRACTICE, AttemptStatus.IN_PROGRESS, "1");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(3));
        attempt.getTest().setDurationMinutes(1);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(testAttemptRepository.findByIdAndUserId(30L, user.getId())).thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByTestIdAndPartNumbersOrderByQuestionNumber(20L, List.of(1))).thenReturn(List.of());

        AttemptResponse response = testAttemptService.getAttempt(USER_EMAIL, 30L);

        assertThat(response.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(response.getExpiresAt()).isNull();
        assertThat(response.getRemainingSeconds()).isNull();
        verify(testAttemptRepository, never()).save(attempt);
    }

    private TestAttempt attempt(Long id, AttemptMode mode, AttemptStatus status, String selectedPartNumbers) {
        return TestAttempt.builder()
                .id(id)
                .user(user)
                .test(publishedTest)
                .mode(mode)
                .status(status)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .selectedPartNumbers(selectedPartNumbers)
                .build();
    }

    private com.nhuhuy05.enghub.test.entity.Test testEntity(Long id, boolean published, int durationMinutes) {
        return com.nhuhuy05.enghub.test.entity.Test.builder()
                .id(id)
                .title("ETS Test")
                .published(published)
                .durationMinutes(durationMinutes)
                .totalQuestions(200)
                .build();
    }

    private Question question(Long id, int partNumber) {
        TestPart testPart = TestPart.builder()
                .id((long) partNumber)
                .test(publishedTest)
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
                .questionTextEn("Question " + id)
                .build();
    }

    private Answer answer(Long id, Question question, boolean correct) {
        return Answer.builder()
                .id(id)
                .question(question)
                .answerTextEn("Answer " + id)
                .isCorrect(correct)
                .build();
    }

    private UserAnswer userAnswer(TestAttempt attempt, Question question, boolean correct) {
        return UserAnswer.builder()
                .attempt(attempt)
                .question(question)
                .questionId(question.getId())
                .selectedAnswerId(correct ? 1L : 2L)
                .isCorrect(correct)
                .answeredAt(LocalDateTime.now())
                .build();
    }
}
