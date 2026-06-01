-- V15__enhance_vocabulary_topic_learning.sql
-- Vocabulary phase 1: topic-based learning, richer word content, and review history.

-- =========================================================
-- 1. VOCABULARY CONTENT
-- =========================================================

ALTER TABLE vocabulary
    ADD COLUMN part_of_speech VARCHAR(50),
    ADD COLUMN example_sentence_vi TEXT,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- The existing vocabulary.example_sentence column is the English example.

ALTER TABLE vocabulary
    ADD CONSTRAINT chk_vocabulary_part_of_speech_not_blank
    CHECK (part_of_speech IS NULL OR length(trim(part_of_speech)) > 0);

-- =========================================================
-- 2. VOCABULARY TOPICS
-- =========================================================

ALTER TABLE vocabulary_topics
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- =========================================================
-- 3. USER VOCABULARY PROGRESS
-- =========================================================

ALTER TABLE user_vocabulary_progress
    ADD COLUMN learned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN correct_count INT NOT NULL DEFAULT 0,
    ADD COLUMN mastered BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE user_vocabulary_progress
    ADD CONSTRAINT chk_user_vocabulary_progress_correct_count
    CHECK (correct_count >= 0);

ALTER TABLE user_vocabulary_progress
    ADD CONSTRAINT chk_user_vocabulary_progress_interval_days
    CHECK (interval_days >= 0);

-- =========================================================
-- 4. USER VOCABULARY REVIEW HISTORY
-- =========================================================

CREATE TABLE user_vocabulary_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vocabulary_id BIGINT NOT NULL,
    rating VARCHAR(20) NOT NULL,
    reviewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_vocabulary_reviews_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_user_vocabulary_reviews_vocabulary
        FOREIGN KEY (vocabulary_id) REFERENCES vocabulary(id) ON DELETE CASCADE,

    CONSTRAINT chk_user_vocabulary_reviews_rating
        CHECK (rating IN ('AGAIN', 'HARD', 'GOOD', 'EASY'))
);

-- =========================================================
-- 5. INDEXES
-- =========================================================

CREATE INDEX idx_vocabulary_word_lower
    ON vocabulary (LOWER(word));

CREATE INDEX idx_vocabulary_part_of_speech
    ON vocabulary (part_of_speech);

CREATE INDEX idx_vocabulary_topic_map_vocabulary_id
    ON vocabulary_topic_map (vocabulary_id);

CREATE INDEX idx_user_vocab_progress_user_mastered_due
    ON user_vocabulary_progress (user_id, mastered, next_review_at);

CREATE INDEX idx_user_vocab_reviews_user_reviewed_at
    ON user_vocabulary_reviews (user_id, reviewed_at DESC);

CREATE INDEX idx_user_vocab_reviews_vocab_reviewed_at
    ON user_vocabulary_reviews (vocabulary_id, reviewed_at DESC);
