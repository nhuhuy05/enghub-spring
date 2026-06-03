package com.nhuhuy05.enghub.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class GeminiPromptFactory {
    public String transcriptPrompt(int partNumber) {
        String partRule = switch (partNumber) {
            case 1 -> """
                    - Part 1: transcript contains only choices A-D, one choice per line.
                    - Remove question numbers and test-book directions such as "Number ..." or "Look at the picture ...".
                    - Keep choice labels in transcript, for example: A. ...
                    - Extract A-D into answers; answer_text_* has no A/B/C/D prefix.
                    """;
            case 2 -> """
                    - Part 2: transcript contains the question and choices A-C, each statement on its own line.
                    - Remove question numbers such as "Number 12.".
                    - Do not prefix a single-speaker line with "Speaker:".
                    - Keep choice labels in transcript, for example: A. ...
                    - Extract A-C into answers; answer_text_* has no A/B/C prefix.
                    """;
            default -> """
                    - Part 3/4: use one line per speaker turn.
                    - Remove question-range directions such as "Question 65 through 67 refer to ...".
                    - Keep real speaker names/roles when heard, such as Man, Woman, Captain, Sabine, Amina.
                    - Never use generic labels like Speaker, Speaker 1, or Speaker 2.
                    - If there is only one speaker, do not add a speaker label.
                    - Return an empty answers array.
                    """;
        };

        return """
                Transcribe this TOEIC audio and translate it to Vietnamese.

                Return JSON only:
                {
                  "transcript_en": "...",
                  "transcript_vi": "...",
                  "answers": [
                    {
                      "label": "A",
                      "answer_text_en": "...",
                      "answer_text_vi": "..."
                    }
                  ]
                }

                Rules:
                - Do not invent missing content.
                - Never include TOEIC directions, question numbers, or question-range intro text.
                - Use \\n inside transcript strings for line breaks.
                - Translate transcript_vi line-by-line with the same labels and line count as transcript_en.
                - Use natural Vietnamese.
                - Write [unclear] only where audio is unclear.
                """ + partRule;
    }

    public String questionTranslationPrompt(JsonNode input) {
        return """
                You are helping prepare a TOEIC test for Vietnamese learners.

                Translate question_text_en and answer_text_en into Vietnamese.
                Do not write explanations in this task.
                Do not change correct answers.
                If an English field is blank, return a blank Vietnamese field.
                Do not invent missing text.

                Return JSON only in this exact shape:
                {
                  "questions": [
                    {
                      "question_id": 1,
                      "question_text_vi": "...",
                      "answers": [
                        {"answer_id": 1, "answer_text_vi": "..."}
                      ]
                    }
                  ]
                }

                Input JSON:
                """ + input.toString();
    }

    public String explanationPrompt(JsonNode input) {
        return """
                You are helping prepare TOEIC answer explanations for Vietnamese learners.

                Write explanation_vi for each question in Vietnamese.
                Use the provided correct answer only. Do not change or guess the correct answer.
                Use transcript_en or passage content if provided.
                If images are attached, use them as visual context. The input JSON lists visual assets in the same order as attached files.
                For Part 3/4 graphic questions, use both the transcript and the attached chart/table/map/image.
                For Part 1, use the attached image and answer options.
                For Part 6/7 with passage images, use attached passage images when passage text is missing.
                If there is not enough context, write a short note that the teacher should review the explanation.

                Return JSON only in this exact shape:
                {
                  "questions": [
                    {
                      "question_id": 1,
                      "explanation_vi": "..."
                    }
                  ]
                }

                Input JSON:
                """ + input.toString();
    }

    public String vocabularyTranslationPrompt(JsonNode input) {
        return """
                You are helping build a TOEIC vocabulary list for Vietnamese learners.

                Translate the vocabulary meaning and example sentence into natural Vietnamese.
                Keep the translation concise and suitable for a learner dictionary.
                Do not invent a new English example.
                If example_sentence_en is blank, return a blank example_sentence_vi.

                Return JSON only in this exact shape:
                {
                  "meaning_vi": "...",
                  "example_sentence_vi": "..."
                }

                Input JSON:
                """ + input.toString();
    }

    public String readingTranslationPrompt(JsonNode input) {
        return """
                You are helping prepare TOEIC Part 7 bilingual reading practice for Vietnamese learners.

                Translate the lesson title to Vietnamese as title_vi.
                For each passage, return content_en and content_vi.
                content_en should be the clean English passage text. If content_en is already provided, preserve its meaning and only clean obvious OCR/line-break issues.
                Translate each passage from English to natural Vietnamese.
                If passage images are attached, use them as source/context for the matching visual_asset_order in the input.
                Preserve paragraph and line breaks where helpful for a side-by-side reading UI.
                Do not translate field names, do not add explanations, and do not invent missing content.
                If content_en is blank but a passage image is provided, extract visible English passage text into content_en and translate it into content_vi.
                If both content_en and passage image are missing, return a blank content_vi for that passage.

                Return JSON only in this exact shape:
                {
                  "title_vi": "...",
                  "passages": [
                    {
                      "passage_id": 1,
                      "content_en": "...",
                      "content_vi": "..."
                    }
                  ]
                }

                Input JSON:
                """ + input.toString();
    }

    public String readingVocabularyPrompt(JsonNode input) {
        return """
                You are helping build a TOEIC Part 7 vocabulary hint list for Vietnamese learners.

                Select 5 to 12 useful words or phrases from the passages.
                Prefer business, workplace, travel, notices, scheduling, and email vocabulary.
                Avoid very basic words unless they are part of an important phrase.
                Use concise Vietnamese meanings.
                Include passage_id when the word clearly comes from a passage.
                part_of_speech should be concise, such as noun, verb, adjective, adverb, phrase, phrasal verb.

                Return JSON only in this exact shape:
                {
                  "vocabulary_hints": [
                    {
                      "passage_id": 1,
                      "word": "announce",
                      "part_of_speech": "verb",
                      "meaning_vi": "thong bao"
                    }
                  ]
                }

                Input JSON:
                """ + input.toString();
    }

    public String practiceQuestionChatPrompt(JsonNode context, String userMessage) {
        return """
                You are a TOEIC practice tutor for Vietnamese learners.

                Answer the learner in Vietnamese unless they explicitly ask for English.
                Use only the provided context.
                Do not invent transcript, passage, answer choices, or facts.
                If answered=false, do not reveal the correct answer or identify whether any option is correct.
                If answered=false, give hints and guide the learner to reason.
                If answered=true, explain the selected answer and the correct answer clearly.
                Keep the answer concise and practical for a practice screen.

                Context JSON:
                """ + context.toString() + """

                Learner question:
                """ + emptyIfNull(userMessage);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
