package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupTranscriptLine;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupTranscriptLineRepository;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.media.repository.MediaAssetRepository;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.dto.QuestionGroupTranscriptLinesUpdateRequest;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.repository.*;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionGroupReviewServiceTest {
    @Mock TestRepository testRepository;
    @Mock TestPartRepository testPartRepository;
    @Mock QuestionGroupRepository questionGroupRepository;
    @Mock QuestionGroupImageRepository questionGroupImageRepository;
    @Mock QuestionGroupAudioRepository questionGroupAudioRepository;
    @Mock QuestionGroupTranscriptLineRepository questionGroupTranscriptLineRepository;
    @Mock QuestionGroupPassageRepository questionGroupPassageRepository;
    @Mock QuestionRepository questionRepository;
    @Mock AnswerRepository answerRepository;
    @Mock MediaAssetRepository mediaAssetRepository;
    @Mock UserRepository userRepository;

    @InjectMocks QuestionGroupReviewService service;

    @Test
    void updateTranscriptLinesReplacesLinesAndMarksGroupNeedsReview() {
        QuestionGroup group = group(1);
        QuestionGroupAudio audio = audio(group);
        var request = QuestionGroupTranscriptLinesUpdateRequest.builder()
                .lines(List.of(QuestionGroupTranscriptLinesUpdateRequest.Line.builder()
                        .speaker("W")
                        .textEn("The woman is carrying a tray of food.")
                        .textVi("Người phụ nữ đang bưng một khay thức ăn.")
                        .startMs(1000)
                        .endMs(3500)
                        .orderIndex(0)
                        .build()))
                .build();
        QuestionGroupTranscriptLine savedLine = QuestionGroupTranscriptLine.builder()
                .id(300L)
                .questionGroupAudio(audio)
                .speaker("W")
                .textEn("The woman is carrying a tray of food.")
                .textVi("Người phụ nữ đang bưng một khay thức ăn.")
                .startMs(1000)
                .endMs(3500)
                .orderIndex(0)
                .build();

        when(questionGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(10L, 0)).thenReturn(Optional.of(audio));
        when(questionGroupTranscriptLineRepository.save(any(QuestionGroupTranscriptLine.class))).thenReturn(savedLine);
        when(questionGroupImageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(10L)).thenReturn(List.of());
        when(questionGroupPassageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(10L)).thenReturn(List.of());
        when(questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(10L)).thenReturn(List.of());
        when(questionGroupTranscriptLineRepository.findAllByQuestionGroupAudioIdOrderByOrderIndexAsc(20L))
                .thenReturn(List.of(savedLine));

        var response = service.updateTranscriptLines(10L, request);

        verify(questionGroupTranscriptLineRepository).deleteAllByQuestionGroupAudioId(20L);
        verify(questionGroupTranscriptLineRepository).flush();
        verify(questionGroupRepository).save(group);
        assertThat(group.getReviewStatus()).isEqualTo("needs_review");
        assertThat(response.getAudio().getTranscriptLines()).hasSize(1);
        assertThat(response.getAudio().getTranscriptLines().get(0).getTextEn())
                .isEqualTo("The woman is carrying a tray of food.");
    }

    @Test
    void updateTranscriptLinesRejectsDuplicatedOrderIndex() {
        QuestionGroup group = group(1);
        QuestionGroupAudio audio = audio(group);
        var request = QuestionGroupTranscriptLinesUpdateRequest.builder()
                .lines(List.of(
                        line("First sentence.", 0),
                        line("Second sentence.", 0)
                ))
                .build();

        when(questionGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(10L, 0)).thenReturn(Optional.of(audio));

        assertThatThrownBy(() -> service.updateTranscriptLines(10L, request))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_KEY);
    }

    private QuestionGroupTranscriptLinesUpdateRequest.Line line(String text, int orderIndex) {
        return QuestionGroupTranscriptLinesUpdateRequest.Line.builder()
                .textEn(text)
                .orderIndex(orderIndex)
                .build();
    }

    private QuestionGroup group(int partNumber) {
        TestPart part = TestPart.builder()
                .id(1L)
                .partNumber(partNumber)
                .title("Part " + partNumber)
                .build();
        return QuestionGroup.builder()
                .id(10L)
                .testPart(part)
                .orderIndex(1)
                .reviewStatus("reviewed")
                .build();
    }

    private QuestionGroupAudio audio(QuestionGroup group) {
        return QuestionGroupAudio.builder()
                .id(20L)
                .questionGroup(group)
                .mediaAsset(MediaAsset.builder()
                        .id(30L)
                        .label("Audio 1")
                        .url("https://example.com/audio.mp3")
                        .build())
                .startMs(0)
                .endMs(5000)
                .orderIndex(0)
                .build();
    }
}
