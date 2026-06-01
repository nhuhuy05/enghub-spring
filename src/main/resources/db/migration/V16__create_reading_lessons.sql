-- V16__create_reading_lessons.sql
-- Reading practice lessons published from TOEIC Part 7 question groups.

CREATE TABLE reading_lessons (
    id BIGSERIAL PRIMARY KEY,
    question_group_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    reading_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    difficulty VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reading_lessons_question_group
        FOREIGN KEY (question_group_id)
        REFERENCES question_groups(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_reading_lessons_question_group
        UNIQUE (question_group_id),

    CONSTRAINT chk_reading_lessons_type
        CHECK (reading_type IN ('SINGLE', 'DOUBLE', 'TRIPLE')),

    CONSTRAINT chk_reading_lessons_status
        CHECK (status IN ('DRAFT', 'PUBLISHED')),

    CONSTRAINT chk_reading_lessons_title_not_blank
        CHECK (length(trim(title)) > 0)
);

CREATE INDEX idx_reading_lessons_status_updated
    ON reading_lessons(status, updated_at DESC);

CREATE INDEX idx_reading_lessons_type_updated
    ON reading_lessons(reading_type, updated_at DESC);

CREATE TABLE reading_vocabulary_hints (
    id BIGSERIAL PRIMARY KEY,
    reading_lesson_id BIGINT NOT NULL,
    passage_id BIGINT,
    word VARCHAR(150) NOT NULL,
    meaning_vi TEXT NOT NULL,
    example_sentence_en TEXT,
    example_sentence_vi TEXT,
    note TEXT,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reading_vocab_lesson
        FOREIGN KEY (reading_lesson_id)
        REFERENCES reading_lessons(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_reading_vocab_passage
        FOREIGN KEY (passage_id)
        REFERENCES question_group_passages(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_reading_vocab_word_not_blank
        CHECK (length(trim(word)) > 0),

    CONSTRAINT chk_reading_vocab_meaning_not_blank
        CHECK (length(trim(meaning_vi)) > 0),

    CONSTRAINT chk_reading_vocab_order
        CHECK (order_index >= 0)
);

CREATE INDEX idx_reading_vocab_lesson_order
    ON reading_vocabulary_hints(reading_lesson_id, order_index, id);

CREATE INDEX idx_reading_vocab_passage
    ON reading_vocabulary_hints(passage_id);
