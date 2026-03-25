create table if not exists iam_review_domain_assignments (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    domain_type varchar(64) not null,
    enterprise_id uuid not null references enterprises(id) on delete cascade,
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

create unique index if not exists uk_iam_review_domain_assignments_active
    on iam_review_domain_assignments(user_id, domain_type, enterprise_id)
    where revoked_at is null;

create index if not exists idx_iam_review_domain_assignments_user_id
    on iam_review_domain_assignments(user_id);
create index if not exists idx_iam_review_domain_assignments_domain_type
    on iam_review_domain_assignments(domain_type);
create index if not exists idx_iam_review_domain_assignments_enterprise_id
    on iam_review_domain_assignments(enterprise_id);
create index if not exists idx_iam_review_domain_assignments_effective_from
    on iam_review_domain_assignments(effective_from);
create index if not exists idx_iam_review_domain_assignments_expires_at
    on iam_review_domain_assignments(expires_at);
