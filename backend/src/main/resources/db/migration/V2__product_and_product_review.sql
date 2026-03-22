create table if not exists products (
    id uuid primary key,
    enterprise_id uuid not null references enterprises(id),
    status varchar(32) not null,
    current_profile_id uuid null,
    working_profile_id uuid null,
    latest_submission_at timestamptz null,
    published_at timestamptz null,
    last_review_comment varchar(500) null,
    last_offline_reason varchar(500) null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists product_profiles (
    id uuid primary key,
    product_id uuid not null references products(id) on delete cascade,
    version_no integer not null,
    name_zh varchar(255) not null,
    name_en varchar(255) null,
    model varchar(128) not null,
    brand varchar(128) null,
    category_path varchar(255) not null,
    main_image_url varchar(500) not null,
    gallery_json text not null,
    summary_zh varchar(1000) not null,
    summary_en varchar(2000) null,
    hs_code varchar(32) not null,
    hs_name varchar(255) null,
    origin_country varchar(128) not null,
    unit varchar(32) not null,
    price_amount numeric(18,2) null,
    currency varchar(16) null,
    packaging varchar(255) null,
    moq varchar(64) null,
    material varchar(255) null,
    size_text varchar(255) null,
    weight_text varchar(255) null,
    color varchar(128) null,
    specs_json text not null,
    certifications_json text not null,
    attachments_json text not null,
    display_public boolean not null,
    sort_order integer null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists product_submission_records (
    id uuid primary key,
    product_id uuid not null references products(id) on delete cascade,
    enterprise_id uuid not null references enterprises(id),
    submission_type varchar(32) not null,
    status varchar(32) not null,
    submission_name varchar(255) not null,
    submission_model varchar(128) not null,
    submission_category varchar(255) not null,
    submission_hs_code varchar(32) not null,
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

create table if not exists product_submission_snapshots (
    id uuid primary key,
    product_id uuid not null references products(id) on delete cascade,
    enterprise_id uuid not null references enterprises(id),
    submission_id uuid not null references product_submission_records(id) on delete cascade,
    payload_json text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_product_profiles_product_version
    on product_profiles(product_id, version_no);
create index if not exists idx_products_enterprise_id on products(enterprise_id);
create index if not exists idx_products_status on products(status);
create index if not exists idx_product_profiles_product_id on product_profiles(product_id);
create index if not exists idx_product_submission_records_product_id
    on product_submission_records(product_id);
create index if not exists idx_product_submission_records_enterprise_id
    on product_submission_records(enterprise_id);
create index if not exists idx_product_submission_records_status
    on product_submission_records(status);
