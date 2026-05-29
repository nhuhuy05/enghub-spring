-- V8__normalize_question_group_media.sql
-- Normalize question-group media usage into explicit tables:
-- media_assets stores uploaded files, while question_group_* tables store
-- how each file is used by a question group.

-- =========================================================
-- 1. AUDIO: question_group_audio_ranges -> question_group_audios
-- =========================================================

DO $$
BEGIN
    IF to_regclass('public.question_group_audio_ranges') IS NOT NULL
       AND to_regclass('public.question_group_audios') IS NULL THEN
        ALTER TABLE question_group_audio_ranges RENAME TO question_group_audios;
    END IF;
END $$;

ALTER INDEX IF EXISTS idx_question_group_audio_ranges_group_id
    RENAME TO idx_question_group_audios_group_id;

ALTER INDEX IF EXISTS idx_question_group_audio_ranges_media_asset_id
    RENAME TO idx_question_group_audios_media_asset_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_question_group_audio_ranges_group') THEN
        ALTER TABLE question_group_audios
            RENAME CONSTRAINT fk_question_group_audio_ranges_group
            TO fk_question_group_audios_group;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_qgar_media') THEN
        ALTER TABLE question_group_audios
            RENAME CONSTRAINT fk_qgar_media
            TO fk_question_group_audios_media;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_question_group_audio_ranges_order') THEN
        ALTER TABLE question_group_audios
            RENAME CONSTRAINT uq_question_group_audio_ranges_order
            TO uq_question_group_audios_order;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_question_group_audio_ranges_time') THEN
        ALTER TABLE question_group_audios
            RENAME CONSTRAINT chk_question_group_audio_ranges_time
            TO chk_question_group_audios_time;
    END IF;
END $$;


-- =========================================================
-- 2. PASSAGES: passages -> question_group_passages
-- =========================================================

DO $$
BEGIN
    IF to_regclass('public.passages') IS NOT NULL
       AND to_regclass('public.question_group_passages') IS NULL THEN
        ALTER TABLE passages RENAME TO question_group_passages;
    END IF;
END $$;

ALTER INDEX IF EXISTS idx_passages_question_group_id
    RENAME TO idx_question_group_passages_group_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_passages_question_group') THEN
        ALTER TABLE question_group_passages
            RENAME CONSTRAINT fk_passages_question_group
            TO fk_question_group_passages_group;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pass_media') THEN
        ALTER TABLE question_group_passages
            RENAME CONSTRAINT fk_pass_media
            TO fk_question_group_passages_media;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_passages_group_order') THEN
        ALTER TABLE question_group_passages
            RENAME CONSTRAINT uq_passages_group_order
            TO uq_question_group_passages_order;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_question_group_passages_media_asset_id
    ON question_group_passages(media_asset_id);


-- =========================================================
-- 3. IMAGES: direct group images for Part 1 and Part 3/4 graphics
-- =========================================================

CREATE TABLE IF NOT EXISTS question_group_images (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_group_id BIGINT NOT NULL,
    media_asset_id BIGINT NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_question_group_images_group
        FOREIGN KEY (question_group_id)
        REFERENCES question_groups(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_question_group_images_media
        FOREIGN KEY (media_asset_id)
        REFERENCES media_assets(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_question_group_images_order
        UNIQUE (question_group_id, order_index),

    CONSTRAINT uq_question_group_images_media
        UNIQUE (question_group_id, media_asset_id),

    CONSTRAINT chk_question_group_images_order
        CHECK (order_index >= 0)
);

CREATE INDEX IF NOT EXISTS idx_question_group_images_group_id
    ON question_group_images(question_group_id);

CREATE INDEX IF NOT EXISTS idx_question_group_images_media_asset_id
    ON question_group_images(media_asset_id);


-- =========================================================
-- 4. BACKFILL LEGACY question_groups.media_asset_id
-- =========================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'question_groups'
          AND column_name = 'media_asset_id'
    ) THEN
        INSERT INTO question_group_images (
            question_group_id,
            media_asset_id,
            order_index
        )
        SELECT
            qg.id,
            qg.media_asset_id,
            0
        FROM question_groups qg
        JOIN media_assets ma ON ma.id = qg.media_asset_id
        WHERE qg.media_asset_id IS NOT NULL
          AND ma.media_type = 'image'
        ON CONFLICT (question_group_id, media_asset_id) DO NOTHING;
    END IF;
END $$;


-- =========================================================
-- 5. DROP LEGACY GROUP MEDIA COLUMN
-- =========================================================

ALTER TABLE question_groups
    DROP CONSTRAINT IF EXISTS fk_qg_media;

ALTER TABLE question_groups
    DROP COLUMN IF EXISTS media_asset_id;
