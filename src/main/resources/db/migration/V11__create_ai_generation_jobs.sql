create table if not exists ai_generation_jobs (
    id bigserial primary key,
    test_id bigint not null references tests(id) on delete cascade,
    status varchar(50) not null,
    transcript_enabled boolean not null default true,
    question_translation_enabled boolean not null default true,
    explanation_enabled boolean not null default true,
    overwrite boolean not null default false,
    total_groups integer not null default 0,
    processed_groups integer not null default 0,
    success_groups integer not null default 0,
    failed_groups integer not null default 0,
    estimated_requests integer not null default 0,
    stop_requested boolean not null default false,
    current_group_id bigint references question_groups(id) on delete set null,
    current_step varchar(50),
    started_at timestamp,
    finished_at timestamp,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index if not exists idx_ai_generation_jobs_test_status
    on ai_generation_jobs(test_id, status);

create table if not exists ai_generation_job_items (
    id bigserial primary key,
    job_id bigint not null references ai_generation_jobs(id) on delete cascade,
    question_group_id bigint not null references question_groups(id) on delete cascade,
    part_number integer not null,
    group_order integer not null,
    status varchar(50) not null,
    transcript_status varchar(50) not null default 'pending',
    question_translation_status varchar(50) not null default 'pending',
    explanation_status varchar(50) not null default 'pending',
    error_code varchar(100),
    error_message text,
    started_at timestamp,
    finished_at timestamp,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uq_ai_generation_job_items_group unique(job_id, question_group_id)
);

create index if not exists idx_ai_generation_job_items_job_order
    on ai_generation_job_items(job_id, part_number, group_order);
