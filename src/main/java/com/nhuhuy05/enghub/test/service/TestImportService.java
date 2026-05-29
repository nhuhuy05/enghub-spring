package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.media.repository.MediaAssetRepository;
import com.nhuhuy05.enghub.reading.entity.QuestionGroupPassage;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.dto.ImportErrorResponse;
import com.nhuhuy05.enghub.test.dto.ImportSummaryResponse;
import com.nhuhuy05.enghub.test.dto.TestImportResponse;
import com.nhuhuy05.enghub.test.entity.Answer;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.QuestionGroupImage;
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.repository.AnswerRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupImageRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestAttemptRepository;
import com.nhuhuy05.enghub.test.repository.TestPartRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestImportService {
    TestRepository testRepository;
    TestPartRepository testPartRepository;
    QuestionRepository questionRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionGroupImageRepository questionGroupImageRepository;
    AnswerRepository answerRepository;
    MediaAssetRepository mediaAssetRepository;
    QuestionGroupAudioRepository questionGroupAudioRepository;
    QuestionGroupPassageRepository questionGroupPassageRepository;
    TestAttemptRepository testAttemptRepository;

    static final List<String> REQUIRED_HEADERS = List.of(
            "part",
            "group_order",
            "q_number",
            "question_text",
            "option_a",
            "option_b",
            "option_c",
            "option_d",
            "correct",
            "explanation"
    );

    @Transactional
    public TestImportResponse importQuestions(Long testId, MultipartFile file, boolean replace) {
        testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        if (questionRepository.countByTestId(testId) > 0 && !replace) {
            throw new AppException(ErrorCode.TEST_ALREADY_IMPORTED);
        }
        if (replace && testAttemptRepository.existsByTestId(testId)) {
            throw new AppException(ErrorCode.TEST_HAS_ATTEMPTS);
        }

        ParseResult parseResult = parse(file);
        List<ImportErrorResponse> errors = new ArrayList<>(parseResult.errors());
        List<QuestionRow> rows = parseResult.rows();

        validateRows(testId, rows, errors);

        if (!errors.isEmpty()) {
            return TestImportResponse.builder()
                    .success(false)
                    .summary(summary(rows.size(), errors.size()))
                    .errors(errors)
                    .build();
        }

        if (replace) {
            clearImportedContent(testId);
        }
        persistRows(testId, rows);

        return TestImportResponse.builder()
                .success(true)
                .summary(summary(rows.size(), 0))
                .errors(List.of())
                .build();
    }

    private ParseResult parse(MultipartFile file) {
        List<ImportErrorResponse> errors = new ArrayList<>();
        List<QuestionRow> rows = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add(error(1, "file", "Excel file must not be empty"));
            return new ParseResult(rows, errors);
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("questions");
            if (sheet == null) {
                errors.add(error(1, "sheet", "Sheet 'questions' was not found"));
                return new ParseResult(rows, errors);
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headers = readHeaders(headerRow);
            for (String requiredHeader : REQUIRED_HEADERS) {
                if (!headers.containsKey(requiredHeader)) {
                    errors.add(error(1, requiredHeader, "Missing required column " + requiredHeader));
                }
            }
            if (!errors.isEmpty()) {
                return new ParseResult(rows, errors);
            }

            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                int excelRow = row.getRowNum() + 1;
                rows.add(QuestionRow.builder()
                        .rowNumber(excelRow)
                        .part(parseInteger(cell(row, headers, "part", formatter)))
                        .groupOrder(parseInteger(cell(row, headers, "group_order", formatter)))
                        .questionNumber(parseInteger(cell(row, headers, "q_number", formatter)))
                        .questionText(blankToNull(cell(row, headers, "question_text", formatter)))
                        .optionA(blankToNull(cell(row, headers, "option_a", formatter)))
                        .optionB(blankToNull(cell(row, headers, "option_b", formatter)))
                        .optionC(blankToNull(cell(row, headers, "option_c", formatter)))
                        .optionD(blankToNull(cell(row, headers, "option_d", formatter)))
                        .correct(blankToNull(cell(row, headers, "correct", formatter)))
                        .explanation(blankToNull(cell(row, headers, "explanation", formatter)))
                        .build());
            }
        } catch (IOException | RuntimeException exception) {
            errors.add(error(1, "file", "Excel file could not be read"));
        }

        return new ParseResult(rows, errors);
    }

    private void validateRows(Long testId, List<QuestionRow> rows, List<ImportErrorResponse> errors) {
        if (rows.isEmpty()) {
            errors.add(error(1, "questions", "Sheet 'questions' has no data rows"));
            return;
        }

        Set<Integer> questionNumbers = new HashSet<>();
        for (QuestionRow row : rows) {
            validateRequiredIntegers(row, errors);
            if (row.part() == null || row.groupOrder() == null || row.questionNumber() == null) {
                continue;
            }

            if (row.part() < 1 || row.part() > 7) {
                errors.add(error(row.rowNumber(), "part", "part must be between 1 and 7"));
            }
            if (row.groupOrder() < 1 || row.groupOrder() > 200) {
                errors.add(error(row.rowNumber(), "group_order", "group_order must be between 1 and 200"));
            }
            if (row.questionNumber() < 1 || row.questionNumber() > 200) {
                errors.add(error(row.rowNumber(), "q_number", "q_number must be between 1 and 200"));
            }
            if (!questionNumbers.add(row.questionNumber())) {
                errors.add(error(row.rowNumber(), "q_number", "q_number " + row.questionNumber() + " is duplicated"));
            }

            validateOptions(row, errors);
        }

        validatePartsExist(testId, rows, errors);
        validateMedia(testId, rows, errors);
    }

    private void validateRequiredIntegers(QuestionRow row, List<ImportErrorResponse> errors) {
        if (row.part() == null) {
            errors.add(error(row.rowNumber(), "part", "part is required and must be a number"));
        }
        if (row.groupOrder() == null) {
            errors.add(error(row.rowNumber(), "group_order", "group_order is required and must be a number"));
        }
        if (row.questionNumber() == null) {
            errors.add(error(row.rowNumber(), "q_number", "q_number is required and must be a number"));
        }
    }

    private void validateOptions(QuestionRow row, List<ImportErrorResponse> errors) {
        if (isBlank(row.optionA())) {
            errors.add(error(row.rowNumber(), "option_a", "option_a is required"));
        }
        if (isBlank(row.optionB())) {
            errors.add(error(row.rowNumber(), "option_b", "option_b is required"));
        }
        if (isBlank(row.optionC())) {
            errors.add(error(row.rowNumber(), "option_c", "option_c is required"));
        }
        if (row.part() != null && row.part() != 2 && isBlank(row.optionD())) {
            errors.add(error(row.rowNumber(), "option_d", "option_d is required for this part"));
        }
        if (isBlank(row.correct())) {
            errors.add(error(row.rowNumber(), "correct", "correct is required"));
            return;
        }

        String correct = row.correct();
        if (!correct.equals(correct.toUpperCase(Locale.ROOT))) {
            errors.add(error(row.rowNumber(), "correct", "correct must be uppercase"));
            return;
        }

        Set<String> allowed = row.part() != null && row.part() == 2
                ? Set.of("A", "B", "C")
                : Set.of("A", "B", "C", "D");
        if (!allowed.contains(correct)) {
            errors.add(error(row.rowNumber(), "correct", "correct is not valid for this part"));
        }
    }

    private void validatePartsExist(Long testId, List<QuestionRow> rows, List<ImportErrorResponse> errors) {
        Set<Integer> parts = rows.stream()
                .map(QuestionRow::part)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Integer part : parts) {
            if (part >= 1 && part <= 7 && testPartRepository.findByTestIdAndPartNumber(testId, part).isEmpty()) {
                errors.add(error(1, "part", "Test does not have part " + part));
            }
        }
    }

    private void validateMedia(Long testId, List<QuestionRow> rows, List<ImportErrorResponse> errors) {
        Map<GroupKey, List<QuestionRow>> rowsByGroup = groupValidRows(rows);

        for (Map.Entry<GroupKey, List<QuestionRow>> entry : rowsByGroup.entrySet()) {
            GroupKey key = entry.getKey();
            List<QuestionRow> groupRows = entry.getValue();

            if (key.part() == 1 && findGroupImage(testId, key, groupRows).isEmpty()) {
                errors.add(error(1, "media", "Part 1 group_order " + key.groupOrder() + " is missing image"));
            }
            if (key.part() >= 1 && key.part() <= 4 && findGroupAudio(testId, key, groupRows).isEmpty()) {
                errors.add(error(1, "media", "Part " + key.part() + " group_order " + key.groupOrder() + " is missing audio"));
            }
        }
    }

    private void persistRows(Long testId, List<QuestionRow> rows) {
        Map<Integer, TestPart> partsByNumber = testPartRepository.findAllByTestIdOrderByPartNumberAsc(testId).stream()
                .collect(Collectors.toMap(TestPart::getPartNumber, Function.identity()));

        Map<GroupKey, List<QuestionRow>> rowsByGroup = groupValidRows(rows);

        for (Map.Entry<GroupKey, List<QuestionRow>> entry : rowsByGroup.entrySet()) {
            GroupKey key = entry.getKey();
            List<QuestionRow> groupRows = entry.getValue();
            TestPart testPart = partsByNumber.get(key.part());

            QuestionGroup questionGroup = questionGroupRepository.save(QuestionGroup.builder()
                    .testPart(testPart)
                    .orderIndex(key.groupOrder())
                    .build());

            if (key.part() == 1 || key.part() == 3 || key.part() == 4) {
                findGroupImage(testId, key, groupRows)
                        .ifPresent(image -> persistGroupImage(questionGroup, image, 0));
            }

            if (key.part() >= 1 && key.part() <= 4) {
                MediaAsset audio = findGroupAudio(testId, key, groupRows)
                        .orElseThrow(() -> new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED));
                questionGroupAudioRepository.save(QuestionGroupAudio.builder()
                        .questionGroup(questionGroup)
                        .mediaAsset(audio)
                        .startMs(0)
                        .endMs(null)
                        .orderIndex(0)
                        .build());
            }

            if (key.part() == 6 || key.part() == 7) {
                persistPassageImages(testId, questionGroup, key, groupRows);
            }

            groupRows.stream()
                    .sorted(Comparator.comparing(QuestionRow::questionNumber))
                    .forEach(row -> persistQuestion(questionGroup, row));
        }
    }

    private Map<GroupKey, List<QuestionRow>> groupValidRows(List<QuestionRow> rows) {
        return rows.stream()
                .filter(row -> row.part() != null && row.groupOrder() != null && row.questionNumber() != null)
                .collect(Collectors.groupingBy(
                        row -> new GroupKey(row.part(), row.groupOrder()),
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    private Optional<MediaAsset> findGroupImage(Long testId, GroupKey key, List<QuestionRow> groupRows) {
        return findMedia(testId, "image", groupLabelCandidates(key, groupRows));
    }

    private Optional<MediaAsset> findGroupAudio(Long testId, GroupKey key, List<QuestionRow> groupRows) {
        List<String> labels = new ArrayList<>(groupLabelCandidates(key, groupRows));
        labels.add("audio_main");
        return findMedia(testId, "audio", labels);
    }

    private Optional<MediaAsset> findMedia(Long testId, String mediaType, List<String> labels) {
        return labels.stream()
                .filter(label -> label != null && !label.isBlank())
                .distinct()
                .map(label -> mediaAssetRepository.findByTestIdAndLabelAndMediaType(testId, label, mediaType))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private void persistGroupImage(QuestionGroup questionGroup, MediaAsset mediaAsset, int orderIndex) {
        questionGroupImageRepository.save(QuestionGroupImage.builder()
                .questionGroup(questionGroup)
                .mediaAsset(mediaAsset)
                .orderIndex(orderIndex)
                .build());
    }

    private void persistPassageImages(Long testId, QuestionGroup questionGroup, GroupKey key, List<QuestionRow> groupRows) {
        List<String> baseLabels = groupLabelCandidates(key, groupRows);

        List<MediaAsset> pageImages = new ArrayList<>();
        for (int page = 1; page <= 20; page++) {
            Optional<MediaAsset> pageImage = findMedia(testId, "image", pageLabelCandidates(baseLabels, page));
            if (pageImage.isEmpty()) {
                break;
            }
            pageImages.add(pageImage.get());
        }

        if (pageImages.isEmpty()) {
            findMedia(testId, "image", baseLabels).ifPresent(pageImages::add);
        }

        for (int index = 0; index < pageImages.size(); index++) {
            MediaAsset image = pageImages.get(index);
            questionGroupPassageRepository.save(QuestionGroupPassage.builder()
                    .questionGroup(questionGroup)
                    .title(image.getLabel())
                    .passageType("image")
                    .contentFormat("image")
                    .mediaAsset(image)
                    .orderIndex(index)
                    .build());
        }
    }

    private List<String> groupLabelCandidates(GroupKey key, List<QuestionRow> groupRows) {
        int start = groupRows.stream()
                .map(QuestionRow::questionNumber)
                .min(Integer::compareTo)
                .orElse(key.groupOrder());
        int end = groupRows.stream()
                .map(QuestionRow::questionNumber)
                .max(Integer::compareTo)
                .orElse(start);

        List<String> labels = new ArrayList<>();
        labels.add(canonicalLabel(key.part(), start, end));
        labels.add(plainRangeLabel(start, end));
        labels.add(String.valueOf(key.groupOrder()));
        return labels;
    }

    private List<String> pageLabelCandidates(List<String> baseLabels, int page) {
        List<String> labels = new ArrayList<>();
        for (String baseLabel : baseLabels) {
            labels.add(baseLabel + "-" + String.format("%02d", page));
            labels.add(baseLabel + "-" + page);
            labels.add(baseLabel + "(" + page + ")");
        }
        return labels;
    }

    private String canonicalLabel(int part, int start, int end) {
        if (start == end) {
            return String.format("p%02d-q%03d", part, start);
        }
        return String.format("p%02d-q%03d-%03d", part, start, end);
    }

    private String plainRangeLabel(int start, int end) {
        if (start == end) {
            return String.valueOf(start);
        }
        return start + "-" + end;
    }

    private void clearImportedContent(Long testId) {
        questionGroupRepository.deleteAll(questionGroupRepository.findAllByTestPartTestId(testId));
        questionGroupRepository.flush();
    }

    private void persistQuestion(QuestionGroup questionGroup, QuestionRow row) {
        Question question = questionRepository.save(Question.builder()
                .questionGroup(questionGroup)
                .questionNumber(row.questionNumber())
                .questionText(row.questionText())
                .explanation(row.explanation())
                .build());

        List<Answer> answers = new ArrayList<>();
        answers.add(answer(question, row.optionA(), "A".equals(row.correct())));
        answers.add(answer(question, row.optionB(), "B".equals(row.correct())));
        answers.add(answer(question, row.optionC(), "C".equals(row.correct())));
        if (row.part() != 2) {
            answers.add(answer(question, row.optionD(), "D".equals(row.correct())));
        }
        answerRepository.saveAll(answers);
    }

    private Answer answer(Question question, String text, boolean correct) {
        return Answer.builder()
                .question(question)
                .answerText(text)
                .isCorrect(correct)
                .build();
    }

    private Map<String, Integer> readHeaders(Row headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        if (headerRow == null) {
            return headers;
        }

        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell);
            if (!isBlank(header)) {
                headers.put(header.trim().toLowerCase(Locale.ROOT), cell.getColumnIndex());
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
            if (!isBlank(formatter.formatCellValue(cell))) {
                return false;
            }
        }
        return true;
    }

    private Integer parseInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private ImportErrorResponse error(Integer row, String field, String message) {
        return ImportErrorResponse.builder()
                .row(row)
                .field(field)
                .message(message)
                .build();
    }

    private ImportSummaryResponse summary(int rowCount, int errorCount) {
        return ImportSummaryResponse.builder()
                .totalRows(rowCount)
                .validRows(Math.max(rowCount - errorCount, 0))
                .errorCount(errorCount)
                .build();
    }

    private record ParseResult(List<QuestionRow> rows, List<ImportErrorResponse> errors) {
    }

    private record GroupKey(Integer part, Integer groupOrder) implements Comparable<GroupKey> {
        @Override
        public int compareTo(GroupKey other) {
            int partCompare = part.compareTo(other.part);
            if (partCompare != 0) {
                return partCompare;
            }
            return groupOrder.compareTo(other.groupOrder);
        }
    }

    @Builder
    private record QuestionRow(
            Integer rowNumber,
            Integer part,
            Integer groupOrder,
            Integer questionNumber,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correct,
            String explanation
    ) {
    }
}
