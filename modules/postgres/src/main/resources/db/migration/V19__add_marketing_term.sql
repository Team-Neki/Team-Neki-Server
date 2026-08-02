-- Add marketing consent term
INSERT INTO TB_TERM (id, term_type, title, url, version, is_required, is_active, display_order, created_at, updated_at)
VALUES (4, 'MARKETING', '마케팅 수신 동의', 'https://lydian-tip-26b.notion.site/3790d9441db080e8a134debf260caea1', '1.0.0', false, true, 4, NOW(), NOW());