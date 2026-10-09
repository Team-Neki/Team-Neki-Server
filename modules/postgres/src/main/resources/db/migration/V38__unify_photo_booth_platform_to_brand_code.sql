-- BACKEND-228: 지점 원천 키의 platform 을 Workflow Platform 값(LIFE_FOUR_CUT 등)에서 tb_brand.code(LIFEFOURCUTS 등)로 바꾼다.
-- V32 가 두 값을 잇던 tb_brand.platform 은 매핑이 필요 없어지므로 지운다.
-- 옛 값 -> code 매핑은 tb_brand.platform 자체에서 읽으므로 환경마다 code 가 달라도 그 환경의 code 로 바뀐다.
-- Workflow(collect/enrich/stores-sync)와 서버 색인 배치를 멈춘 상태에서 실행하고, Platform 값을 code 로 바꾼 Workflow 를 함께 배포해야 한다.
-- Workflow 를 먼저 다시 돌리면 stores-sync 가 옛 원천 키로 지점을 새로 만든다.

CREATE TEMP TABLE platform_to_code AS
SELECT platform AS old_platform, code AS new_platform
FROM TB_BRAND
WHERE deleted_at IS NULL AND platform IS NOT NULL AND platform <> code;

-- 1. 지점 마스터: 원천 키와 그것으로 만든 map_id (source:<platform>:<idx>)
UPDATE TB_PHOTO_BOOTH_LOCATION l
SET source_platform = m.new_platform,
    map_id          = 'source:' || m.new_platform || ':' || l.source_idx
FROM platform_to_code m
WHERE l.source_platform = m.old_platform;

-- 2. 검색 색인 (read/write 는 색인 배치가 이름을 맞바꾸며 쓰므로 둘 다)
UPDATE tb_photo_booth_search_read s
SET platform = m.new_platform
FROM platform_to_code m
WHERE s.platform = m.old_platform;

UPDATE tb_photo_booth_search_write s
SET platform = m.new_platform
FROM platform_to_code m
WHERE s.platform = m.old_platform;

-- 3. Workflow 가 만드는 테이블. 스키마는 Workflow 소유라 Flyway 가 모르는 환경(로컬 등)에서는 없을 수 있다.
--    enriched 의 직전 세대(_prev)와 수집 manifest 도 같은 값을 쓰므로 함께 바꾼다.
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['tb_photo_booth_enriched', 'tb_photo_booth_enriched_prev', 'tb_store_collect_manifest'] LOOP
        IF to_regclass(t) IS NOT NULL THEN
            EXECUTE format(
                'UPDATE %I x SET platform = m.new_platform FROM platform_to_code m WHERE x.platform = m.old_platform', t);
        END IF;
    END LOOP;
END $$;

DROP TABLE platform_to_code;

-- 4. 매핑 컬럼 제거 (uk_brand_platform 도 함께 지워진다)
ALTER TABLE TB_BRAND DROP COLUMN platform;

COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_platform IS '수집 원천 브랜드 코드 (tb_brand.code). source_idx와 함께 원천 식별자';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.map_id IS 'COLLECTED/MANUAL 은 source:<brand code>:<idx>. 원천 매칭은 source 컬럼 사용';
COMMENT ON COLUMN tb_photo_booth_search_read.platform IS '브랜드 코드 (tb_brand.code 와 조인)';
COMMENT ON COLUMN tb_photo_booth_search_write.platform IS '브랜드 코드 (tb_brand.code 와 조인)';
