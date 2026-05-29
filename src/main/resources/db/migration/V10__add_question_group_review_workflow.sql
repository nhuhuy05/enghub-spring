-- V10__add_question_group_review_workflow.sql
-- Track review progress for the manual question-group review step.

ALTER TABLE tests
    ADD COLUMN workflow_status VARCHAR(50) NOT NULL DEFAULT 'draft',
    ADD CONSTRAINT chk_tests_workflow_status
        CHECK (workflow_status IN ('draft', 'media_uploaded', 'imported', 'reviewing', 'preview_ready', 'published'));

ALTER TABLE question_groups
    ADD COLUMN review_status VARCHAR(50) NOT NULL DEFAULT 'needs_review',
    ADD COLUMN reviewed_at TIMESTAMP,
    ADD COLUMN reviewed_by BIGINT,
    ADD CONSTRAINT fk_question_groups_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_question_groups_review_status
        CHECK (review_status IN ('needs_review', 'reviewed'));

CREATE INDEX idx_question_groups_review_status
    ON question_groups(review_status);

CREATE INDEX idx_question_groups_test_part_review
    ON question_groups(test_part_id, review_status);
