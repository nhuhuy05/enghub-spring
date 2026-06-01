package com.nhuhuy05.enghub.listening.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.dto.ListeningDictationSessionResponse;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupTranscriptLine;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupTranscriptLineRepository;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.Test;
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestPartRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListeningDictationService {
    private static final List<Integer> HINT_LEVELS = List.of(30, 50, 100);

    TestRepository testRepository;
    TestPartRepository testPartRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionRepository questionRepository;
    QuestionGroupAudioRepository questionGroupAudioRepository;
    QuestionGroupTranscriptLineRepository questionGroupTranscriptLineRepository;
    ListeningTranscriptSplitter transcriptSplitter;

    @Transactional(readOnly = true)
    public ListeningDictationSessionResponse getDictationSession(Long testId, Integer partNumber) {
        Test test = testRepository.findByIdAndPublishedTrue(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));
        validateListeningPart(partNumber);
        TestPart testPart = testPartRepository.findByTestIdAndPartNumber(testId, partNumber)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        List<ListeningDictationSessionResponse.Group> groups = questionGroupRepository
                .findAllByTestIdAndPartNumbersOrderByPartAndOrder(testId, List.of(partNumber)).stream()
                .map(this::toGroup)
                .filter(group -> !group.getSentences().isEmpty())
                .toList();

        return ListeningDictationSessionResponse.builder()
                .testId(test.getId())
                .partId("part-" + partNumber)
                .partNumber(partNumber)
                .title(test.getTitle())
                .partName("Part " + partNumber)
                .instruction(testPart.getTitle() + ": listen to each sentence and practice dictation.")
                .groups(groups)
                .build();
    }

    private ListeningDictationSessionResponse.Group toGroup(QuestionGroup group) {
        List<Integer> questionNumbers = questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(group.getId()).stream()
                .map(Question::getQuestionNumber)
                .toList();
        QuestionGroupAudio audio = questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(group.getId(), 0)
                .orElse(null);

        return ListeningDictationSessionResponse.Group.builder()
                .id(group.getId())
                .title(groupTitle(group, questionNumbers))
                .groupOrder(group.getOrderIndex())
                .questionNumbers(questionNumbers)
                .sentences(audio == null ? List.of() : toSentences(group, audio))
                .build();
    }

    private List<ListeningDictationSessionResponse.Sentence> toSentences(QuestionGroup group, QuestionGroupAudio audio) {
        List<QuestionGroupTranscriptLine> lines = questionGroupTranscriptLineRepository
                .findAllByQuestionGroupAudioIdOrderByOrderIndexAsc(audio.getId());
        if (!lines.isEmpty()) {
            return lines.stream()
                    .map(line -> toSentence(group, audio, line))
                    .toList();
        }

        List<ListeningTranscriptSplitter.TranscriptSentence> fallbackSentences = transcriptSplitter.split(
                audio.getTranscriptEn(),
                audio.getTranscriptVi()
        );
        return java.util.stream.IntStream.range(0, fallbackSentences.size())
                .mapToObj(index -> toFallbackSentence(group, audio, fallbackSentences.get(index), index))
                .toList();
    }

    private ListeningDictationSessionResponse.Sentence toSentence(
            QuestionGroup group,
            QuestionGroupAudio audio,
            QuestionGroupTranscriptLine line
    ) {
        return ListeningDictationSessionResponse.Sentence.builder()
                .id(group.getId() + "-" + line.getOrderIndex())
                .speaker(line.getSpeaker())
                .text(line.getTextEn())
                .translation(line.getTextVi())
                .audioUrl(audio.getMediaAsset().getUrl())
                .startMs(line.getStartMs() == null ? audio.getStartMs() : line.getStartMs())
                .endMs(line.getEndMs() == null ? audio.getEndMs() : line.getEndMs())
                .orderIndex(line.getOrderIndex())
                .completed(false)
                .hintLevels(HINT_LEVELS)
                .build();
    }

    private ListeningDictationSessionResponse.Sentence toFallbackSentence(
            QuestionGroup group,
            QuestionGroupAudio audio,
            ListeningTranscriptSplitter.TranscriptSentence sentence,
            int orderIndex
    ) {
        return ListeningDictationSessionResponse.Sentence.builder()
                .id(group.getId() + "-fallback-" + orderIndex)
                .speaker(sentence.speaker())
                .text(sentence.text())
                .translation(sentence.translation())
                .audioUrl(audio.getMediaAsset().getUrl())
                .startMs(audio.getStartMs())
                .endMs(audio.getEndMs())
                .orderIndex(orderIndex)
                .completed(false)
                .hintLevels(HINT_LEVELS)
                .build();
    }

    private String groupTitle(QuestionGroup group, List<Integer> questionNumbers) {
        if (group.getTitle() != null && !group.getTitle().isBlank()) {
            return group.getTitle();
        }
        int part = group.getTestPart().getPartNumber();
        if (part <= 2 || questionNumbers.isEmpty()) {
            return "Câu " + group.getOrderIndex();
        }
        int first = questionNumbers.get(0);
        int last = questionNumbers.get(questionNumbers.size() - 1);
        return first == last ? "Bài " + first : "Bài " + first + "-" + last;
    }

    private void validateListeningPart(Integer partNumber) {
        if (partNumber == null || partNumber < 1 || partNumber > 4) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}
