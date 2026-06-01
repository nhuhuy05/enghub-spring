-- V18__add_reading_lesson_title_vi.sql
-- Vietnamese title for bilingual reading lessons.

ALTER TABLE reading_lessons
    ADD COLUMN title_vi VARCHAR(255);

ALTER TABLE reading_lessons
    ADD CONSTRAINT chk_reading_lessons_title_vi_not_blank
    CHECK (title_vi IS NULL OR length(trim(title_vi)) > 0);
