update test_attempts
set mode = upper(mode)
where mode in ('practice', 'mock', 'review');

update test_attempts
set status = case status
    when 'in_progress' then 'IN_PROGRESS'
    when 'submitted' then 'SUBMITTED'
    when 'abandoned' then 'ABANDONED'
    else status
end;

alter table test_attempts
    drop constraint if exists chk_test_attempts_mode;

alter table test_attempts
    add constraint chk_test_attempts_mode
    check (mode in ('PRACTICE', 'MOCK', 'REVIEW'));

alter table test_attempts
    drop constraint if exists chk_test_attempts_status;

alter table test_attempts
    add constraint chk_test_attempts_status
    check (status in ('IN_PROGRESS', 'SUBMITTED', 'ABANDONED'));
