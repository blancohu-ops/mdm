alter table users
    add column if not exists service_provider_id uuid null;

create index if not exists idx_users_service_provider_id
    on users(service_provider_id);

create table if not exists service_provider_applications (
    id uuid primary key,
    company_name varchar(255) not null,
    contact_name varchar(128) not null,
    phone varchar(32) not null,
    email varchar(128) not null,
    website varchar(255) null,
    service_scope varchar(255) not null,
    summary text not null,
    logo_url varchar(500) null,
    license_file_name varchar(255) null,
    license_preview_url varchar(500) null,
    status varchar(32) not null,
    review_comment varchar(500) null,
    reviewed_by uuid null references users(id),
    reviewed_at timestamptz null,
    approved_provider_id uuid null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_service_provider_applications_status
    on service_provider_applications(status);

create table if not exists service_providers (
    id uuid primary key,
    name varchar(255) not null,
    status varchar(32) not null,
    current_profile_id uuid null,
    working_profile_id uuid null,
    latest_application_id uuid null references service_provider_applications(id),
    joined_at date null,
    last_review_comment varchar(500) null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_service_providers_status
    on service_providers(status);

create table if not exists service_provider_profiles (
    id uuid primary key,
    service_provider_id uuid not null references service_providers(id) on delete cascade,
    version_no integer not null,
    company_name varchar(255) not null,
    short_name varchar(128) null,
    service_scope varchar(255) not null,
    summary text not null,
    website varchar(255) null,
    logo_url varchar(500) null,
    license_file_name varchar(255) null,
    license_preview_url varchar(500) null,
    contact_name varchar(128) not null,
    contact_phone varchar(32) not null,
    contact_email varchar(128) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_service_provider_profiles_version
    on service_provider_profiles(service_provider_id, version_no);

alter table service_providers
    add constraint fk_service_providers_current_profile
        foreign key (current_profile_id) references service_provider_profiles(id);

alter table service_providers
    add constraint fk_service_providers_working_profile
        foreign key (working_profile_id) references service_provider_profiles(id);

create table if not exists provider_activation_tokens (
    id uuid primary key,
    service_provider_id uuid not null references service_providers(id) on delete cascade,
    account varchar(128) not null,
    phone varchar(32) not null,
    email varchar(128) not null,
    token_value varchar(128) not null,
    expires_at timestamptz not null,
    used_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_provider_activation_tokens_token_value
    on provider_activation_tokens(token_value);

create index if not exists idx_provider_activation_tokens_provider_id
    on provider_activation_tokens(service_provider_id);

create table if not exists service_categories (
    id uuid primary key,
    name varchar(128) not null,
    code varchar(64) not null,
    description varchar(500) null,
    sort_order integer not null default 0,
    status varchar(32) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_service_categories_code
    on service_categories(code);

create table if not exists services (
    id uuid primary key,
    service_provider_id uuid null references service_providers(id),
    category_id uuid not null references service_categories(id),
    operator_type varchar(32) not null,
    status varchar(32) not null,
    title varchar(255) not null,
    summary varchar(500) not null,
    description text not null,
    cover_image_url varchar(500) null,
    deliverable_summary varchar(500) null,
    requires_payment boolean not null default true,
    published_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_services_provider_id
    on services(service_provider_id);

create index if not exists idx_services_category_id
    on services(category_id);

create index if not exists idx_services_status
    on services(status);

create table if not exists service_offers (
    id uuid primary key,
    service_id uuid not null references services(id) on delete cascade,
    name varchar(255) not null,
    target_resource_type varchar(32) not null,
    billing_mode varchar(32) not null,
    price_amount numeric(18,2) not null,
    currency varchar(16) not null,
    unit_label varchar(64) not null,
    validity_days integer null,
    highlight_text varchar(255) null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_service_offers_service_id
    on service_offers(service_id);

create index if not exists idx_service_offers_target_resource_type
    on service_offers(target_resource_type);

create table if not exists service_orders (
    id uuid primary key,
    order_no varchar(64) not null,
    enterprise_id uuid not null references enterprises(id) on delete cascade,
    product_id uuid null references products(id),
    service_id uuid not null references services(id),
    offer_id uuid not null references service_offers(id),
    service_provider_id uuid null references service_providers(id),
    status varchar(32) not null,
    payment_status varchar(32) not null,
    amount numeric(18,2) not null,
    currency varchar(16) not null,
    customer_note varchar(500) null,
    created_by_user_id uuid not null references users(id),
    latest_fulfillment_at timestamptz null,
    completed_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_service_orders_order_no
    on service_orders(order_no);

create index if not exists idx_service_orders_enterprise_id
    on service_orders(enterprise_id);

create index if not exists idx_service_orders_provider_id
    on service_orders(service_provider_id);

create index if not exists idx_service_orders_status
    on service_orders(status);

create table if not exists payment_records (
    id uuid primary key,
    service_order_id uuid not null references service_orders(id) on delete cascade,
    amount numeric(18,2) not null,
    currency varchar(16) not null,
    payment_method varchar(32) not null,
    status varchar(32) not null,
    evidence_file_url varchar(500) null,
    note varchar(500) null,
    submitted_at timestamptz null,
    confirmed_by uuid null references users(id),
    confirmed_at timestamptz null,
    confirmed_note varchar(500) null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_payment_records_order_id
    on payment_records(service_order_id);

create index if not exists idx_payment_records_status
    on payment_records(status);

create table if not exists service_fulfillments (
    id uuid primary key,
    service_order_id uuid not null references service_orders(id) on delete cascade,
    service_provider_id uuid null references service_providers(id),
    milestone_code varchar(64) not null,
    milestone_name varchar(255) not null,
    status varchar(32) not null,
    detail varchar(1000) null,
    due_at timestamptz null,
    completed_at timestamptz null,
    updated_by_user_id uuid null references users(id),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_service_fulfillments_order_id
    on service_fulfillments(service_order_id);

create index if not exists idx_service_fulfillments_provider_id
    on service_fulfillments(service_provider_id);

create table if not exists delivery_artifacts (
    id uuid primary key,
    service_order_id uuid not null references service_orders(id) on delete cascade,
    service_fulfillment_id uuid null references service_fulfillments(id) on delete set null,
    file_name varchar(255) not null,
    file_url varchar(500) not null,
    artifact_type varchar(64) not null,
    note varchar(500) null,
    visible_to_enterprise boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_delivery_artifacts_order_id
    on delivery_artifacts(service_order_id);

