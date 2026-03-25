create table if not exists iam_access_grant_requests (
    id uuid primary key,
    requested_by_user_id uuid not null references users(id) on delete restrict,
    target_user_id uuid not null references users(id) on delete restrict,
    target_enterprise_id uuid null references enterprises(id) on delete set null,
    permission_code varchar(64) not null,
    enterprise_id uuid null references enterprises(id) on delete set null,
    scope_type varchar(64) null,
    scope_value varchar(255) null,
    resource_type varchar(64) null,
    resource_id uuid null,
    reason varchar(500) not null,
    ticket_no varchar(128) null,
    effective_from timestamptz not null,
    expires_at timestamptz not null,
    status varchar(32) not null,
    decision_comment varchar(500) null,
    approved_by_user_id uuid null references users(id) on delete set null,
    approved_at timestamptz null,
    rejected_by_user_id uuid null references users(id) on delete set null,
    rejected_at timestamptz null,
    approved_grant_id uuid null references iam_access_grants(id) on delete set null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_iam_access_grant_requests_status
    on iam_access_grant_requests(status, created_at desc);
create index if not exists idx_iam_access_grant_requests_target_user
    on iam_access_grant_requests(target_user_id);
create index if not exists idx_iam_access_grant_requests_requested_by
    on iam_access_grant_requests(requested_by_user_id);
