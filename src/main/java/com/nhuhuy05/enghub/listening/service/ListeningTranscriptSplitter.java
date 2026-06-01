package com.nhuhuy05.enghub.listening.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ListeningTranscriptSplitter {
    private static final Pattern SPEAKER_BOUNDARY_PATTERN = Pattern.compile(
            "[ \\t]+(?=(?:M|W|Man|Woman|Speaker\\s*\\d+|[A-Z][A-Za-z]*(?:[- ][A-Z]?[A-Za-z]+){0,3}):\\s)"
    );
    private static final Pattern SPEAKER_PREFIX_PATTERN = Pattern.compile(
            "^(M|W|Man|Woman|Speaker\\s*\\d+|[A-Z][A-Za-z]*(?:[- ][A-Z]?[A-Za-z]+){0,3}):\\s*(.*)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TOEIC_DIRECTION_LINE_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:Number\\s+\\d+\\.?|Questions?\\s+\\d+(?:\\s*(?:-|through|to)\\s*\\d+)?\\s+refer\\s+to\\s+the\\s+following\\s+.*\\.?|Look\\s+at\\s+the\\s+picture\\s+.*test\\s+book\\.?)\\s*$"
    );
    private static final Pattern OPTION_LABEL_PATTERN = Pattern.compile("^[A-D]\\.\\s+.*");
    private static final Pattern OPTION_BOUNDARY_PATTERN = Pattern.compile("\\s+(?=[A-D]\\.\\s+)");

    public List<TranscriptSentence> split(String transcriptEn, String transcriptVi) {
        List<TranscriptSentence> englishSentences = splitEnglish(transcriptEn);
        List<String> translations = splitTranslation(transcriptVi);

        List<TranscriptSentence> result = new ArrayList<>();
        for (int i = 0; i < englishSentences.size(); i++) {
            TranscriptSentence sentence = englishSentences.get(i);
            result.add(new TranscriptSentence(
                    sentence.speaker(),
                    sentence.text(),
                    i < translations.size() ? translations.get(i) : null
            ));
        }
        return result;
    }

    private List<TranscriptSentence> splitEnglish(String value) {
        List<TranscriptSentence> result = new ArrayList<>();
        if (isBlank(value)) {
            return result;
        }

        String normalized = normalize(value);
        String speaker = null;
        for (String line : normalized.split("\\n+")) {
            String currentLine = line.trim();
            if (currentLine.isEmpty() || TOEIC_DIRECTION_LINE_PATTERN.matcher(currentLine).matches()) {
                continue;
            }

            var matcher = SPEAKER_PREFIX_PATTERN.matcher(currentLine);
            if (matcher.matches()) {
                speaker = matcher.group(1).trim();
                currentLine = matcher.group(2).trim();
            }
            if (currentLine.isEmpty()) {
                continue;
            }

            for (String sentence : splitSentenceText(currentLine)) {
                if (!sentence.isBlank()) {
                    result.add(new TranscriptSentence(speaker, sentence.trim(), null));
                }
            }
        }
        return result;
    }

    private List<String> splitTranslation(String value) {
        List<String> result = new ArrayList<>();
        if (isBlank(value)) {
            return result;
        }

        String normalized = normalize(value);
        for (String line : normalized.split("\\n+")) {
            String currentLine = line.trim();
            if (currentLine.isEmpty() || TOEIC_DIRECTION_LINE_PATTERN.matcher(currentLine).matches()) {
                continue;
            }

            var matcher = SPEAKER_PREFIX_PATTERN.matcher(currentLine);
            if (matcher.matches()) {
                currentLine = matcher.group(2).trim();
            }
            for (String sentence : splitSentenceText(currentLine)) {
                if (!sentence.isBlank()) {
                    result.add(sentence.trim());
                }
            }
        }
        return result;
    }

    private List<String> splitSentenceText(String text) {
        List<String> result = new ArrayList<>();
        for (String optionPart : OPTION_BOUNDARY_PATTERN.split(text.trim())) {
            String value = optionPart.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (OPTION_LABEL_PATTERN.matcher(value).matches()) {
                result.add(value);
                continue;
            }

            int start = 0;
            for (int i = 0; i < value.length(); i++) {
                char current = value.charAt(i);
                if (current != '.' && current != '?' && current != '!') {
                    continue;
                }
                boolean atEnd = i == value.length() - 1;
                boolean followedBySpace = !atEnd && Character.isWhitespace(value.charAt(i + 1));
                if (!atEnd && !followedBySpace) {
                    continue;
                }

                int end = i + 1;
                result.add(value.substring(start, end).trim());
                start = end;
                while (start < value.length() && Character.isWhitespace(value.charAt(start))) {
                    start++;
                }
            }
            if (start < value.length()) {
                result.add(value.substring(start).trim());
            }
        }
        return result;
    }

    private String normalize(String value) {
        return SPEAKER_BOUNDARY_PATTERN.matcher(value
                        .replace("\r\n", "\n")
                        .replace('\r', '\n')
                        .trim())
                .replaceAll("\n");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record TranscriptSentence(String speaker, String text, String translation) {
    }
}
