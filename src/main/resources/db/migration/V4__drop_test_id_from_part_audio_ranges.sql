-- V4__drop_test_id_from_part_audio_ranges.sql
-- Remove redundant test_id from part_audio_ranges and rely on test_part_id -> test_parts(test_id)

-- 1) Drop FK constraints related to test_id and composite test_part/test pair.
ALTER TABLE part_audio_ranges
    DROP CONSTRAINT IF EXISTS fk_part_audio_ranges_test;

ALTER TABLE part_audio_ranges
    DROP CONSTRAINT IF EXISTS fk_part_audio_ranges_test_part_test;

-- 2) Drop index on test_id if it exists.
DROP INDEX IF EXISTS idx_part_audio_ranges_test_id;

-- 3) Drop the redundant column.
ALTER TABLE part_audio_ranges
    DROP COLUMN IF EXISTS test_id;

