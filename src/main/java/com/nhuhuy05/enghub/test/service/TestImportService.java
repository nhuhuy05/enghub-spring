package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupTranscriptLine;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupTranscriptLineRepository;
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
import com.nhuhuy05.enghub.test.repository.QuestionGroupImageRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
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
    QuestionGroupTranscriptLineRepository questionGroupTranscriptLineRepository;
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

    static final List<String> TRANSCRIPT_HEADERS = List.of(
            "part",
            "group_order",
            "transcript_en",
            "transcript_vi"
    );

    static final List<String> TRANSCRIPT_LINE_HEADERS = List.of(
            "part",
            "group_order",
            "order_index",
            "speaker",
            "text_en",
            "text_vi",
            "start_ms",
            "end_ms"
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
        List<TranscriptRow> transcriptRows = parseResult.transcriptRows();
        List<TranscriptLineRow> transcriptLineRows = parseResult.transcriptLineRows();

        validateRows(testId, rows, errors);
        validateTranscriptRows(rows, transcriptRows, transcriptLineRows, errors);

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
        Map<GroupKey, QuestionGroupAudio> audiosByGroup = persistRows(testId, rows);
        persistTranscripts(audiosByGroup, transcriptRows, transcriptLineRows);

        return TestImportResponse.builder()
                .success(true)
                .summary(summary(rows.size(), 0))
                .errors(List.of())
                .build();
    }

    private ParseResult parse(MultipartFile file) {
        List<ImportErrorResponse> errors = new ArrayList<>();
        List<QuestionRow> rows = new ArrayList<>();
        List<TranscriptRow> transcriptRows = new ArrayList<>();
        List<TranscriptLineRow> transcriptLineRows = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add(error(1, "file", "Excel file must not be empty"));
            return new ParseResult(rows, transcriptRows, transcriptLineRows, errors);
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("questions");
            if (sheet == null) {
                errors.add(error(1, "sheet", "Sheet 'questions' was not found"));
                return new ParseResult(rows, transcriptRows, transcriptLineRows, errors);
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headers = readHeaders(headerRow);
            for (String requiredHeader : REQUIRED_HEADERS) {
                if (!headers.containsKey(requiredHeader)) {
                    errors.add(error(1, requiredHeader, "Missing required column " + requiredHeader));
                }
            }
            if (!errors.isEmpty()) {
                return new ParseResult(rows, transcriptRows, transcriptLineRows, errors);
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
                        .questionTextVi(blankToNull(cell(row, headers, "question_text_vi", formatter)))
                        .optionA(blankToNull(cell(row, headers, "option_a", formatter)))
                        .optionAVi(blankToNull(cell(row, headers, "option_a_vi", formatter)))
                        .optionB(blankToNull(cell(row, headers, "option_b", formatter)))
                        .optionBVi(blankToNull(cell(row, headers, "option_b_vi", formatter)))
                        .optionC(blankToNull(cell(row, headers, "option_c", formatter)))
                        .optionCVi(blankToNull(cell(row, headers, "option_c_vi", formatter)))
                        .optionD(blankToNull(cell(row, headers, "option_d", formatter)))
                        .optionDVi(blankToNull(cell(row, headers, "option_d_vi", formatter)))
                        .correct(blankToNull(cell(row, headers, "correct", formatter)))
                        .explanation(blankToNull(cell(row, headers, "explanation", formatter)))
                        .build());
            }

            transcriptRows.addAll(parseTranscriptSheet(workbook, formatter, errors));
            transcriptLineRows.addAll(parseTranscriptLineSheet(workbook, formatter, errors));
        } catch (IOException | RuntimeException exception) {
            errors.add(error(1, "file", "Excel file could not be read"));
        }

        return new ParseResult(rows, transcriptRows, transcriptLineRows, errors);
    }

    private List<TranscriptRow> parseTranscriptSheet(
            Workbook workbook,
            DataFormatter formatter,
            List<ImportErrorResponse> errors
    ) {
        List<TranscriptRow> transcriptRows = new ArrayList<>();
        Sheet sheet = workbook.getSheet("transcripts");
        if (sheet == null) {
            return transcriptRows;
        }

        Map<String, Integer> headers = readHeaders(sheet.getRow(0));
        if (!validateSheetHeaders("transcripts", headers, TRANSCRIPT_HEADERS, errors)) {
            return transcriptRows;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlankRow(row, formatter)) {
                continue;
            }

            transcriptRows.add(TranscriptRow.builder()
                    .rowNumber(row.getRowNum() + 1)
                    .part(parseInteger(cell(row, headers, "part", formatter)))
                    .groupOrder(parseInteger(cell(row, headers, "group_order", formatter)))
                    .transcriptEn(blankToNull(cell(row, headers, "transcript_en", formatter)))
                    .transcriptVi(blankToNull(cell(row, headers, "transcript_vi", formatter)))
                    .build());
        }
        return transcriptRows;
    }

    private List<TranscriptLineRow> parseTranscriptLineSheet(
            Workbook workbook,
            DataFormatter formatter,
            List<ImportErrorResponse> errors
    ) {
        List<TranscriptLineRow> transcriptLineRows = new ArrayList<>();
        Sheet sheet = workbook.getSheet("transcript_lines");
        if (sheet == null) {
            return transcriptLineRows;
        }

        Map<String, Integer> headers = readHeaders(sheet.getRow(0));
        if (!validateSheetHeaders("transcript_lines", headers, TRANSCRIPT_LINE_HEADERS, errors)) {
            return transcriptLineRows;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlankRow(row, formatter)) {
                continue;
            }

            transcriptLineRows.add(TranscriptLineRow.builder()
                    .rowNumber(row.getRowNum() + 1)
                    .part(parseInteger(cell(row, headers, "part", formatter)))
                    .groupOrder(parseInteger(cell(row, headers, "group_order", formatter)))
                    .orderIndex(parseInteger(cell(row, headers, "order_index", formatter)))
                    .speaker(blankToNull(cell(row, headers, "speaker", formatter)))
                    .textEn(blankToNull(cell(row, headers, "text_en", formatter)))
                    .textVi(blankToNull(cell(row, headers, "text_vi", formatter)))
                    .startMs(parseInteger(cell(row, headers, "start_ms", formatter)))
                    .endMs(parseInteger(cell(row, headers, "end_ms", formatter)))
                    .build());
        }
        return transcriptLineRows;
    }

    private boolean validateSheetHeaders(
            String sheetName,
            Map<String, Integer> headers,
            List<String> requiredHeaders,
            List<ImportErrorResponse> errors
    ) {
        boolean valid = true;
        for (String requiredHeader : requiredHeaders) {
            if (!headers.containsKey(requiredHeader)) {
                errors.add(error(1, requiredHeader, "Sheet '" + sheetName + "' is missing required column " + requiredHeader));
                valid = false;
            }
        }
        return valid;
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

    private void validateTranscriptRows(
            List<QuestionRow> questionRows,
            List<TranscriptRow> transcriptRows,
            List<TranscriptLineRow> transcriptLineRows,
            List<ImportErrorResponse> errors
    ) {
        Set<GroupKey> listeningGroups = groupValidRows(questionRows).keySet().stream()
                .filter(key -> key.part() >= 1 && key.part() <= 4)
                .collect(Collectors.toSet());

        Set<GroupKey> transcriptGroups = new HashSet<>();
        for (TranscriptRow row : transcriptRows) {
            validateTranscriptGroup(row.rowNumber(), row.part(), row.groupOrder(), listeningGroups, errors);
            if (row.part() == null || row.groupOrder() == null) {
                continue;
            }
            GroupKey key = new GroupKey(row.part(), row.groupOrder());
            if (!transcriptGroups.add(key)) {
                errors.add(error(row.rowNumber(), "group_order", "Transcript for this part/group_order is duplicated"));
            }
            if (isBlank(row.transcriptEn()) && isBlank(row.transcriptVi())) {
                errors.add(error(row.rowNumber(), "transcript_en", "transcript_en or transcript_vi is required"));
            }
        }

        Map<GroupKey, Set<Integer>> lineOrdersByGroup = new HashMap<>();
        for (TranscriptLineRow row : transcriptLineRows) {
            validateTranscriptGroup(row.rowNumber(), row.part(), row.groupOrder(), listeningGroups, errors);
            if (row.orderIndex() == null) {
                errors.add(error(row.rowNumber(), "order_index", "order_index is required and must be a number"));
            } else if (row.orderIndex() < 0) {
                errors.add(error(row.rowNumber(), "order_index", "order_index must be greater than or equal to 0"));
            }
            if (isBlank(row.textEn())) {
                errors.add(error(row.rowNumber(), "text_en", "text_en is required"));
            }
            if (row.startMs() != null && row.startMs() < 0) {
                errors.add(error(row.rowNumber(), "start_ms", "start_ms must be greater than or equal to 0"));
            }
            if (row.endMs() != null && row.endMs() < 0) {
                errors.add(error(row.rowNumber(), "end_ms", "end_ms must be greater than or equal to 0"));
            }
            if (row.startMs() != null && row.endMs() != null && row.endMs() <= row.startMs()) {
                errors.add(error(row.rowNumber(), "end_ms", "end_ms must be greater than start_ms"));
            }
            if (row.part() == null || row.groupOrder() == null || row.orderIndex() == null) {
                continue;
            }

            GroupKey key = new GroupKey(row.part(), row.groupOrder());
            Set<Integer> lineOrders = lineOrdersByGroup.computeIfAbsent(key, ignored -> new HashSet<>());
            if (!lineOrders.add(row.orderIndex())) {
                errors.add(error(row.rowNumber(), "order_index", "order_index is duplicated for this part/group_order"));
            }
        }
    }

    private void validateTranscriptGroup(
            Integer rowNumber,
            Integer part,
            Integer groupOrder,
            Set<GroupKey> listeningGroups,
            List<ImportErrorResponse> errors
    ) {
        if (part == null) {
            errors.add(error(rowNumber, "part", "part is required and must be a number"));
        }
        if (groupOrder == null) {
            errors.add(error(rowNumber, "group_order", "group_order is required and must be a number"));
        }
        if (part == null || groupOrder == null) {
            return;
        }
        if (part < 1 || part > 4) {
            errors.add(error(rowNumber, "part", "transcripts only support Part 1-4"));
            return;
        }
        if (!listeningGroups.contains(new GroupKey(part, groupOrder))) {
            errors.add(error(rowNumber, "group_order", "No listening question group found for this part/group_order"));
        }
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

    private Map<GroupKey, QuestionGroupAudio> persistRows(Long testId, List<QuestionRow> rows) {
        Map<Integer, TestPart> partsByNumber = testPartRepository.findAllByTestIdOrderByPartNumberAsc(testId).stream()
                .collect(Collectors.toMap(TestPart::getPartNumber, Function.identity()));

        Map<GroupKey, List<QuestionRow>> rowsByGroup = groupValidRows(rows);
        Map<GroupKey, QuestionGroupAudio> audiosByGroup = new HashMap<>();

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
                QuestionGroupAudio questionGroupAudio = questionGroupAudioRepository.save(QuestionGroupAudio.builder()
                        .questionGroup(questionGroup)
                        .mediaAsset(audio)
                        .startMs(0)
                        .endMs(null)
                        .orderIndex(0)
                        .build());
                audiosByGroup.put(key, questionGroupAudio);
            }

            if (key.part() == 6 || key.part() == 7) {
                persistPassageImages(testId, questionGroup, key, groupRows);
            }

            groupRows.stream()
                    .sorted(Comparator.comparing(QuestionRow::questionNumber))
                    .forEach(row -> persistQuestion(questionGroup, row));
        }

        return audiosByGroup;
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

    private void persistTranscripts(
            Map<GroupKey, QuestionGroupAudio> audiosByGroup,
            List<TranscriptRow> transcriptRows,
            List<TranscriptLineRow> transcriptLineRows
    ) {
        for (TranscriptRow row : transcriptRows) {
            QuestionGroupAudio audio = audiosByGroup.get(new GroupKey(row.part(), row.groupOrder()));
            if (audio == null) {
                continue;
            }
            audio.setTranscriptEn(row.transcriptEn());
            audio.setTranscriptVi(row.transcriptVi());
            questionGroupAudioRepository.save(audio);
        }

        for (TranscriptLineRow row : transcriptLineRows) {
            QuestionGroupAudio audio = audiosByGroup.get(new GroupKey(row.part(), row.groupOrder()));
            if (audio == null) {
                continue;
            }
            questionGroupTranscriptLineRepository.save(QuestionGroupTranscriptLine.builder()
                    .questionGroupAudio(audio)
                    .speaker(row.speaker())
                    .textEn(row.textEn())
                    .textVi(row.textVi())
                    .startMs(row.startMs())
                    .endMs(row.endMs())
                    .orderIndex(row.orderIndex())
                    .build());
        }
    }

    private void persistQuestion(QuestionGroup questionGroup, QuestionRow row) {
        Question question = questionRepository.save(Question.builder()
                .questionGroup(questionGroup)
                .questionNumber(row.questionNumber())
                .questionTextEn(row.questionText())
                .questionTextVi(row.questionTextVi())
                .explanationVi(row.explanation())
                .build());

        List<Answer> answers = new ArrayList<>();
        answers.add(answer(question, row.optionA(), row.optionAVi(), "A".equals(row.correct())));
        answers.add(answer(question, row.optionB(), row.optionBVi(), "B".equals(row.correct())));
        answers.add(answer(question, row.optionC(), row.optionCVi(), "C".equals(row.correct())));
        if (row.part() != 2) {
            answers.add(answer(question, row.optionD(), row.optionDVi(), "D".equals(row.correct())));
        }
        answerRepository.saveAll(answers);
    }

    private Answer answer(Question question, String textEn, String textVi, boolean correct) {
        return Answer.builder()
                .question(question)
                .answerTextEn(textEn)
                .answerTextVi(textVi)
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

    private record ParseResult(
            List<QuestionRow> rows,
            List<TranscriptRow> transcriptRows,
            List<TranscriptLineRow> transcriptLineRows,
            List<ImportErrorResponse> errors
    ) {
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
            String questionTextVi,
            String optionA,
            String optionAVi,
            String optionB,
            String optionBVi,
            String optionC,
            String optionCVi,
            String optionD,
            String optionDVi,
            String correct,
            String explanation
    ) {
    }

    @Builder
    private record TranscriptRow(
            Integer rowNumber,
            Integer part,
            Integer groupOrder,
            String transcriptEn,
            String transcriptVi
    ) {
    }

    @Builder
    private record TranscriptLineRow(
            Integer rowNumber,
            Integer part,
            Integer groupOrder,
            Integer orderIndex,
            String speaker,
            String textEn,
            String textVi,
            Integer startMs,
            Integer endMs
    ) {
    }
}
