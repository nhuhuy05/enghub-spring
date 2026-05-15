package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.enums.AttemptMode;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.test.dto.AttemptResponse;
import com.nhuhuy05.enghub.test.dto.UserAnswerResponse;
import com.nhuhuy05.enghub.test.entity.TestAttempt;
import com.nhuhuy05.enghub.test.entity.UserAnswer;
import com.nhuhuy05.enghub.test.repository.AnswerRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestAttemptRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import com.nhuhuy05.enghub.test.repository.UserAnswerRepository;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestAttemptService {
    UserRepository userRepository;
    TestRepository testRepository;
    QuestionRepository questionRepository;
    AnswerRepository answerRepository;
    UserAnswerRepository userAnswerRepository;
    TestAttemptRepository testAttemptRepository;

    @Transactional
    public AttemptResponse startAttempt(String userEmail, Long testId, AttemptMode mode) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        TestAttempt attempt = TestAttempt.builder()
                .user(user)
                .test(test)
                .mode(mode)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .correctCount(0)
                .totalQuestions(test.getTotalQuestions())
                .build();

        return toAttemptResponse(testAttemptRepository.save(attempt));
    }

    @Transactional
    public UserAnswerResponse saveAnswer(String userEmail, Long attemptId, Long questionId, Long selectedAnswerId) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var attempt = testAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_EXISTED));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.ATTEMPT_INVALID_STATE);
        }

        var question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_EXISTED));
        var selectedAnswer = selectedAnswerId == null ? null : answerRepository.findById(selectedAnswerId)
                .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_EXISTED));
        if (selectedAnswer != null && !selectedAnswer.getQuestion().getId().equals(questionId)) {
            throw new AppException(ErrorCode.ANSWER_NOT_BELONG_TO_QUESTION);
        }

        UserAnswer userAnswer = userAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> UserAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .build());

        userAnswer.setSelectedAnswer(selectedAnswer);
        userAnswer.setCorrect(selectedAnswer != null && selectedAnswer.isCorrect());
        userAnswer.setAnsweredAt(LocalDateTime.now());
        return toUserAnswerResponse(userAnswerRepository.save(userAnswer));
    }

    @Transactional
    public AttemptResponse submitAttempt(String userEmail, Long attemptId) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var attempt = testAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_EXISTED));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.ATTEMPT_INVALID_STATE);
        }

        List<UserAnswer> answers = userAnswerRepository.findAllByAttemptId(attemptId);
        int correctCount = (int) answers.stream().filter(UserAnswer::isCorrect).count();

        attempt.setCorrectCount(correctCount);
        attempt.setTotalQuestions(attempt.getTest().getTotalQuestions());
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());

        return toAttemptResponse(testAttemptRepository.save(attempt));
    }

    private AttemptResponse toAttemptResponse(TestAttempt attempt) {
        return AttemptResponse.builder()
                .id(attempt.getId())
                .testId(attempt.getTest().getId())
                .mode(attempt.getMode())
                .status(attempt.getStatus())
                .correctCount(attempt.getCorrectCount())
                .totalQuestions(attempt.getTotalQuestions())
                .totalScore(attempt.getTotalScore())
                .readingScore(attempt.getReadingScore())
                .listeningScore(attempt.getListeningScore())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    private UserAnswerResponse toUserAnswerResponse(UserAnswer userAnswer) {
        return UserAnswerResponse.builder()
                .attemptId(userAnswer.getAttempt().getId())
                .questionId(userAnswer.getQuestion().getId())
                .selectedAnswerId(userAnswer.getSelectedAnswer() == null ? null : userAnswer.getSelectedAnswer().getId())
                .correct(userAnswer.isCorrect())
                .answeredAt(userAnswer.getAnsweredAt())
                .build();
    }
}
