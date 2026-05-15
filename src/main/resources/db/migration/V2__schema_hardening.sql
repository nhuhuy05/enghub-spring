-- V2__schema_hardening.sql
-- Hardening constraints, indexes, and seed text fixes for V1 schema

-- =========================================================
-- 1) FIX ENCODING OF SEEDED ROLE / PERMISSION DESCRIPTIONS
-- =========================================================

UPDATE roles
SET description = CASE name
    WHEN 'STUDENT' THEN 'Người học'
    WHEN 'TEACHER' THEN 'Giáo viên'
    WHEN 'ADMIN' THEN 'Quản trị viên'
    ELSE description
END
WHERE name IN ('STUDENT', 'TEACHER', 'ADMIN');

UPDATE permissions
SET description = CASE name
    WHEN 'USER_READ' THEN 'Xem thông tin người dùng'
    WHEN 'USER_MANAGE' THEN 'Quản lý người dùng'
    WHEN 'CONTENT_READ' THEN 'Xem nội dung học tập'
    WHEN 'CONTENT_MANAGE' THEN 'Quản lý nội dung học tập'
    WHEN 'TEST_MANAGE' THEN 'Quản lý bài thi và bài luyện tập'
    WHEN 'SYSTEM_MANAGE' THEN 'Quản lý hệ thống'
    ELSE description
END
WHERE name IN (
    'USER_READ',
    'USER_MANAGE',
    'CONTENT_READ',
    'CONTENT_MANAGE',
    'TEST_MANAGE',
    'SYSTEM_MANAGE'
);

-- =========================================================
-- 2) ENFORCE ANSWER BELONGS TO QUESTION / EXERCISE
-- =========================================================

-- user_answers: selected_answer_id must belong to question_id
CREATE UNIQUE INDEX IF NOT EXISTS uq_answers_id_question_id
    ON answers (id, question_id);

ALTER TABLE user_answers
    ADD CONSTRAINT fk_user_answers_selected_answer_question
    FOREIGN KEY (selected_answer_id, question_id)
    REFERENCES answers (id, question_id)
    ON DELETE NO ACTION;

-- user_grammar_answers: selected_option_id must belong to exercise_id
CREATE UNIQUE INDEX IF NOT EXISTS uq_grammar_exercise_options_id_exercise_id
    ON grammar_exercise_options (id, exercise_id);

ALTER TABLE user_grammar_answers
    ADD CONSTRAINT fk_user_grammar_answers_selected_option_exercise
    FOREIGN KEY (selected_option_id, exercise_id)
    REFERENCES grammar_exercise_options (id, exercise_id)
    ON DELETE NO ACTION;

-- =========================================================
-- 3) PREVENT TEST MISMATCH IN PART AUDIO RANGES
-- =========================================================

CREATE UNIQUE INDEX IF NOT EXISTS uq_test_parts_id_test_id
    ON test_parts (id, test_id);

ALTER TABLE part_audio_ranges
    ADD CONSTRAINT fk_part_audio_ranges_test_part_test
    FOREIGN KEY (test_part_id, test_id)
    REFERENCES test_parts (id, test_id)
    ON DELETE CASCADE;

-- =========================================================
-- 4) QUERY-DRIVEN INDEXES
-- =========================================================

-- attempt history / leaderboard-like lookups
CREATE INDEX IF NOT EXISTS idx_test_attempts_user_status_submitted
    ON test_attempts (user_id, status, submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_test_attempts_test_status_submitted
    ON test_attempts (test_id, status, submitted_at DESC);

-- notification inbox
CREATE INDEX IF NOT EXISTS idx_notifications_user_is_read_scheduled
    ON notifications (user_id, is_read, scheduled_at);

-- vocabulary due review queue
CREATE INDEX IF NOT EXISTS idx_user_vocab_progress_user_next_review
    ON user_vocabulary_progress (user_id, next_review_at);
