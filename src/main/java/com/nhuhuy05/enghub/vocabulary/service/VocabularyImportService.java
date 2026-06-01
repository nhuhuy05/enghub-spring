package com.nhuhuy05.enghub.vocabulary.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.vocabulary.dto.VocabularyImportErrorResponse;
import com.nhuhuy05.enghub.vocabulary.dto.VocabularyImportResponse;
import com.nhuhuy05.enghub.vocabulary.entity.Vocabulary;
import com.nhuhuy05.enghub.vocabulary.entity.VocabularyTopic;
import com.nhuhuy05.enghub.vocabulary.entity.VocabularyTopicMap;
import com.nhuhuy05.enghub.vocabulary.entity.VocabularyTopicMapId;
import com.nhuhuy05.enghub.vocabulary.repository.VocabularyRepository;
import com.nhuhuy05.enghub.vocabulary.repository.VocabularyTopicMapRepository;
import com.nhuhuy05.enghub.vocabulary.repository.VocabularyTopicRepository;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VocabularyImportService {
    VocabularyTopicRepository vocabularyTopicRepository;
    VocabularyRepository vocabularyRepository;
    VocabularyTopicMapRepository vocabularyTopicMapRepository;

    @Transactional
    public VocabularyImportResponse importToTopic(Long topicId, MultipartFile file, boolean replace) {
        VocabularyTopic topic = vocabularyTopicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_TOPIC_NOT_EXISTED));

        ParseResult parseResult = parse(file);
        if (!parseResult.errors().isEmpty()) {
            return response(topicId, parseResult.rows().size(), 0, 0, 0, parseResult.errors(), false);
        }

        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        List<VocabularyImportErrorResponse> errors = new ArrayList<>();

        for (VocabularyRow row : parseResult.rows()) {
            String word = blankToNull(row.word());
            if (word == null) {
                errors.add(error(row.rowNumber(), "word", "word is required"));
                continue;
            }

            Optional<Vocabulary> existingVocabulary = vocabularyRepository.findByWordIgnoreCase(word);
            if (existingVocabulary.isEmpty()) {
                Vocabulary vocabulary = Vocabulary.builder()
                        .word(word)
                        .partOfSpeech(blankToNull(row.partOfSpeech()))
                        .pronunciation(blankToNull(row.pronunciation()))
                        .meaningEn(blankToNull(row.meaningEn()))
                        .meaningVi(blankToNull(row.meaningVi()))
                        .exampleSentence(blankToNull(row.exampleSentenceEn()))
                        .exampleSentenceVi(blankToNull(row.exampleSentenceVi()))
                        .audioUrl(normalizeAudioUrl(row.audioUrl()))
                        .build();
                Vocabulary savedVocabulary = vocabularyRepository.save(vocabulary);
                attachTopic(savedVocabulary, topic);
                createdCount++;
                continue;
            }

            Vocabulary vocabulary = existingVocabulary.get();
            boolean attached = attachTopic(vocabulary, topic);
            boolean changed = false;
            if (replace) {
                changed = applyRow(vocabulary, row);
                if (changed) {
                    vocabulary.setUpdatedAt(LocalDateTime.now());
                    vocabularyRepository.save(vocabulary);
                }
            }

            if (changed || attached) {
                updatedCount++;
            } else {
                skippedCount++;
            }
        }

        boolean success = errors.isEmpty();
        return response(topicId, parseResult.rows().size(), createdCount, updatedCount, skippedCount, errors, success);
    }

    private ParseResult parse(MultipartFile file) {
        List<VocabularyImportErrorResponse> errors = new ArrayList<>();
        List<VocabularyRow> rows = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add(error(1, "file", "Vocabulary import file must not be empty"));
            return new ParseResult(rows, errors);
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (filename.endsWith(".csv")) {
                rows.addAll(parseCsv(file, errors));
            } else {
                rows.addAll(parseWorkbook(file, errors));
            }
        } catch (Exception exception) {
            errors.add(error(1, "file", "Vocabulary import file could not be read"));
        }

        return new ParseResult(rows, errors);
    }

    private List<VocabularyRow> parseWorkbook(MultipartFile file, List<VocabularyImportErrorResponse> errors) throws Exception {
        List<VocabularyRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                errors.add(error(1, "sheet", "Workbook has no sheets"));
                return rows;
            }

            Map<String, Integer> headers = readHeaders(sheet.getRow(0));
            if (!headers.containsKey("word")) {
                errors.add(error(1, "word", "Missing required column word"));
                return rows;
            }

            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                rows.add(toRow(row.getRowNum() + 1, name -> cell(row, headers, name, formatter)));
            }
        }
        return rows;
    }

    private List<VocabularyRow> parseCsv(MultipartFile file, List<VocabularyImportErrorResponse> errors) throws Exception {
        List<VocabularyRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                errors.add(error(1, "file", "CSV file has no header row"));
                return rows;
            }
            List<String> headerValues = parseCsvLine(stripBom(headerLine));
            Map<String, Integer> headers = new HashMap<>();
            for (int i = 0; i < headerValues.size(); i++) {
                String header = normalizeHeader(headerValues.get(i));
                if (header != null) {
                    headers.put(header, i);
                }
            }
            if (!headers.containsKey("word")) {
                errors.add(error(1, "word", "Missing required column word"));
                return rows;
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                List<String> values = parseCsvLine(line);
                if (values.stream().allMatch(value -> blankToNull(value) == null)) {
                    continue;
                }
                rows.add(toRow(rowNumber, name -> value(values, headers, name)));
            }
        }
        return rows;
    }

    private VocabularyRow toRow(Integer rowNumber, ValueReader reader) {
        return VocabularyRow.builder()
                .rowNumber(rowNumber)
                .word(reader.get("word"))
                .partOfSpeech(reader.get("part_of_speech"))
                .pronunciation(reader.get("pronunciation"))
                .meaningEn(reader.get("meaning_en"))
                .meaningVi(reader.get("meaning_vi"))
                .exampleSentenceEn(reader.get("example_sentence_en"))
                .exampleSentenceVi(reader.get("example_sentence_vi"))
                .audioUrl(reader.get("audio_url"))
                .build();
    }

    private boolean applyRow(Vocabulary vocabulary, VocabularyRow row) {
        boolean changed = false;
        changed |= setIfPresent(row.partOfSpeech(), vocabulary.getPartOfSpeech(), vocabulary::setPartOfSpeech);
        changed |= setIfPresent(row.pronunciation(), vocabulary.getPronunciation(), vocabulary::setPronunciation);
        changed |= setIfPresent(row.meaningEn(), vocabulary.getMeaningEn(), vocabulary::setMeaningEn);
        changed |= setIfPresent(row.meaningVi(), vocabulary.getMeaningVi(), vocabulary::setMeaningVi);
        changed |= setIfPresent(row.exampleSentenceEn(), vocabulary.getExampleSentence(), vocabulary::setExampleSentence);
        changed |= setIfPresent(row.exampleSentenceVi(), vocabulary.getExampleSentenceVi(), vocabulary::setExampleSentenceVi);
        changed |= setIfPresent(normalizeAudioUrl(row.audioUrl()), vocabulary.getAudioUrl(), vocabulary::setAudioUrl);
        return changed;
    }

    private boolean attachTopic(Vocabulary vocabulary, VocabularyTopic topic) {
        VocabularyTopicMapId id = new VocabularyTopicMapId(vocabulary.getId(), topic.getId());
        if (vocabularyTopicMapRepository.existsById(id)) {
            return false;
        }
        vocabularyTopicMapRepository.save(VocabularyTopicMap.builder()
                .id(id)
                .vocabulary(vocabulary)
                .topic(topic)
                .build());
        return true;
    }

    private Map<String, Integer> readHeaders(Row headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        if (headerRow == null) {
            return headers;
        }
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            if (header != null) {
                headers.put(header, cell.getColumnIndex());
            }
        }
        return headers;
    }

    private String cell(Row row, Map<String, Integer> headers, String name, DataFormatter formatter) {
        Integer index = headers.get(name);
        if (index == null) {
            return null;
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (blankToNull(formatter.formatCellValue(cell)) != null) {
                return false;
            }
        }
        return true;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private String value(List<String> values, Map<String, Integer> headers, String name) {
        Integer index = headers.get(name);
        if (index == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    private String normalizeHeader(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        return switch (normalized) {
            case "example_sentence", "example_en" -> "example_sentence_en";
            case "example_vi" -> "example_sentence_vi";
            case "pos" -> "part_of_speech";
            case "audio" -> "audio_url";
            default -> normalized;
        };
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

    private boolean setIfPresent(String incoming, String current, java.util.function.Consumer<String> setter) {
        String normalized = blankToNull(incoming);
        if (normalized == null || Objects.equals(normalized, current)) {
            return false;
        }
        setter.accept(normalized);
        return true;
    }

    private String stripBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private VocabularyImportErrorResponse error(Integer row, String field, String message) {
        return VocabularyImportErrorResponse.builder()
                .row(row)
                .field(field)
                .message(message)
                .build();
    }

    private VocabularyImportResponse response(
            Long topicId,
            int totalRows,
            int createdCount,
            int updatedCount,
            int skippedCount,
            List<VocabularyImportErrorResponse> errors,
            boolean success
    ) {
        return VocabularyImportResponse.builder()
                .success(success)
                .topicId(topicId)
                .totalRows(totalRows)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .errors(errors)
                .build();
    }

    private record ParseResult(List<VocabularyRow> rows, List<VocabularyImportErrorResponse> errors) {
    }

    @Builder
    private record VocabularyRow(
            Integer rowNumber,
            String word,
            String partOfSpeech,
            String pronunciation,
            String meaningEn,
            String meaningVi,
            String exampleSentenceEn,
            String exampleSentenceVi,
            String audioUrl
    ) {
    }

    @FunctionalInterface
    private interface ValueReader {
        String get(String name);
    }
}
