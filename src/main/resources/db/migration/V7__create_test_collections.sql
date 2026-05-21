-- V7__create_test_collections.sql
-- Group related tests into collections such as "ETS 2023".

CREATE TABLE test_collections (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_test_collections_name UNIQUE (name)
);

ALTER TABLE tests
    ADD COLUMN collection_id BIGINT,
    ADD COLUMN test_number INT,
    ADD CONSTRAINT fk_tests_collection
        FOREIGN KEY (collection_id) REFERENCES test_collections(id) ON DELETE SET NULL,
    ADD CONSTRAINT uq_tests_collection_number
        UNIQUE (collection_id, test_number),
    ADD CONSTRAINT chk_tests_collection_number_pair
        CHECK (
            (collection_id IS NULL AND test_number IS NULL)
            OR (collection_id IS NOT NULL AND test_number IS NOT NULL)
        ),
    ADD CONSTRAINT chk_tests_test_number
        CHECK (test_number IS NULL OR test_number > 0);

CREATE INDEX idx_tests_collection_id ON tests(collection_id);
CREATE INDEX idx_tests_collection_number ON tests(collection_id, test_number);
