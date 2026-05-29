-- V9__add_bilingual_questions_and_transcripts.sql
-- Add bilingual question/answer fields and structured listening transcripts.

ALTER TABLE questions
    RENAME COLUMN question_text TO question_text_en;

ALTER TABLE questions
    ADD COLUMN question_text_vi TEXT;

ALTER TABLE questions
    RENAME COLUMN explanation TO explanation_vi;

ALTER TABLE answers
    RENAME COLUMN answer_text TO answer_text_en;

ALTER TABLE answers
    ADD COLUMN answer_text_vi TEXT;

ALTER TABLE question_group_audios
    ADD COLUMN transcript_en TEXT,
    ADD COLUMN transcript_vi TEXT;

CREATE TABLE question_group_transcript_lines (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_group_audio_id BIGINT NOT NULL,
    speaker VARCHAR(100),
    text_en TEXT NOT NULL,
    text_vi TEXT,
    start_ms INT,
    end_ms INT,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_qgtl_audio
        FOREIGN KEY (question_group_audio_id)
        REFERENCES question_group_audios(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_qgtl_audio_order
        UNIQUE (question_group_audio_id, order_index),

    CONSTRAINT chk_qgtl_time
        CHECK (start_ms IS NULL OR end_ms IS NULL OR end_ms > start_ms),

    CONSTRAINT chk_qgtl_start_ms
        CHECK (start_ms IS NULL OR start_ms >= 0),

    CONSTRAINT chk_qgtl_end_ms
        CHECK (end_ms IS NULL OR end_ms >= 0)
);

CREATE INDEX idx_qgtl_audio_id
    ON question_group_transcript_lines(question_group_audio_id);
