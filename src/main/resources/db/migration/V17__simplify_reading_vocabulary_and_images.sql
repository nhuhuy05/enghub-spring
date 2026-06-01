-- V17__simplify_reading_vocabulary_and_images.sql
-- Reading vocabulary hints only need word, part of speech, and Vietnamese meaning.

ALTER TABLE reading_vocabulary_hints
    ADD COLUMN part_of_speech VARCHAR(50);

ALTER TABLE reading_vocabulary_hints
    DROP COLUMN IF EXISTS example_sentence_en,
    DROP COLUMN IF EXISTS example_sentence_vi,
    DROP COLUMN IF EXISTS note;

ALTER TABLE reading_vocabulary_hints
    ADD CONSTRAINT chk_reading_vocab_part_of_speech_not_blank
    CHECK (part_of_speech IS NULL OR length(trim(part_of_speech)) > 0);
