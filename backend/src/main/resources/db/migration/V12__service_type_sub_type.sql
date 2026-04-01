CREATE TABLE service_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE service_sub_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_type_id UUID NOT NULL REFERENCES service_types(id),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE services ADD COLUMN service_type_id UUID REFERENCES service_types(id);
ALTER TABLE services ADD COLUMN service_sub_type_id UUID REFERENCES service_sub_types(id);

CREATE INDEX idx_services_service_type_id ON services(service_type_id);
CREATE INDEX idx_services_service_sub_type_id ON services(service_sub_type_id);
CREATE INDEX idx_service_sub_types_service_type_id ON service_sub_types(service_type_id);

INSERT INTO service_types (code, name, sort_order) VALUES
    ('logistics', '物流', 1),
    ('certification', '认证', 2),
    ('consulting', '咨询', 3),
    ('finance', '金融', 4),
    ('marketing', '营销', 5),
    ('translation', '翻译', 6);

INSERT INTO service_sub_types (service_type_id, code, name, sort_order) VALUES
    ((SELECT id FROM service_types WHERE code = 'logistics'), 'international_freight', '国际货运', 1),
    ((SELECT id FROM service_types WHERE code = 'logistics'), 'customs_clearance', '报关清关', 2),
    ((SELECT id FROM service_types WHERE code = 'logistics'), 'warehousing', '仓储配送', 3),
    ((SELECT id FROM service_types WHERE code = 'logistics'), 'cross_border_tracking', '跨境物流追踪', 4),
    ((SELECT id FROM service_types WHERE code = 'certification'), 'quality_system', '质量体系认证', 1),
    ((SELECT id FROM service_types WHERE code = 'certification'), 'product_safety', '产品安全认证', 2),
    ((SELECT id FROM service_types WHERE code = 'certification'), 'carbon_neutral', '碳中和', 3),
    ((SELECT id FROM service_types WHERE code = 'certification'), 'export_compliance', '出口合规认证', 4),
    ((SELECT id FROM service_types WHERE code = 'consulting'), 'market_access', '市场准入咨询', 1),
    ((SELECT id FROM service_types WHERE code = 'consulting'), 'legal_compliance', '法律合规咨询', 2),
    ((SELECT id FROM service_types WHERE code = 'consulting'), 'ip_consulting', '知识产权咨询', 3),
    ((SELECT id FROM service_types WHERE code = 'consulting'), 'tax_planning', '税务筹划', 4),
    ((SELECT id FROM service_types WHERE code = 'finance'), 'export_credit_insurance', '出口信用保险', 1),
    ((SELECT id FROM service_types WHERE code = 'finance'), 'cross_border_payment', '跨境支付', 2),
    ((SELECT id FROM service_types WHERE code = 'finance'), 'trade_finance', '贸易融资', 3),
    ((SELECT id FROM service_types WHERE code = 'marketing'), 'overseas_promotion', '海外推广', 1),
    ((SELECT id FROM service_types WHERE code = 'marketing'), 'exhibition', '展会服务', 2),
    ((SELECT id FROM service_types WHERE code = 'marketing'), 'digital_marketing', '数字营销', 3),
    ((SELECT id FROM service_types WHERE code = 'marketing'), 'brand_localization', '品牌本地化', 4),
    ((SELECT id FROM service_types WHERE code = 'translation'), 'technical_translation', '技术文档翻译', 1),
    ((SELECT id FROM service_types WHERE code = 'translation'), 'contract_translation', '合同翻译', 2),
    ((SELECT id FROM service_types WHERE code = 'translation'), 'localization', '本地化服务', 3);
