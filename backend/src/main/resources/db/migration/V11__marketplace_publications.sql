create table if not exists marketplace_publications (
    id uuid primary key,
    service_order_id uuid not null references service_orders(id) on delete cascade,
    enterprise_id uuid not null references enterprises(id) on delete cascade,
    product_id uuid null references products(id),
    service_id uuid not null references services(id),
    offer_id uuid not null references service_offers(id),
    service_provider_id uuid null references service_providers(id),
    publication_type varchar(32) not null,
    target_resource_type varchar(32) not null,
    status varchar(32) not null,
    activation_note varchar(500) null,
    starts_at timestamptz not null,
    expires_at timestamptz null,
    activated_at timestamptz not null,
    deactivated_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists uk_marketplace_publications_service_order_id
    on marketplace_publications(service_order_id);

create index if not exists idx_marketplace_publications_enterprise_id
    on marketplace_publications(enterprise_id);

create index if not exists idx_marketplace_publications_product_id
    on marketplace_publications(product_id);

create index if not exists idx_marketplace_publications_status
    on marketplace_publications(status);

insert into marketplace_publications (
    id,
    service_order_id,
    enterprise_id,
    product_id,
    service_id,
    offer_id,
    service_provider_id,
    publication_type,
    target_resource_type,
    status,
    activation_note,
    starts_at,
    expires_at,
    activated_at,
    deactivated_at,
    created_at,
    updated_at
)
select
    (
        substring(md5(random()::text || clock_timestamp()::text), 1, 8)
        || '-'
        || substring(md5(random()::text || clock_timestamp()::text), 1, 4)
        || '-'
        || substring(md5(random()::text || clock_timestamp()::text), 1, 4)
        || '-'
        || substring(md5(random()::text || clock_timestamp()::text), 1, 4)
        || '-'
        || substring(md5(random()::text || clock_timestamp()::text), 1, 12)
    )::uuid,
    so.id,
    so.enterprise_id,
    so.product_id,
    so.service_id,
    so.offer_id,
    so.service_provider_id,
    case
        when soff.target_resource_type = 'PRODUCT' then 'PRODUCT_PROMOTION'
        else 'ENTERPRISE_SHOWCASE'
    end,
    soff.target_resource_type,
    case
        when soff.validity_days is not null
             and coalesce(payment.confirmed_at, so.updated_at, now())
                 + (soff.validity_days || ' days')::interval <= now()
            then 'EXPIRED'
        else 'ACTIVE'
    end,
    'Backfilled from confirmed promotion order',
    coalesce(payment.confirmed_at, so.updated_at, now()),
    case
        when soff.validity_days is not null
            then coalesce(payment.confirmed_at, so.updated_at, now())
                + (soff.validity_days || ' days')::interval
        else null
    end,
    coalesce(payment.confirmed_at, so.updated_at, now()),
    case
        when soff.validity_days is not null
             and coalesce(payment.confirmed_at, so.updated_at, now())
                 + (soff.validity_days || ' days')::interval <= now()
            then coalesce(payment.confirmed_at, so.updated_at, now())
                + (soff.validity_days || ' days')::interval
        else null
    end,
    coalesce(payment.confirmed_at, so.updated_at, now()),
    now()
from service_orders so
join services s on s.id = so.service_id
join service_categories sc on sc.id = s.category_id
join service_offers soff on soff.id = so.offer_id
left join lateral (
    select pr.confirmed_at
    from payment_records pr
    where pr.service_order_id = so.id
      and pr.status = 'CONFIRMED'
    order by coalesce(pr.confirmed_at, pr.updated_at, pr.created_at) desc
    limit 1
) payment on true
where sc.code = 'promotion'
  and so.payment_status = 'CONFIRMED'
  and not exists (
      select 1
      from marketplace_publications mp
      where mp.service_order_id = so.id
  );
