package com.nhuhuy05.enghub.listening.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListeningTranscriptSplitterTest {
    ListeningTranscriptSplitter splitter = new ListeningTranscriptSplitter();

    @Test
    void splitKeepsSpeakerAndMapsTranslationByOrder() {
        var sentences = splitter.split(
                "W: Good morning. M: The meeting starts at nine.",
                "Chào buổi sáng. Cuộc họp bắt đầu lúc chín giờ."
        );

        assertThat(sentences).hasSize(2);
        assertThat(sentences.get(0).speaker()).isEqualTo("W");
        assertThat(sentences.get(0).text()).isEqualTo("Good morning.");
        assertThat(sentences.get(0).translation()).isEqualTo("Chào buổi sáng.");
        assertThat(sentences.get(1).speaker()).isEqualTo("M");
        assertThat(sentences.get(1).text()).isEqualTo("The meeting starts at nine.");
    }

    @Test
    void splitDoesNotBreakToeicOptionLabels() {
        var sentences = splitter.split(
                "A. The woman is carrying a tray of food. B. The woman is opening a refrigerator.",
                null
        );

        assertThat(sentences).extracting(ListeningTranscriptSplitter.TranscriptSentence::text)
                .containsExactly(
                        "A. The woman is carrying a tray of food.",
                        "B. The woman is opening a refrigerator."
                );
    }

    @Test
    void splitToleratesMissingTranslationLines() {
        var sentences = splitter.split("First sentence. Second sentence.", "Câu đầu tiên.");

        assertThat(sentences).hasSize(2);
        assertThat(sentences.get(0).translation()).isEqualTo("Câu đầu tiên.");
        assertThat(sentences.get(1).translation()).isNull();
    }
}
