create table if not exists iam_audit_logs (
    id uuid primary key,
    actor_user_id uuid null references users(id) on delete set null,
    actor_role varchar(32) null,
    actor_enterprise_id uuid null references enterprises(id) on delete set null,
    action_code varchar(64) not null,
    target_type varchar(32) not null,
    target_id uuid null,
    target_user_id uuid null references users(id) on delete set null,
    target_enterprise_id uuid null references enterprises(id) on delete set null,
    summary varchar(255) not null,
    detail_json text null,
    request_id varchar(64) null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_iam_audit_logs_action_code
    on iam_audit_logs(action_code);
create index if not exists idx_iam_audit_logs_actor_user_id
    on iam_audit_logs(actor_user_id);
create index if not exists idx_iam_audit_logs_target_user_id
    on iam_audit_logs(target_user_id);
create index if not exists idx_iam_audit_logs_created_at
    on iam_audit_logs(created_at desc);
