-- BACKEND-222: 관리자 등록 지점(TB_PHOTO_BOOTH_MANUAL)도 stores-sync 가 지점 마스터에 MANUAL 로 동기화한다.
-- enrich 가 관리자 지점을 (tb_brand.platform, 'manual-<id>') 원천 키로 담으므로, 같은 키로 upsert 해
-- 검색 카드와 지도 지점을 (platform, idx) 로 짝지을 수 있게 한다.
-- V34 제약은 MANUAL 에 원천 키를 허용하지 않으므로 바꾼다. 원천 키 없는 기존 MANUAL(어드민 직접 입력)도 계속 허용한다.
-- 제약 이름을 바꿔 stores-sync 가 이 마이그레이션 적용 여부를 확인할 수 있게 한다.
ALTER TABLE TB_PHOTO_BOOTH_LOCATION
    DROP CONSTRAINT ck_photo_booth_location_source,
    ADD CONSTRAINT ck_photo_booth_location_source_keyed CHECK (
        (source_type IN ('COLLECTED', 'MANUAL') AND source_platform IS NOT NULL AND source_idx IS NOT NULL
            AND source_name IS NOT NULL AND source_address IS NOT NULL
            AND source_location IS NOT NULL AND source_dt IS NOT NULL
            AND source_collected_at IS NOT NULL)
        OR (source_type IN ('LEGACY', 'MANUAL') AND source_platform IS NULL AND source_idx IS NULL)
    );

COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_type IS 'LEGACY 기존 카카오 지점 / COLLECTED 배치 수집 / MANUAL 관리자 등록 (TB_PHOTO_BOOTH_MANUAL 에서 동기화되면 원천 키 manual-<id> 를 가짐)';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_idx IS '수집 사이트의 지점 ID. 관리자 등록 지점은 manual-<TB_PHOTO_BOOTH_MANUAL.id>. 삭제 후 재생성하지 않음';
