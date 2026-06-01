package com.nhuhuy05.enghub.vocabulary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhuhuy05.enghub.ai.service.GeminiClientService;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.user.entity.User;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import com.nhuhuy05.enghub.vocabulary.dto.*;
import com.nhuhuy05.enghub.vocabulary.entity.*;
import com.nhuhuy05.enghub.vocabulary.enums.VocabularyReviewRating;
import com.nhuhuy05.enghub.vocabulary.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VocabularyService {
    private static final BigDecimal DEFAULT_EASE_FACTOR = BigDecimal.valueOf(2.50);
    private static final BigDecimal MIN_EASE_FACTOR = BigDecimal.valueOf(1.30);
    private static final List<VocabularyReviewRating> REVIEW_RATINGS = List.of(
            VocabularyReviewRating.AGAIN,
            VocabularyReviewRating.HARD,
            VocabularyReviewRating.GOOD,
            VocabularyReviewRating.EASY
    );

    VocabularyRepository vocabularyRepository;
    VocabularyTopicRepository vocabularyTopicRepository;
    VocabularyTopicMapRepository vocabularyTopicMapRepository;
    UserVocabularyProgressRepository userVocabularyProgressRepository;
    UserVocabularyReviewRepository userVocabularyReviewRepository;
    UserRepository userRepository;
    GeminiClientService geminiClientService;
    RestClient.Builder restClientBuilder;
    ObjectMapper objectMapper;

    @Transactional
    public VocabularyTopicResponse createTopic(VocabularyTopicRequest request) {
        String name = requiredTrim(request.getName());
        if (vocabularyTopicRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.VOCABULARY_TOPIC_EXISTED);
        }

        VocabularyTopic topic = vocabularyTopicRepository.save(VocabularyTopic.builder()
                .name(name)
                .description(blankToNull(request.getDescription()))
                .build());
        return toTopicResponse(topic, 0L);
    }

    @Transactional(readOnly = true)
    public List<VocabularyTopicResponse> getTopics() {
        return vocabularyTopicRepository.findAll().stream()
                .map(topic -> toTopicResponse(topic, vocabularyTopicMapRepository.countByTopicId(topic.getId())))
                .toList();
    }

    @Transactional
    public VocabularyTopicResponse updateTopic(Long topicId, VocabularyTopicRequest request) {
        VocabularyTopic topic = getTopic(topicId);
        String name = requiredTrim(request.getName());
        Optional<VocabularyTopic> existingTopic = vocabularyTopicRepository.findByNameIgnoreCase(name);
        if (existingTopic.isPresent() && !existingTopic.get().getId().equals(topicId)) {
            throw new AppException(ErrorCode.VOCABULARY_TOPIC_EXISTED);
        }

        topic.setName(name);
        topic.setDescription(blankToNull(request.getDescription()));
        topic.setUpdatedAt(LocalDateTime.now());
        return toTopicResponse(vocabularyTopicRepository.save(topic), vocabularyTopicMapRepository.countByTopicId(topicId));
    }

    @Transactional
    public void deleteTopic(Long topicId) {
        VocabularyTopic topic = getTopic(topicId);
        vocabularyTopicRepository.delete(topic);
    }

    @Transactional
    public VocabularyResponse createVocabulary(VocabularyRequest request) {
        String word = requiredTrim(request.getWord());
        if (vocabularyRepository.existsByWordIgnoreCase(word)) {
            throw new AppException(ErrorCode.VOCABULARY_EXISTED);
        }

        Vocabulary vocabulary = Vocabulary.builder()
                .word(word)
                .build();
        applyVocabularyRequest(vocabulary, request, false);
        Vocabulary savedVocabulary = vocabularyRepository.save(vocabulary);
        replaceTopics(savedVocabulary, request.getTopicIds());
        return toVocabularyResponse(savedVocabulary, null);
    }

    @Transactional(readOnly = true)
    public List<VocabularyResponse> searchVocabulary(Long topicId, String keyword) {
        if (topicId != null) {
            getTopic(topicId);
        }
        return vocabularyRepository.search(topicId, searchKeyword(keyword)).stream()
                .map(vocabulary -> toVocabularyResponse(vocabulary, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public VocabularyResponse getVocabulary(Long vocabularyId, String userEmail) {
        Vocabulary vocabulary = getVocabulary(vocabularyId);
        UserVocabularyProgress progress = userProgressOrNull(userEmail, vocabularyId);
        return toVocabularyResponse(vocabulary, progress);
    }

    @Transactional
    public VocabularyResponse updateVocabulary(Long vocabularyId, VocabularyRequest request) {
        Vocabulary vocabulary = getVocabulary(vocabularyId);
        String word = requiredTrim(request.getWord());
        Optional<Vocabulary> existingVocabulary = vocabularyRepository.findByWordIgnoreCase(word);
        if (existingVocabulary.isPresent() && !existingVocabulary.get().getId().equals(vocabularyId)) {
            throw new AppException(ErrorCode.VOCABULARY_EXISTED);
        }

        vocabulary.setWord(word);
        applyVocabularyRequest(vocabulary, request, true);
        Vocabulary savedVocabulary = vocabularyRepository.save(vocabulary);
        replaceTopics(savedVocabulary, request.getTopicIds());
        return toVocabularyResponse(savedVocabulary, null);
    }

    @Transactional
    public void deleteVocabulary(Long vocabularyId) {
        vocabularyRepository.delete(getVocabulary(vocabularyId));
    }

    @Transactional
    public VocabularyResponse attachTopic(Long vocabularyId, Long topicId) {
        Vocabulary vocabulary = getVocabulary(vocabularyId);
        VocabularyTopic topic = getTopic(topicId);
        VocabularyTopicMapId id = new VocabularyTopicMapId(vocabularyId, topicId);
        if (!vocabularyTopicMapRepository.existsById(id)) {
            vocabularyTopicMapRepository.save(VocabularyTopicMap.builder()
                    .id(id)
                    .vocabulary(vocabulary)
                    .topic(topic)
                    .build());
        }
        return toVocabularyResponse(vocabulary, null);
    }

    @Transactional
    public VocabularyResponse detachTopic(Long vocabularyId, Long topicId) {
        Vocabulary vocabulary = getVocabulary(vocabularyId);
        getTopic(topicId);
        vocabularyTopicMapRepository.deleteById(new VocabularyTopicMapId(vocabularyId, topicId));
        return toVocabularyResponse(vocabulary, null);
    }

    @Transactional(readOnly = true)
    public List<VocabularyResponse> getTopicWords(Long topicId, String userEmail) {
        getTopic(topicId);
        List<Vocabulary> vocabularies = vocabularyRepository.search(topicId, searchKeyword(null));
        Map<Long, UserVocabularyProgress> progressByVocabularyId = progressByVocabularyId(userEmail, vocabularies);
        return vocabularies.stream()
                .map(vocabulary -> toVocabularyResponse(vocabulary, progressByVocabularyId.get(vocabulary.getId())))
                .toList();
    }

    @Transactional
    public VocabularyResponse learn(String userEmail, Long vocabularyId) {
        User user = getUser(userEmail);
        Vocabulary vocabulary = getVocabulary(vocabularyId);
        UserVocabularyProgress progress = userVocabularyProgressRepository
                .findByUserIdAndVocabularyId(user.getId(), vocabularyId)
                .orElseGet(() -> userVocabularyProgressRepository.save(newProgress(user, vocabulary)));
        return toVocabularyResponse(vocabulary, progress);
    }

    @Transactional(readOnly = true)
    public List<VocabularyResponse> getProgress(String userEmail) {
        User user = getUser(userEmail);
        return userVocabularyProgressRepository.findAllByUserIdOrderByLearnedAtDesc(user.getId()).stream()
                .map(progress -> toVocabularyResponse(progress.getVocabulary(), progress))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VocabularyResponse> getDue(String userEmail, Long topicId) {
        User user = getUser(userEmail);
        if (topicId != null) {
            getTopic(topicId);
        }
        return userVocabularyProgressRepository.findDue(user.getId(), topicId, LocalDateTime.now()).stream()
                .map(progress -> toVocabularyResponse(progress.getVocabulary(), progress))
                .toList();
    }

    @Transactional
    public VocabularyResponse review(String userEmail, Long vocabularyId, VocabularyReviewRequest request) {
        User user = getUser(userEmail);
        Vocabulary vocabulary = getVocabulary(vocabularyId);
        UserVocabularyProgress progress = userVocabularyProgressRepository
                .findByUserIdAndVocabularyId(user.getId(), vocabularyId)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_PROGRESS_NOT_EXISTED));

        LocalDateTime now = LocalDateTime.now();
        VocabularyReviewRating rating = request.getRating();
        userVocabularyReviewRepository.save(UserVocabularyReview.builder()
                .user(user)
                .vocabulary(vocabulary)
                .rating(rating)
                .reviewedAt(now)
                .build());

        applyReview(progress, rating, now);
        return toVocabularyResponse(vocabulary, userVocabularyProgressRepository.save(progress));
    }

    @Transactional(readOnly = true)
    public VocabularyLookupResponse lookup(String word) {
        String normalizedWord = requiredTrim(word);
        try {
            String response = restClientBuilder.build()
                    .get()
                    .uri("https://api.dictionaryapi.dev/api/v2/entries/en/{word}", normalizedWord)
                    .retrieve()
                    .body(String.class);
            return translateLookup(parseLookup(response, normalizedWord));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new AppException(ErrorCode.VOCABULARY_NOT_EXISTED);
            }
            throw new AppException(ErrorCode.INVALID_KEY);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private void applyVocabularyRequest(Vocabulary vocabulary, VocabularyRequest request, boolean updateTimestamp) {
        vocabulary.setMeaningVi(blankToNull(request.getMeaningVi()));
        vocabulary.setMeaningEn(blankToNull(request.getMeaningEn()));
        vocabulary.setPartOfSpeech(blankToNull(request.getPartOfSpeech()));
        vocabulary.setPronunciation(blankToNull(request.getPronunciation()));
        vocabulary.setExampleSentence(blankToNull(request.getExampleSentenceEn()));
        vocabulary.setExampleSentenceVi(blankToNull(request.getExampleSentenceVi()));
        vocabulary.setAudioUrl(normalizeAudioUrl(request.getAudioUrl()));
        if (updateTimestamp) {
            vocabulary.setUpdatedAt(LocalDateTime.now());
        }
    }

    private void replaceTopics(Vocabulary vocabulary, List<Long> topicIds) {
        vocabularyTopicMapRepository.deleteAll(vocabularyTopicMapRepository.findAllByVocabularyId(vocabulary.getId()));
        Set<Long> distinctTopicIds = topicIds == null ? Set.of() : new LinkedHashSet<>(topicIds);
        for (Long topicId : distinctTopicIds) {
            if (topicId == null) {
                continue;
            }
            VocabularyTopic topic = getTopic(topicId);
            vocabularyTopicMapRepository.save(VocabularyTopicMap.builder()
                    .id(new VocabularyTopicMapId(vocabulary.getId(), topicId))
                    .vocabulary(vocabulary)
                    .topic(topic)
                    .build());
        }
    }

    private UserVocabularyProgress newProgress(User user, Vocabulary vocabulary) {
        LocalDateTime now = LocalDateTime.now();
        return UserVocabularyProgress.builder()
                .user(user)
                .vocabulary(vocabulary)
                .level(0)
                .learnedAt(now)
                .lastReviewedAt(null)
                .nextReviewAt(now)
                .reviewCount(0)
                .correctCount(0)
                .intervalDays(0)
                .easeFactor(DEFAULT_EASE_FACTOR)
                .mastered(false)
                .build();
    }

    private void applyReview(UserVocabularyProgress progress, VocabularyReviewRating rating, LocalDateTime now) {
        int reviewCount = safeInt(progress.getReviewCount()) + 1;
        int correctCount = safeInt(progress.getCorrectCount());
        int level = safeInt(progress.getLevel());
        BigDecimal easeFactor = progress.getEaseFactor() == null ? DEFAULT_EASE_FACTOR : progress.getEaseFactor();
        ReviewSchedule schedule = reviewSchedule(rating, now);

        switch (rating) {
            case AGAIN -> {
                level = Math.max(0, level - 1);
                easeFactor = decreaseEase(easeFactor, "0.20");
            }
            case HARD -> {
                easeFactor = decreaseEase(easeFactor, "0.15");
                correctCount++;
            }
            case GOOD -> {
                level++;
                correctCount++;
            }
            case EASY -> {
                level += 2;
                easeFactor = easeFactor.add(BigDecimal.valueOf(0.15));
                correctCount++;
            }
            default -> throw new AppException(ErrorCode.INVALID_KEY);
        }

        progress.setLevel(level);
        progress.setReviewCount(reviewCount);
        progress.setCorrectCount(correctCount);
        progress.setIntervalDays(schedule.intervalDays());
        progress.setEaseFactor(easeFactor.setScale(2, RoundingMode.HALF_UP));
        progress.setLastReviewedAt(now);
        progress.setNextReviewAt(schedule.nextReviewAt());
        progress.setMastered(level >= 5 && correctCount >= 5);
    }

    private VocabularyLookupResponse parseLookup(String response, String fallbackWord) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode entry = root.isArray() && !root.isEmpty() ? root.get(0) : root;
        String word = textOrNull(entry, "word");

        String pronunciation = null;
        String audioUrl = null;
        JsonNode phonetics = entry.path("phonetics");
        if (phonetics.isArray()) {
            for (JsonNode phonetic : phonetics) {
                if (pronunciation == null) {
                    pronunciation = textOrNull(phonetic, "text");
                }
                if (audioUrl == null) {
                    audioUrl = normalizeAudioUrl(textOrNull(phonetic, "audio"));
                }
                if (pronunciation != null && audioUrl != null) {
                    break;
                }
            }
        }

        String partOfSpeech = null;
        String meaningEn = null;
        String exampleSentenceEn = null;
        String fallbackPartOfSpeech = null;
        String fallbackMeaningEn = null;
        JsonNode meanings = entry.path("meanings");
        if (meanings.isArray()) {
            for (JsonNode meaning : meanings) {
                String currentPartOfSpeech = textOrNull(meaning, "partOfSpeech");
                JsonNode definitions = meaning.path("definitions");
                if (!definitions.isArray()) {
                    continue;
                }
                for (JsonNode definition : definitions) {
                    String currentMeaningEn = textOrNull(definition, "definition");
                    if (fallbackMeaningEn == null && currentMeaningEn != null) {
                        fallbackMeaningEn = currentMeaningEn;
                        fallbackPartOfSpeech = currentPartOfSpeech;
                    }
                    String currentExampleSentenceEn = textOrNull(definition, "example");
                    if (currentExampleSentenceEn != null) {
                        partOfSpeech = currentPartOfSpeech;
                        meaningEn = currentMeaningEn == null ? fallbackMeaningEn : currentMeaningEn;
                        exampleSentenceEn = currentExampleSentenceEn;
                        break;
                    }
                }
                if (exampleSentenceEn != null) {
                    break;
                }
            }
        }

        if (meaningEn == null) {
            meaningEn = fallbackMeaningEn;
            partOfSpeech = fallbackPartOfSpeech;
        }
        if (partOfSpeech == null && meanings.isArray()) {
            for (JsonNode meaning : meanings) {
                partOfSpeech = textOrNull(meaning, "partOfSpeech");
                if (partOfSpeech != null) {
                    break;
                }
            }
        }

        return VocabularyLookupResponse.builder()
                .word(word == null ? fallbackWord : word)
                .partOfSpeech(partOfSpeech)
                .pronunciation(pronunciation)
                .meaningEn(meaningEn)
                .exampleSentenceEn(exampleSentenceEn)
                .audioUrl(audioUrl)
                .build();
    }

    private VocabularyLookupResponse translateLookup(VocabularyLookupResponse lookup) {
        if (lookup == null || (blankToNull(lookup.getMeaningEn()) == null && blankToNull(lookup.getExampleSentenceEn()) == null)) {
            return lookup;
        }

        JsonNode translation = geminiClientService.translateVocabulary(translationInput(lookup));
        lookup.setMeaningVi(textOrNull(translation, "meaning_vi"));
        lookup.setExampleSentenceVi(textOrNull(translation, "example_sentence_vi"));
        return lookup;
    }

    private ObjectNode translationInput(VocabularyLookupResponse lookup) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("word", emptyIfNull(lookup.getWord()));
        root.put("part_of_speech", emptyIfNull(lookup.getPartOfSpeech()));
        root.put("meaning_en", emptyIfNull(lookup.getMeaningEn()));
        root.put("example_sentence_en", emptyIfNull(lookup.getExampleSentenceEn()));
        root.put("meaning_vi", "");
        root.put("example_sentence_vi", "");
        return root;
    }

    private VocabularyTopic getTopic(Long topicId) {
        return vocabularyTopicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_TOPIC_NOT_EXISTED));
    }

    private Vocabulary getVocabulary(Long vocabularyId) {
        return vocabularyRepository.findById(vocabularyId)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_NOT_EXISTED));
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private UserVocabularyProgress userProgressOrNull(String userEmail, Long vocabularyId) {
        if (userEmail == null || userEmail.isBlank() || "anonymousUser".equals(userEmail)) {
            return null;
        }
        return userRepository.findByEmail(userEmail)
                .flatMap(user -> userVocabularyProgressRepository.findByUserIdAndVocabularyId(user.getId(), vocabularyId))
                .orElse(null);
    }

    private Map<Long, UserVocabularyProgress> progressByVocabularyId(String userEmail, List<Vocabulary> vocabularies) {
        if (userEmail == null || userEmail.isBlank() || "anonymousUser".equals(userEmail) || vocabularies.isEmpty()) {
            return Map.of();
        }
        Optional<User> user = userRepository.findByEmail(userEmail);
        if (user.isEmpty()) {
            return Map.of();
        }
        List<Long> vocabularyIds = vocabularies.stream().map(Vocabulary::getId).toList();
        return userVocabularyProgressRepository.findAllByUserIdAndVocabularyIdIn(user.get().getId(), vocabularyIds).stream()
                .collect(Collectors.toMap(progress -> progress.getVocabulary().getId(), progress -> progress));
    }

    private VocabularyResponse toVocabularyResponse(Vocabulary vocabulary, UserVocabularyProgress progress) {
        return VocabularyResponse.builder()
                .id(vocabulary.getId())
                .word(vocabulary.getWord())
                .meaningVi(vocabulary.getMeaningVi())
                .meaningEn(vocabulary.getMeaningEn())
                .partOfSpeech(vocabulary.getPartOfSpeech())
                .pronunciation(vocabulary.getPronunciation())
                .exampleSentenceEn(vocabulary.getExampleSentence())
                .exampleSentenceVi(vocabulary.getExampleSentenceVi())
                .audioUrl(vocabulary.getAudioUrl())
                .topics(vocabularyTopicMapRepository.findAllByVocabularyId(vocabulary.getId()).stream()
                        .map(topicMap -> toTopicResponse(topicMap.getTopic(), null))
                        .toList())
                .progress(progress == null ? null : toProgressResponse(progress))
                .reviewOptions(reviewOptions(LocalDateTime.now()))
                .createdAt(vocabulary.getCreatedAt())
                .updatedAt(vocabulary.getUpdatedAt())
                .build();
    }

    private List<VocabularyReviewOptionResponse> reviewOptions(LocalDateTime now) {
        return REVIEW_RATINGS.stream()
                .map(rating -> {
                    ReviewSchedule schedule = reviewSchedule(rating, now);
                    return VocabularyReviewOptionResponse.builder()
                            .rating(rating)
                            .label(reviewLabel(rating))
                            .delayLabel(schedule.delayLabel())
                            .nextReviewAt(schedule.nextReviewAt())
                            .build();
                })
                .toList();
    }

    private ReviewSchedule reviewSchedule(VocabularyReviewRating rating, LocalDateTime now) {
        return switch (rating) {
            case AGAIN -> new ReviewSchedule("< 10m", now.plusMinutes(10), 0);
            case HARD -> new ReviewSchedule("1d", now.plusDays(1), 1);
            case GOOD -> new ReviewSchedule("3d", now.plusDays(3), 3);
            case EASY -> new ReviewSchedule("7d", now.plusDays(7), 7);
        };
    }

    private String reviewLabel(VocabularyReviewRating rating) {
        return switch (rating) {
            case AGAIN -> "Again";
            case HARD -> "Hard";
            case GOOD -> "Good";
            case EASY -> "Easy";
        };
    }

    private VocabularyTopicResponse toTopicResponse(VocabularyTopic topic, Long wordCount) {
        return VocabularyTopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .wordCount(wordCount)
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }

    private VocabularyProgressResponse toProgressResponse(UserVocabularyProgress progress) {
        return VocabularyProgressResponse.builder()
                .id(progress.getId())
                .vocabularyId(progress.getVocabulary().getId())
                .level(progress.getLevel())
                .learnedAt(progress.getLearnedAt())
                .lastReviewedAt(progress.getLastReviewedAt())
                .nextReviewAt(progress.getNextReviewAt())
                .reviewCount(progress.getReviewCount())
                .correctCount(progress.getCorrectCount())
                .intervalDays(progress.getIntervalDays())
                .easeFactor(progress.getEaseFactor())
                .mastered(progress.isMastered())
                .build();
    }

    private String requiredTrim(String value) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return trimmed;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String searchKeyword(String value) {
        String keyword = blankToNull(value);
        return keyword == null ? "" : keyword;
    }

    private String normalizeAudioUrl(String audioUrl) {
        String normalized = blankToNull(audioUrl);
        if (normalized != null && normalized.startsWith("//")) {
            return "https:" + normalized;
        }
        return normalized;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return blankToNull(value.asText());
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal decreaseEase(BigDecimal easeFactor, String value) {
        BigDecimal decreased = easeFactor.subtract(new BigDecimal(value));
        if (decreased.compareTo(MIN_EASE_FACTOR) < 0) {
            return MIN_EASE_FACTOR;
        }
        return decreased;
    }

    private record ReviewSchedule(String delayLabel, LocalDateTime nextReviewAt, int intervalDays) {
    }
}
