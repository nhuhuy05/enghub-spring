package com.nhuhuy05.enghub.vocabulary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhuhuy05.enghub.ai.service.GeminiClientService;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.vocabulary.dto.*;
import com.nhuhuy05.enghub.vocabulary.entity.Vocabulary;
import com.nhuhuy05.enghub.vocabulary.repository.VocabularyRepository;
import com.nhuhuy05.enghub.vocabulary.repository.VocabularyTopicRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VocabularyEnrichmentService {
    VocabularyRepository vocabularyRepository;
    VocabularyTopicRepository vocabularyTopicRepository;
    VocabularyService vocabularyService;
    GeminiClientService geminiClientService;
    ObjectMapper objectMapper;

    @Transactional
    public VocabularyEnrichResponse enrichVocabulary(Long vocabularyId, VocabularyEnrichRequest request) {
        Vocabulary vocabulary = getVocabulary(vocabularyId);
        EnrichOptions options = options(request);
        List<VocabularyEnrichErrorResponse> errors = new ArrayList<>();

        boolean updated = enrichOne(vocabulary, options, errors);
        VocabularyResponse word = vocabularyService.getVocabulary(vocabularyId, null);
        return VocabularyEnrichResponse.builder()
                .vocabularyId(vocabularyId)
                .totalWords(1)
                .updatedCount(updated ? 1 : 0)
                .skippedCount(updated ? 0 : 1)
                .errors(errors)
                .words(List.of(word))
                .build();
    }

    @Transactional
    public VocabularyEnrichResponse enrichTopic(Long topicId, VocabularyEnrichRequest request) {
        vocabularyTopicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_TOPIC_NOT_EXISTED));

        EnrichOptions options = options(request);
        List<Vocabulary> vocabularies = vocabularyRepository.search(topicId, "");
        List<VocabularyEnrichErrorResponse> errors = new ArrayList<>();
        int updatedCount = 0;
        int skippedCount = 0;

        for (Vocabulary vocabulary : vocabularies) {
            boolean updated = enrichOne(vocabulary, options, errors);
            if (updated) {
                updatedCount++;
            } else {
                skippedCount++;
            }
        }

        List<VocabularyResponse> words = vocabularyRepository.search(topicId, "").stream()
                .map(vocabulary -> vocabularyService.getVocabulary(vocabulary.getId(), null))
                .toList();

        return VocabularyEnrichResponse.builder()
                .topicId(topicId)
                .totalWords(vocabularies.size())
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .errors(errors)
                .words(words)
                .build();
    }

    private boolean enrichOne(
            Vocabulary vocabulary,
            EnrichOptions options,
            List<VocabularyEnrichErrorResponse> errors
    ) {
        boolean changed = false;

        if (options.lookupEn() && needsLookup(vocabulary, options.overwrite())) {
            try {
                VocabularyLookupResponse lookup = vocabularyService.lookup(vocabulary.getWord());
                changed |= applyLookup(vocabulary, lookup, options.overwrite());
            } catch (RuntimeException exception) {
                errors.add(error(vocabulary, "Dictionary lookup failed"));
            }
        }

        if (options.translateVi() && needsTranslation(vocabulary, options.overwrite())) {
            try {
                JsonNode translation = geminiClientService.translateVocabulary(translationInput(vocabulary));
                changed |= applyTranslation(vocabulary, translation, options.overwrite());
            } catch (RuntimeException exception) {
                errors.add(error(vocabulary, "Vietnamese translation failed"));
            }
        }

        if (changed) {
            vocabulary.setUpdatedAt(LocalDateTime.now());
            vocabularyRepository.save(vocabulary);
        }
        return changed;
    }

    private boolean needsLookup(Vocabulary vocabulary, boolean overwrite) {
        return overwrite
                || isBlank(vocabulary.getPartOfSpeech())
                || isBlank(vocabulary.getPronunciation())
                || isBlank(vocabulary.getMeaningEn())
                || isBlank(vocabulary.getExampleSentence())
                || isBlank(vocabulary.getAudioUrl());
    }

    private boolean needsTranslation(Vocabulary vocabulary, boolean overwrite) {
        if (overwrite || isBlank(vocabulary.getMeaningVi()) || isBlank(vocabulary.getExampleSentenceVi())) {
            return !isBlank(vocabulary.getMeaningEn()) || !isBlank(vocabulary.getExampleSentence());
        }
        return false;
    }

    private boolean applyLookup(Vocabulary vocabulary, VocabularyLookupResponse lookup, boolean overwrite) {
        boolean changed = false;
        changed |= setIfAllowed(vocabulary.getPartOfSpeech(), lookup.getPartOfSpeech(), overwrite, vocabulary::setPartOfSpeech);
        changed |= setIfAllowed(vocabulary.getPronunciation(), lookup.getPronunciation(), overwrite, vocabulary::setPronunciation);
        changed |= setIfAllowed(vocabulary.getMeaningEn(), lookup.getMeaningEn(), overwrite, vocabulary::setMeaningEn);
        changed |= setIfAllowed(vocabulary.getExampleSentence(), lookup.getExampleSentenceEn(), overwrite, vocabulary::setExampleSentence);
        changed |= setIfAllowed(vocabulary.getAudioUrl(), normalizeAudioUrl(lookup.getAudioUrl()), overwrite, vocabulary::setAudioUrl);
        return changed;
    }

    private boolean applyTranslation(Vocabulary vocabulary, JsonNode translation, boolean overwrite) {
        boolean changed = false;
        changed |= setIfAllowed(vocabulary.getMeaningVi(), textOrNull(translation, "meaning_vi"), overwrite, vocabulary::setMeaningVi);
        changed |= setIfAllowed(vocabulary.getExampleSentenceVi(), textOrNull(translation, "example_sentence_vi"), overwrite, vocabulary::setExampleSentenceVi);
        return changed;
    }

    private ObjectNode translationInput(Vocabulary vocabulary) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("word", emptyIfNull(vocabulary.getWord()));
        root.put("part_of_speech", emptyIfNull(vocabulary.getPartOfSpeech()));
        root.put("meaning_en", emptyIfNull(vocabulary.getMeaningEn()));
        root.put("example_sentence_en", emptyIfNull(vocabulary.getExampleSentence()));
        root.put("meaning_vi", emptyIfNull(vocabulary.getMeaningVi()));
        root.put("example_sentence_vi", emptyIfNull(vocabulary.getExampleSentenceVi()));
        return root;
    }

    private Vocabulary getVocabulary(Long vocabularyId) {
        return vocabularyRepository.findById(vocabularyId)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_NOT_EXISTED));
    }

    private EnrichOptions options(VocabularyEnrichRequest request) {
        boolean lookupEn = request == null || request.getLookupEn() == null || request.getLookupEn();
        boolean translateVi = request == null || request.getTranslateVi() == null || request.getTranslateVi();
        boolean overwrite = request != null && Boolean.TRUE.equals(request.getOverwrite());
        return new EnrichOptions(lookupEn, translateVi, overwrite);
    }

    private boolean setIfAllowed(
            String current,
            String incoming,
            boolean overwrite,
            java.util.function.Consumer<String> setter
    ) {
        String normalized = blankToNull(incoming);
        if (normalized == null) {
            return false;
        }
        if (!overwrite && !isBlank(current)) {
            return false;
        }
        if (Objects.equals(current, normalized)) {
            return false;
        }
        setter.accept(normalized);
        return true;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return blankToNull(value.asText());
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeAudioUrl(String audioUrl) {
        String normalized = blankToNull(audioUrl);
        if (normalized != null && normalized.startsWith("//")) {
            return "https:" + normalized;
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private VocabularyEnrichErrorResponse error(Vocabulary vocabulary, String message) {
        return VocabularyEnrichErrorResponse.builder()
                .vocabularyId(vocabulary.getId())
                .word(vocabulary.getWord())
                .message(message)
                .build();
    }

    private record EnrichOptions(boolean lookupEn, boolean translateVi, boolean overwrite) {
    }
}
