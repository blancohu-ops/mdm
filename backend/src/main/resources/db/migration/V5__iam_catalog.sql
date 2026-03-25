create table if not exists iam_role_templates (
    id uuid primary key,
    code varchar(64) not null unique,
    name varchar(128) not null,
    legacy_role_code varchar(32) null unique,
    built_in boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists iam_permissions (
    id uuid primary key,
    code varchar(64) not null unique,
    description varchar(255) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists iam_capabilities (
    id uuid primary key,
    code varchar(64) not null unique,
    description varchar(255) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists iam_data_scopes (
    id uuid primary key,
    code varchar(64) not null unique,
    description varchar(255) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists iam_role_template_permissions (
    id uuid primary key,
    role_template_id uuid not null references iam_role_templates(id) on delete cascade,
    permission_id uuid not null references iam_permissions(id) on delete cascade,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists iam_role_template_capabilities (
    id uuid primary key,
    role_template_id uuid not null references iam_role_templates(id) on delete cascade,
    capability_id uuid not null references iam_capabilities(id) on delete cascade,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists iam_role_template_scopes (
    id uuid primary key,
    role_template_id uuid not null references iam_role_templates(id) on delete cascade,
    data_scope_id uuid not null references iam_data_scopes(id) on delete cascade,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists iam_user_capability_bindings (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    capability_id uuid not null references iam_capabilities(id) on delete cascade,
    source_type varchar(32) not null,
    reason varchar(500) null,
    granted_by uuid null references users(id),
    approved_by uuid null references users(id),
    effective_from timestamptz not null,
    expires_at timestamptz null,
    revoked_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_iam_role_template_permissions_template_permission
    on iam_role_template_permissions(role_template_id, permission_id);
create unique index if not exists uk_iam_role_template_capabilities_template_capability
    on iam_role_template_capabilities(role_template_id, capability_id);
create unique index if not exists uk_iam_role_template_scopes_template_scope
    on iam_role_template_scopes(role_template_id, data_scope_id);

create index if not exists idx_iam_role_templates_legacy_role_code
    on iam_role_templates(legacy_role_code);
create index if not exists idx_iam_user_capability_bindings_user_id
    on iam_user_capability_bindings(user_id);
create index if not exists idx_iam_user_capability_bindings_capability_id
    on iam_user_capability_bindings(capability_id);
create index if not exists idx_iam_user_capability_bindings_effective_from
    on iam_user_capability_bindings(effective_from);
create index if not exists idx_iam_user_capability_bindings_expires_at
    on iam_user_capability_bindings(expires_at);
