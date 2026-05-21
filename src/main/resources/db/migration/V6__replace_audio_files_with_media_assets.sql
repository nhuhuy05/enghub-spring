-- V6__replace_audio_files_with_media_assets.sql
-- Replace direct audio/image columns with media_assets references.

CREATE TABLE media_assets (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    test_id BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    duration_ms INT,
    original_filename VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ma_test
        FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
    CONSTRAINT uq_ma_label
        UNIQUE (test_id, label, media_type),
    CONSTRAINT chk_media_assets_media_type
        CHECK (media_type IN ('image', 'audio')),
    CONSTRAINT chk_media_assets_duration_ms
        CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

CREATE INDEX idx_media_assets_test_id ON media_assets(test_id);
CREATE INDEX idx_media_assets_type ON media_assets(media_type);

INSERT INTO media_assets (
    test_id,
    label,
    media_type,
    cloudinary_public_id,
    url,
    duration_ms,
    original_filename,
    created_at
)
SELECT
    test_id,
    'audio-' || id,
    'audio',
    COALESCE(NULLIF(audio_type, ''), 'legacy-audio-' || id),
    audio_url,
    duration_ms,
    title,
    created_at
FROM audio_files;

ALTER TABLE question_group_audio_ranges
    ADD COLUMN media_asset_id BIGINT;

UPDATE question_group_audio_ranges qgar
SET media_asset_id = ma.id
FROM audio_files af
JOIN media_assets ma
    ON ma.test_id = af.test_id
    AND ma.media_type = 'audio'
    AND ma.label = 'audio-' || af.id
WHERE qgar.audio_file_id = af.id;

ALTER TABLE question_group_audio_ranges
    ALTER COLUMN media_asset_id SET NOT NULL;

ALTER TABLE question_group_audio_ranges
    DROP CONSTRAINT IF EXISTS fk_question_group_audio_ranges_audio_file;

DROP INDEX IF EXISTS idx_question_group_audio_ranges_audio_file_id;

ALTER TABLE question_group_audio_ranges
    DROP COLUMN audio_file_id,
    ADD CONSTRAINT fk_qgar_media
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id) ON DELETE CASCADE;

CREATE INDEX idx_question_group_audio_ranges_media_asset_id
    ON question_group_audio_ranges(media_asset_id);

DROP TABLE IF EXISTS part_audio_ranges;
DROP TABLE IF EXISTS audio_files;

ALTER TABLE question_groups
    DROP CONSTRAINT IF EXISTS chk_question_groups_question_count,
    DROP COLUMN IF EXISTS question_count,
    ADD COLUMN media_asset_id BIGINT,
    ADD CONSTRAINT fk_qg_media
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;

INSERT INTO media_assets (
    test_id,
    label,
    media_type,
    cloudinary_public_id,
    url
)
SELECT
    tp.test_id,
    'passage-' || p.id,
    'image',
    'legacy-passage-' || p.id,
    p.image_url
FROM passages p
JOIN question_groups qg ON qg.id = p.question_group_id
JOIN test_parts tp ON tp.id = qg.test_part_id
WHERE p.image_url IS NOT NULL;

ALTER TABLE passages
    ADD COLUMN media_asset_id BIGINT,
    ADD CONSTRAINT fk_pass_media
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;

UPDATE passages p
SET media_asset_id = ma.id
FROM question_groups qg
JOIN test_parts tp ON tp.id = qg.test_part_id
JOIN media_assets ma
    ON ma.test_id = tp.test_id
    AND ma.media_type = 'image'
WHERE qg.id = p.question_group_id
  AND ma.label = 'passage-' || p.id
  AND p.image_url IS NOT NULL;

ALTER TABLE passages
    DROP COLUMN IF EXISTS image_url;

INSERT INTO media_assets (
    test_id,
    label,
    media_type,
    cloudinary_public_id,
    url
)
SELECT
    tp.test_id,
    'question-' || q.id,
    'image',
    'legacy-question-' || q.id,
    q.image_url
FROM questions q
JOIN question_groups qg ON qg.id = q.question_group_id
JOIN test_parts tp ON tp.id = qg.test_part_id
WHERE q.image_url IS NOT NULL;

UPDATE question_groups qg
SET media_asset_id = picked.media_asset_id
FROM (
    SELECT DISTINCT ON (q.question_group_id)
        q.question_group_id,
        ma.id AS media_asset_id
    FROM questions q
    JOIN question_groups qg2 ON qg2.id = q.question_group_id
    JOIN test_parts tp ON tp.id = qg2.test_part_id
    JOIN media_assets ma
        ON ma.test_id = tp.test_id
        AND ma.media_type = 'image'
        AND ma.label = 'question-' || q.id
    WHERE q.image_url IS NOT NULL
    ORDER BY q.question_group_id, q.question_number
) picked
WHERE qg.id = picked.question_group_id
  AND qg.media_asset_id IS NULL;

ALTER TABLE questions
    DROP COLUMN IF EXISTS image_url;

ALTER TABLE test_attempts
    DROP COLUMN IF EXISTS correct_count,
    DROP COLUMN IF EXISTS total_questions;

ALTER TABLE user_vocabulary_progress
    ADD COLUMN interval_days INT NOT NULL DEFAULT 0;

ALTER TABLE tests
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
