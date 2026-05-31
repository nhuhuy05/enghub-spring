alter table test_attempts
    add column if not exists selected_part_numbers varchar(50);
