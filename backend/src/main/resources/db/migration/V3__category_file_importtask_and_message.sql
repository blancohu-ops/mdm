create table if not exists categories (
    id uuid primary key,
    parent_id uuid null references categories(id) on delete set null,
    name varchar(128) not null,
    code varchar(64) not null,
    status varchar(32) not null,
    sort_order integer not null default 0,
    level_no integer not null default 1,
    path_name varchar(512) not null,
    path_code varchar(512) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists stored_files (
    id uuid primary key,
    business_type varchar(64) not null,
    access_scope varchar(32) not null,
    original_file_name varchar(255) not null,
    stored_file_name varchar(255) not null,
    mime_type varchar(128) null,
    extension varchar(32) null,
    file_size bigint not null,
    storage_path varchar(1000) not null,
    uploaded_by uuid not null references users(id),
    enterprise_id uuid null references enterprises(id),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists import_tasks (
    id uuid primary key,
    enterprise_id uuid not null references enterprises(id),
    source_file_id uuid not null references stored_files(id),
    mode varchar(32) not null,
    status varchar(32) not null,
    total_rows integer not null,
    passed_rows integer not null,
    failed_rows integer not null,
    imported_rows integer not null default 0,
    report_message varchar(500) null,
    created_by uuid not null references users(id),
    confirmed_by uuid null references users(id),
    confirmed_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists import_task_rows (
    id uuid primary key,
    import_task_id uuid not null references import_tasks(id) on delete cascade,
    row_no integer not null,
    product_name varchar(255) not null,
    model varchar(128) not null,
    validation_result varchar(32) not null,
    reason varchar(500) null,
    normalized_payload_json text null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists messages (
    id uuid primary key,
    recipient_user_id uuid not null references users(id) on delete cascade,
    enterprise_id uuid null references enterprises(id) on delete set null,
    type varchar(32) not null,
    status varchar(32) not null,
    title varchar(255) not null,
    summary varchar(500) not null,
    content text null,
    related_resource_type varchar(64) null,
    related_resource_id uuid null,
    sent_at timestamptz not null,
    read_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_categories_code on categories(code);
create index if not exists idx_categories_parent_id on categories(parent_id);
create index if not exists idx_categories_status on categories(status);
create index if not exists idx_categories_sort_order on categories(sort_order);

create index if not exists idx_stored_files_uploaded_by on stored_files(uploaded_by);
create index if not exists idx_stored_files_enterprise_id on stored_files(enterprise_id);
create index if not exists idx_stored_files_business_type on stored_files(business_type);

create index if not exists idx_import_tasks_enterprise_id on import_tasks(enterprise_id);
create index if not exists idx_import_tasks_status on import_tasks(status);
create index if not exists idx_import_task_rows_import_task_id on import_task_rows(import_task_id);
create unique index if not exists uk_import_task_rows_task_row on import_task_rows(import_task_id, row_no);

create index if not exists idx_messages_recipient_user_id on messages(recipient_user_id);
create index if not exists idx_messages_enterprise_id on messages(enterprise_id);
create index if not exists idx_messages_status on messages(status);
create index if not exists idx_messages_type on messages(type);
