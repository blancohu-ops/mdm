create table if not exists users (
    id uuid primary key,
    account varchar(128) not null unique,
    phone varchar(32) not null unique,
    email varchar(128) not null unique,
    password_hash varchar(255) not null,
    role varchar(32) not null,
    status varchar(32) not null,
    enterprise_id uuid null,
    display_name varchar(128) not null,
    organization varchar(255) not null,
    last_login_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists refresh_tokens (
    id uuid primary key,
    user_id uuid not null references users(id),
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists login_logs (
    id uuid primary key,
    user_id uuid null references users(id),
    account varchar(128) not null,
    role varchar(32) null,
    enterprise_id uuid null,
    result varchar(16) not null,
    client_ip varchar(64) null,
    user_agent varchar(512) null,
    failure_reason varchar(255) null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists sms_codes (
    id uuid primary key,
    phone varchar(32) not null,
    purpose varchar(32) not null,
    code varchar(16) not null,
    expires_at timestamptz not null,
    used_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists enterprises (
    id uuid primary key,
    name varchar(255) not null,
    status varchar(32) not null,
    current_profile_id uuid null,
    working_profile_id uuid null,
    latest_submission_at timestamptz null,
    joined_at date null,
    last_review_comment varchar(500) null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists enterprise_profiles (
    id uuid primary key,
    enterprise_id uuid not null references enterprises(id),
    version_no integer not null,
    name varchar(255) not null,
    short_name varchar(128) null,
    social_credit_code varchar(64) not null,
    company_type varchar(64) not null,
    industry varchar(64) not null,
    main_categories varchar(512) not null,
    province varchar(64) not null,
    city varchar(64) not null,
    district varchar(64) not null,
    address varchar(255) not null,
    summary varchar(1000) not null,
    website varchar(255) null,
    logo_url varchar(500) null,
    license_file_name varchar(255) not null,
    license_preview_url varchar(500) null,
    contact_name varchar(128) not null,
    contact_title varchar(128) null,
    contact_phone varchar(32) not null,
    contact_email varchar(128) not null,
    public_contact_name boolean not null,
    public_contact_phone boolean not null,
    public_contact_email boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists enterprise_submission_records (
    id uuid primary key,
    enterprise_id uuid not null references enterprises(id),
    submission_type varchar(32) not null,
    status varchar(32) not null,
    submission_name varchar(255) not null,
    submission_social_credit_code varchar(64) not null,
    submission_industry varchar(64) not null,
    submission_contact_name varchar(128) not null,
    submission_contact_phone varchar(32) not null,
    submitted_by uuid not null references users(id),
    submitted_at timestamptz not null,
    reviewed_by uuid null references users(id),
    reviewed_at timestamptz null,
    review_comment varchar(500) null,
    internal_note varchar(500) null,
    snapshot_id uuid null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists enterprise_submission_snapshots (
    id uuid primary key,
    enterprise_id uuid not null references enterprises(id),
    submission_id uuid not null references enterprise_submission_records(id),
    payload_json text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_users_enterprise_id on users(enterprise_id);
create index if not exists idx_refresh_tokens_user_id on refresh_tokens(user_id);
create index if not exists idx_sms_codes_phone_purpose on sms_codes(phone, purpose);
create index if not exists idx_enterprises_status on enterprises(status);
create index if not exists idx_enterprise_profiles_enterprise_id on enterprise_profiles(enterprise_id);
create index if not exists idx_enterprise_submission_records_enterprise_id on enterprise_submission_records(enterprise_id);
create index if not exists idx_enterprise_submission_records_status on enterprise_submission_records(status);
