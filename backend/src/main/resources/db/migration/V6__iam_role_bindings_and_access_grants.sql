alter table users
    add column if not exists authz_version integer not null default 0;

create table if not exists iam_user_role_bindings (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    role_template_id uuid not null references iam_role_templates(id) on delete cascade,
    enterprise_id uuid null references enterprises(id) on delete cascade,
    source_type varchar(32) not null,
    is_primary boolean not null,
    granted_by uuid null references users(id),
    reason varchar(500) null,
    effective_from timestamptz not null,
    expires_at timestamptz null,
    revoked_at timestamptz null,
    revoked_by uuid null references users(id),
    revoked_reason varchar(500) null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists iam_access_grants (
    id uuid primary key,
    principal_type varchar(32) not null,
    principal_id uuid not null,
    permission_code varchar(64) not null,
    enterprise_id uuid null references enterprises(id) on delete cascade,
    scope_type varchar(64) null,
    scope_value varchar(255) null,
    resource_type varchar(64) null,
    resource_id uuid null,
    grant_type varchar(32) not null,
    effect varchar(16) not null,
    granted_by uuid null references users(id),
    approved_by uuid null references users(id),
    reason varchar(500) null,
    ticket_no varchar(128) null,
    effective_from timestamptz not null,
    expires_at timestamptz null,
    revoked_at timestamptz null,
    revoked_by uuid null references users(id),
    revoked_reason varchar(500) null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_iam_user_role_bindings_active
    on iam_user_role_bindings(
        user_id,
        role_template_id,
        coalesce(enterprise_id, '00000000-0000-0000-0000-000000000000'::uuid),
        source_type
    )
    where revoked_at is null;

create index if not exists idx_iam_user_role_bindings_user_id
    on iam_user_role_bindings(user_id);
create index if not exists idx_iam_user_role_bindings_role_template_id
    on iam_user_role_bindings(role_template_id);
create index if not exists idx_iam_user_role_bindings_enterprise_id
    on iam_user_role_bindings(enterprise_id);
create index if not exists idx_iam_user_role_bindings_effective_from
    on iam_user_role_bindings(effective_from);
create index if not exists idx_iam_user_role_bindings_expires_at
    on iam_user_role_bindings(expires_at);

create index if not exists idx_iam_access_grants_principal
    on iam_access_grants(principal_type, principal_id);
create index if not exists idx_iam_access_grants_permission_code
    on iam_access_grants(permission_code);
create index if not exists idx_iam_access_grants_enterprise_id
    on iam_access_grants(enterprise_id);
create index if not exists idx_iam_access_grants_effective_from
    on iam_access_grants(effective_from);
create index if not exists idx_iam_access_grants_expires_at
    on iam_access_grants(expires_at);
