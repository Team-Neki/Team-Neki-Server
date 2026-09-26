-- BACKEND-153: 지점 마스터는 ID를 유지하고 원천 키로 증분 갱신한다.
-- 기존 카카오 지점은 LEGACY로 보존한다. 즐겨찾기 이관이나 기존 행 삭제는 하지 않는다.
ALTER TABLE TB_PHOTO_BOOTH_LOCATION
    ALTER COLUMN map_id TYPE VARCHAR(128),
    ALTER COLUMN branch_name TYPE VARCHAR(255),
    ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN source_platform VARCHAR(32),
    ADD COLUMN source_idx VARCHAR(64),
    ADD COLUMN source_name VARCHAR(255),
    ADD COLUMN source_address VARCHAR(255),
    ADD COLUMN source_location geometry(Point, 4326),
    ADD COLUMN source_b_code CHAR(10),
    ADD COLUMN source_dt DATE,
    ADD COLUMN source_collected_at TIMESTAMP,
    ADD COLUMN override_branch_name VARCHAR(255),
    ADD COLUMN override_address VARCHAR(255),
    ADD COLUMN override_location geometry(Point, 4326),
    ADD COLUMN override_b_code CHAR(10),
    ADD COLUMN b_code CHAR(10),
    ADD COLUMN admin_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_photo_booth_location_source CHECK (
        (source_type = 'COLLECTED' AND source_platform IS NOT NULL AND source_idx IS NOT NULL
            AND source_name IS NOT NULL AND source_address IS NOT NULL
            AND source_location IS NOT NULL AND source_dt IS NOT NULL
            AND source_collected_at IS NOT NULL)
        OR (source_type IN ('LEGACY', 'MANUAL') AND source_platform IS NULL AND source_idx IS NULL)
    ),
    ADD CONSTRAINT uq_photo_booth_location_source UNIQUE (source_platform, source_idx);

COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_type IS 'LEGACY 기존 카카오 지점 / COLLECTED 배치 수집 / MANUAL 관리자 등록';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_platform IS '수집 platform. source_idx와 함께 원천 식별자';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_idx IS '수집 사이트의 지점 ID. 삭제 후 재생성하지 않음';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_name IS '수집 지점명 원문. 관리자 보정과 분리';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_address IS '수집 주소 원문';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_location IS '수집 좌표 (SRID 4326)';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_b_code IS '수집 좌표의 법정동 코드. 미확정이면 NULL';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_dt IS '수집 사이클 날짜';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.source_collected_at IS '수집 시각 (KST 벽시계). 오래된 입력 차단 기준';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.override_branch_name IS '관리자 지점명 보정. NULL이면 source_name 사용';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.override_address IS '관리자 주소 보정. NULL이면 source_address 사용';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.override_location IS '관리자 좌표 보정. NULL이면 source_location 사용';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.override_b_code IS '관리자 법정동 보정. 좌표 보정 시 함께 지정';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.b_code IS '조회와 검색 색인이 사용하는 최종 법정동 코드';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.admin_hidden IS '관리자 노출 중지. 배치가 변경하지 않음';
COMMENT ON COLUMN TB_PHOTO_BOOTH_LOCATION.map_id IS 'LEGACY는 카카오 장소 ID, COLLECTED는 source:<platform>:<idx>. 원천 매칭은 source 컬럼 사용';
