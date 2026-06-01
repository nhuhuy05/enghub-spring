package com.nhuhuy05.enghub.listening.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupTranscriptLine;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupTranscriptLineRepository;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestPartRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListeningDictationServiceTest {
    @Mock TestRepository testRepository;
    @Mock TestPartRepository testPartRepository;
    @Mock QuestionGroupRepository questionGroupRepository;
    @Mock QuestionRepository questionRepository;
    @Mock QuestionGroupAudioRepository questionGroupAudioRepository;
    @Mock QuestionGroupTranscriptLineRepository questionGroupTranscriptLineRepository;
    @Spy ListeningTranscriptSplitter transcriptSplitter = new ListeningTranscriptSplitter();

    @InjectMocks ListeningDictationService service;

    @Test
    void getDictationSessionRejectsNonListeningPart() {
        com.nhuhuy05.enghub.test.entity.Test test = testEntity();
        when(testRepository.findByIdAndPublishedTrue(1L)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> service.getDictationSession(1L, 5))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_KEY);
    }

    @Test
    void getDictationSessionUsesTranscriptLinesFirst() {
        com.nhuhuy05.enghub.test.entity.Test test = testEntity();
        TestPart part = part(test, 1);
        QuestionGroup group = group(part, 1);
        QuestionGroupAudio audio = audio(test, group);
        QuestionGroupTranscriptLine line = QuestionGroupTranscriptLine.builder()
                .id(200L)
                .questionGroupAudio(audio)
                .speaker("W")
                .textEn("The woman is carrying a tray of food.")
                .textVi("Người phụ nữ đang bưng một khay thức ăn.")
                .startMs(1000)
                .endMs(3500)
                .orderIndex(0)
                .build();

        when(testRepository.findByIdAndPublishedTrue(1L)).thenReturn(Optional.of(test));
        when(testPartRepository.findByTestIdAndPartNumber(1L, 1)).thenReturn(Optional.of(part));
        when(questionGroupRepository.findAllByTestIdAndPartNumbersOrderByPartAndOrder(1L, List.of(1)))
                .thenReturn(List.of(group));
        when(questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(10L))
                .thenReturn(List.of(question(group, 1)));
        when(questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(10L, 0))
                .thenReturn(Optional.of(audio));
        when(questionGroupTranscriptLineRepository.findAllByQuestionGroupAudioIdOrderByOrderIndexAsc(20L))
                .thenReturn(List.of(line));

        var response = service.getDictationSession(1L, 1);

        assertThat(response.getGroups()).hasSize(1);
        assertThat(response.getGroups().get(0).getSentences()).hasSize(1);
        var sentence = response.getGroups().get(0).getSentences().get(0);
        assertThat(sentence.getText()).isEqualTo("The woman is carrying a tray of food.");
        assertThat(sentence.getStartMs()).isEqualTo(1000);
        assertThat(sentence.getEndMs()).isEqualTo(3500);
        assertThat(sentence.getHintLevels()).containsExactly(30, 50, 100);
    }

    @Test
    void getDictationSessionFallsBackToGroupTranscriptWhenLinesAreMissing() {
        com.nhuhuy05.enghub.test.entity.Test test = testEntity();
        TestPart part = part(test, 3);
        QuestionGroup group = group(part, 2);
        QuestionGroupAudio audio = audio(test, group);
        audio.setTranscriptEn("W: Good morning. M: The meeting starts at nine.");
        audio.setTranscriptVi("Chào buổi sáng. Cuộc họp bắt đầu lúc chín giờ.");

        when(testRepository.findByIdAndPublishedTrue(1L)).thenReturn(Optional.of(test));
        when(testPartRepository.findByTestIdAndPartNumber(1L, 3)).thenReturn(Optional.of(part));
        when(questionGroupRepository.findAllByTestIdAndPartNumbersOrderByPartAndOrder(1L, List.of(3)))
                .thenReturn(List.of(group));
        when(questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(10L))
                .thenReturn(List.of(question(group, 32), question(group, 33), question(group, 34)));
        when(questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(10L, 0))
                .thenReturn(Optional.of(audio));
        when(questionGroupTranscriptLineRepository.findAllByQuestionGroupAudioIdOrderByOrderIndexAsc(20L))
                .thenReturn(List.of());

        var response = service.getDictationSession(1L, 3);

        assertThat(response.getGroups()).hasSize(1);
        assertThat(response.getGroups().get(0).getTitle()).isEqualTo("Bài 32-34");
        assertThat(response.getGroups().get(0).getSentences()).hasSize(2);
        assertThat(response.getGroups().get(0).getSentences().get(0).getSpeaker()).isEqualTo("W");
        assertThat(response.getGroups().get(0).getSentences().get(1).getText()).isEqualTo("The meeting starts at nine.");
    }

    private com.nhuhuy05.enghub.test.entity.Test testEntity() {
        return com.nhuhuy05.enghub.test.entity.Test.builder()
                .id(1L)
                .title("TEST 1 2026")
                .published(true)
                .durationMinutes(120)
                .totalQuestions(200)
                .build();
    }

    private TestPart part(com.nhuhuy05.enghub.test.entity.Test test, int partNumber) {
        return TestPart.builder()
                .id((long) partNumber)
                .test(test)
                .partNumber(partNumber)
                .title("Part " + partNumber)
                .build();
    }

    private QuestionGroup group(TestPart part, int orderIndex) {
        return QuestionGroup.builder()
                .id(10L)
                .testPart(part)
                .orderIndex(orderIndex)
                .build();
    }

    private QuestionGroupAudio audio(com.nhuhuy05.enghub.test.entity.Test test, QuestionGroup group) {
        return QuestionGroupAudio.builder()
                .id(20L)
                .questionGroup(group)
                .mediaAsset(MediaAsset.builder()
                        .id(30L)
                        .test(test)
                        .url("https://example.com/audio.mp3")
                        .build())
                .startMs(0)
                .endMs(5000)
                .orderIndex(0)
                .build();
    }

    private Question question(QuestionGroup group, int questionNumber) {
        return Question.builder()
                .id((long) questionNumber)
                .questionGroup(group)
                .questionNumber(questionNumber)
                .build();
    }
}
