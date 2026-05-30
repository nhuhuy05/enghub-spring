-- V1__init_schema.sql
-- PostgreSQL schema for EngHub TOEIC backend
-- Generated from the current ERD.
-- Place this file at: src/main/resources/db/migration/V1__init_schema.sql

-- =========================================================
-- 1. USERS / RBAC
-- =========================================================

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,

    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,

    CONSTRAINT uq_permissions_name UNIQUE (name)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    full_name VARCHAR(255),
    avatar_url VARCHAR(500),
    provider VARCHAR(50),
    provider_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_provider_provider_id UNIQUE (provider, provider_id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- =========================================================
-- 2. GRAMMAR
-- =========================================================

CREATE TABLE grammar_topics (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE grammar_lessons (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    example TEXT,

    CONSTRAINT fk_grammar_lessons_topic
        FOREIGN KEY (topic_id) REFERENCES grammar_topics(id) ON DELETE CASCADE
);

CREATE TABLE grammar_exercises (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    explanation TEXT,

    CONSTRAINT fk_grammar_exercises_lesson
        FOREIGN KEY (lesson_id) REFERENCES grammar_lessons(id) ON DELETE CASCADE
);

CREATE TABLE grammar_exercise_options (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL,
    option_text VARCHAR(1000) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_grammar_exercise_options_exercise
        FOREIGN KEY (exercise_id) REFERENCES grammar_exercises(id) ON DELETE CASCADE,
    CONSTRAINT uq_grammar_exercise_options_order
        UNIQUE (exercise_id, order_index)
);

CREATE TABLE user_grammar_answers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    selected_option_id BIGINT,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_grammar_answers_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_grammar_answers_exercise
        FOREIGN KEY (exercise_id) REFERENCES grammar_exercises(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_grammar_answers_selected_option
        FOREIGN KEY (selected_option_id) REFERENCES grammar_exercise_options(id) ON DELETE SET NULL
);

-- =========================================================
-- 3. TESTS / TEST PARTS
-- =========================================================

CREATE TABLE tests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    total_questions INT NOT NULL DEFAULT 0,
    duration_minutes INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tests_total_questions CHECK (total_questions >= 0),
    CONSTRAINT chk_tests_duration_minutes CHECK (duration_minutes >= 0)
);

CREATE TABLE test_parts (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL,
    part_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,

    CONSTRAINT fk_test_parts_test
        FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
    CONSTRAINT uq_test_parts_test_part_number
        UNIQUE (test_id, part_number),
    CONSTRAINT chk_test_parts_part_number
        CHECK (part_number BETWEEN 1 AND 7)
);

-- =========================================================
-- 4. VOCABULARY
-- =========================================================

CREATE TABLE vocabulary_topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    CONSTRAINT uq_vocabulary_topics_name UNIQUE (name)
);

CREATE TABLE vocabulary (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(255) NOT NULL,
    meaning_vi VARCHAR(1000),
    meaning_en VARCHAR(1000),
    pronunciation VARCHAR(255),
    example_sentence TEXT,
    audio_url VARCHAR(500),

    CONSTRAINT uq_vocabulary_word UNIQUE (word)
);

CREATE TABLE vocabulary_topic_map (
    vocabulary_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,

    CONSTRAINT pk_vocabulary_topic_map PRIMARY KEY (vocabulary_id, topic_id),
    CONSTRAINT fk_vocabulary_topic_map_vocabulary
        FOREIGN KEY (vocabulary_id) REFERENCES vocabulary(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocabulary_topic_map_topic
        FOREIGN KEY (topic_id) REFERENCES vocabulary_topics(id) ON DELETE CASCADE
);

CREATE TABLE vocabulary_test_map (
    vocabulary_id BIGINT NOT NULL,
    test_part_id BIGINT NOT NULL,

    CONSTRAINT pk_vocabulary_test_map PRIMARY KEY (vocabulary_id, test_part_id),
    CONSTRAINT fk_vocabulary_test_map_vocabulary
        FOREIGN KEY (vocabulary_id) REFERENCES vocabulary(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocabulary_test_map_test_part
        FOREIGN KEY (test_part_id) REFERENCES test_parts(id) ON DELETE CASCADE
);

CREATE TABLE user_vocabulary_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vocabulary_id BIGINT NOT NULL,
    level INT NOT NULL DEFAULT 0,
    next_review_at TIMESTAMP,
    last_reviewed_at TIMESTAMP,
    review_count INT NOT NULL DEFAULT 0,
    ease_factor NUMERIC(4,2) NOT NULL DEFAULT 2.50,

    CONSTRAINT fk_user_vocabulary_progress_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_vocabulary_progress_vocabulary
        FOREIGN KEY (vocabulary_id) REFERENCES vocabulary(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_vocabulary_progress
        UNIQUE (user_id, vocabulary_id),
    CONSTRAINT chk_user_vocabulary_progress_level CHECK (level >= 0),
    CONSTRAINT chk_user_vocabulary_progress_review_count CHECK (review_count >= 0),
    CONSTRAINT chk_user_vocabulary_progress_ease_factor CHECK (ease_factor > 0)
);

-- =========================================================
-- 5. QUESTION GROUPS / READING / LISTENING
-- =========================================================

CREATE TABLE question_groups (
    id BIGSERIAL PRIMARY KEY,
    test_part_id BIGINT NOT NULL,
    title VARCHAR(255),
    question_count INT NOT NULL DEFAULT 0,
    order_index INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_question_groups_test_part
        FOREIGN KEY (test_part_id) REFERENCES test_parts(id) ON DELETE CASCADE,
    CONSTRAINT uq_question_groups_order
        UNIQUE (test_part_id, order_index),
    CONSTRAINT chk_question_groups_question_count CHECK (question_count >= 0)
);

CREATE TABLE passages (
    id BIGSERIAL PRIMARY KEY,
    question_group_id BIGINT NOT NULL,
    title VARCHAR(255),
    passage_type VARCHAR(100),
    content_format VARCHAR(50),
    content_en TEXT,
    content_vi TEXT,
    vocab_hints TEXT,
    image_url VARCHAR(500),
    order_index INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_passages_question_group
        FOREIGN KEY (question_group_id) REFERENCES question_groups(id) ON DELETE CASCADE,
    CONSTRAINT uq_passages_group_order
        UNIQUE (question_group_id, order_index)
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    question_group_id BIGINT NOT NULL,
    question_number INT NOT NULL,
    question_text TEXT,
    image_url VARCHAR(500),
    explanation TEXT,

    CONSTRAINT fk_questions_question_group
        FOREIGN KEY (question_group_id) REFERENCES question_groups(id) ON DELETE CASCADE,
    CONSTRAINT uq_questions_group_number
        UNIQUE (question_group_id, question_number),
    CONSTRAINT chk_questions_question_number CHECK (question_number > 0)
);

CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    answer_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_answers_question
        FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- =========================================================
-- 6. AUDIO
-- =========================================================

CREATE TABLE audio_files (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL,
    audio_url VARCHAR(500) NOT NULL,
    duration_ms INT,
    title VARCHAR(255),
    audio_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audio_files_test
        FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
    CONSTRAINT chk_audio_files_duration_ms CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

CREATE TABLE part_audio_ranges (
    id BIGSERIAL PRIMARY KEY,
    test_part_id BIGINT NOT NULL,
    audio_file_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    start_ms INT NOT NULL DEFAULT 0,
    end_ms INT,
    order_index INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_part_audio_ranges_test_part
        FOREIGN KEY (test_part_id) REFERENCES test_parts(id) ON DELETE CASCADE,
    CONSTRAINT fk_part_audio_ranges_audio_file
        FOREIGN KEY (audio_file_id) REFERENCES audio_files(id) ON DELETE CASCADE,
    CONSTRAINT fk_part_audio_ranges_test
        FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
    CONSTRAINT uq_part_audio_ranges_order
        UNIQUE (test_part_id, order_index),
    CONSTRAINT chk_part_audio_ranges_time
        CHECK (start_ms >= 0 AND (end_ms IS NULL OR end_ms > start_ms))
);

CREATE TABLE question_group_audio_ranges (
    id BIGSERIAL PRIMARY KEY,
    question_group_id BIGINT NOT NULL,
    audio_file_id BIGINT NOT NULL,
    start_ms INT NOT NULL DEFAULT 0,
    end_ms INT,
    order_index INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_question_group_audio_ranges_group
        FOREIGN KEY (question_group_id) REFERENCES question_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_question_group_audio_ranges_audio_file
        FOREIGN KEY (audio_file_id) REFERENCES audio_files(id) ON DELETE CASCADE,
    CONSTRAINT uq_question_group_audio_ranges_order
        UNIQUE (question_group_id, order_index),
    CONSTRAINT chk_question_group_audio_ranges_time
        CHECK (start_ms >= 0 AND (end_ms IS NULL OR end_ms > start_ms))
);

-- =========================================================
-- 7. TEST ATTEMPTS / USER ANSWERS
-- =========================================================

CREATE TABLE test_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    mode VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'in_progress',
    reading_score INT,
    listening_score INT,
    total_score INT,
    correct_count INT NOT NULL DEFAULT 0,
    total_questions INT NOT NULL DEFAULT 0,
    duration_seconds INT,

    CONSTRAINT fk_test_attempts_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_test_attempts_test
        FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
    CONSTRAINT chk_test_attempts_status
        CHECK (status IN ('in_progress', 'submitted', 'abandoned')),
    CONSTRAINT chk_test_attempts_mode
        CHECK (mode IN ('practice', 'mock', 'review')),
    CONSTRAINT chk_test_attempts_scores
        CHECK (
            (reading_score IS NULL OR reading_score >= 0)
            AND (listening_score IS NULL OR listening_score >= 0)
            AND (total_score IS NULL OR total_score >= 0)
            AND correct_count >= 0
            AND total_questions >= 0
            AND (duration_seconds IS NULL OR duration_seconds >= 0)
        )
);

CREATE TABLE user_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_answer_id BIGINT,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_answers_attempt
        FOREIGN KEY (attempt_id) REFERENCES test_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_answers_question
        FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_answers_selected_answer
        FOREIGN KEY (selected_answer_id) REFERENCES answers(id) ON DELETE SET NULL,
    CONSTRAINT uq_user_answers_attempt_question
        UNIQUE (attempt_id, question_id)
);

-- =========================================================
-- 8. NOTIFICATIONS / PROGRESS
-- =========================================================

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    question_number INT,
    body TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_skill_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vocab_learned INT NOT NULL DEFAULT 0,
    vocab_due_today INT NOT NULL DEFAULT 0,
    grammar_completed INT NOT NULL DEFAULT 0,
    listening_completed INT NOT NULL DEFAULT 0,
    reading_completed INT NOT NULL DEFAULT 0,
    total_study_minutes INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_skill_progress_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_skill_progress_user UNIQUE (user_id),
    CONSTRAINT chk_user_skill_progress_non_negative CHECK (
        vocab_learned >= 0
        AND vocab_due_today >= 0
        AND grammar_completed >= 0
        AND listening_completed >= 0
        AND reading_completed >= 0
        AND total_study_minutes >= 0
    )
);

CREATE TABLE user_daily_streak (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    study_date DATE NOT NULL,
    minutes_studied INT NOT NULL DEFAULT 0,
    vocab_done BOOLEAN NOT NULL DEFAULT FALSE,
    grammar_done BOOLEAN NOT NULL DEFAULT FALSE,
    listening_done BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_user_daily_streak_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_daily_streak_user_date UNIQUE (user_id, study_date),
    CONSTRAINT chk_user_daily_streak_minutes CHECK (minutes_studied >= 0)
);

-- =========================================================
-- 9. INDEXES
-- =========================================================

CREATE INDEX idx_users_email ON users(email);

CREATE INDEX idx_grammar_lessons_topic_id ON grammar_lessons(topic_id);
CREATE INDEX idx_grammar_exercises_lesson_id ON grammar_exercises(lesson_id);
CREATE INDEX idx_grammar_options_exercise_id ON grammar_exercise_options(exercise_id);
CREATE INDEX idx_user_grammar_answers_user_id ON user_grammar_answers(user_id);
CREATE INDEX idx_user_grammar_answers_exercise_id ON user_grammar_answers(exercise_id);

CREATE INDEX idx_test_parts_test_id ON test_parts(test_id);
CREATE INDEX idx_question_groups_test_part_id ON question_groups(test_part_id);
CREATE INDEX idx_passages_question_group_id ON passages(question_group_id);
CREATE INDEX idx_questions_question_group_id ON questions(question_group_id);
CREATE INDEX idx_answers_question_id ON answers(question_id);

CREATE INDEX idx_audio_files_test_id ON audio_files(test_id);
CREATE INDEX idx_part_audio_ranges_test_part_id ON part_audio_ranges(test_part_id);
CREATE INDEX idx_part_audio_ranges_audio_file_id ON part_audio_ranges(audio_file_id);
CREATE INDEX idx_question_group_audio_ranges_group_id ON question_group_audio_ranges(question_group_id);
CREATE INDEX idx_question_group_audio_ranges_audio_file_id ON question_group_audio_ranges(audio_file_id);

CREATE INDEX idx_vocabulary_topic_map_topic_id ON vocabulary_topic_map(topic_id);
CREATE INDEX idx_vocabulary_test_map_test_part_id ON vocabulary_test_map(test_part_id);
CREATE INDEX idx_user_vocabulary_progress_user_id ON user_vocabulary_progress(user_id);
CREATE INDEX idx_user_vocabulary_progress_vocabulary_id ON user_vocabulary_progress(vocabulary_id);
CREATE INDEX idx_user_vocabulary_progress_next_review_at ON user_vocabulary_progress(next_review_at);

CREATE INDEX idx_test_attempts_user_id ON test_attempts(user_id);
CREATE INDEX idx_test_attempts_test_id ON test_attempts(test_id);
CREATE INDEX idx_test_attempts_status ON test_attempts(status);
CREATE INDEX idx_user_answers_attempt_id ON user_answers(attempt_id);
CREATE INDEX idx_user_answers_question_id ON user_answers(question_id);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_scheduled_at ON notifications(scheduled_at);

CREATE INDEX idx_user_daily_streak_user_id ON user_daily_streak(user_id);
CREATE INDEX idx_user_daily_streak_study_date ON user_daily_streak(study_date);

-- =========================================================
-- 10. SEED BASIC ROLES / PERMISSIONS
-- =========================================================

INSERT INTO roles (name, description) VALUES
('STUDENT', 'Người học'),
('TEACHER', 'Giáo viên'),
('ADMIN', 'Quản trị viên');

INSERT INTO permissions (name, description) VALUES
('USER_READ', 'Xem thông tin người dùng'),
('USER_MANAGE', 'Quản lý người dùng'),
('CONTENT_READ', 'Xem nội dung học tập'),
('CONTENT_MANAGE', 'Quản lý nội dung học tập'),
('TEST_MANAGE', 'Quản lý bài thi và bài luyện tập'),
('SYSTEM_MANAGE', 'Quản lý hệ thống');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('CONTENT_READ')
WHERE r.name = 'STUDENT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('CONTENT_READ', 'CONTENT_MANAGE', 'TEST_MANAGE')
WHERE r.name = 'TEACHER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'USER_READ',
    'USER_MANAGE',
    'CONTENT_READ',
    'CONTENT_MANAGE',
    'TEST_MANAGE',
    'SYSTEM_MANAGE'
)
WHERE r.name = 'ADMIN';
