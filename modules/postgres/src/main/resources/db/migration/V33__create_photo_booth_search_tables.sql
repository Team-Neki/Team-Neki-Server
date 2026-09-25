-- 검색 카드 테이블. searchIndexJob 이 tb_photo_booth_enriched 에서 전량 재생성한다 (BACKEND-65)
CREATE TABLE tb_photo_booth_search
(
    id                     BIGSERIAL PRIMARY KEY,
    platform               VARCHAR(32)   NOT NULL,
    idx                    VARCHAR(64)   NOT NULL,
    brand_id               BIGINT        NOT NULL,
    brand_name             VARCHAR(100)  NOT NULL,
    brand_code             VARCHAR(50)   NOT NULL,
    branch_name            VARCHAR(255)  NOT NULL,
    address                VARCHAR(255),
    location               geometry(Point, 4326) NOT NULL,
    normalized_brand_name  VARCHAR(100)  NOT NULL,
    normalized_branch_name VARCHAR(255)  NOT NULL,
    search_text            TEXT          NOT NULL,
    region_ids             VARCHAR(10)[] NOT NULL DEFAULT '{}',
    site_key               VARCHAR(40)   NOT NULL,
    source_dt              DATE          NOT NULL,
    business_date          DATE          NOT NULL,
    indexed_at             TIMESTAMP     NOT NULL,
    CONSTRAINT uq_photo_booth_search_platform_idx UNIQUE (platform, idx)
);

CREATE INDEX ix_photo_booth_search_region_ids ON tb_photo_booth_search USING GIN (region_ids);
CREATE INDEX ix_photo_booth_search_location   ON tb_photo_booth_search USING GIST (location);
CREATE INDEX ix_photo_booth_search_site_key   ON tb_photo_booth_search (site_key);

COMMENT ON TABLE tb_photo_booth_search IS '포토부스 검색 카드 테이블 (색인 잡이 전량 재생성)';
COMMENT ON COLUMN tb_photo_booth_search.id IS '검색 카드 고유 ID';
COMMENT ON COLUMN tb_photo_booth_search.platform IS 'Workflow Platform 값 (tb_brand.platform 과 조인)';
COMMENT ON COLUMN tb_photo_booth_search.idx IS '원천 tb_photo_booth_enriched 의 지점 식별자';
COMMENT ON COLUMN tb_photo_booth_search.brand_id IS '브랜드 ID (TB_BRAND 참조)';
COMMENT ON COLUMN tb_photo_booth_search.brand_name IS '브랜드 이름 (색인 시점 스냅샷)';
COMMENT ON COLUMN tb_photo_booth_search.brand_code IS '브랜드 코드 (색인 시점 스냅샷)';
COMMENT ON COLUMN tb_photo_booth_search.branch_name IS '지점명 (원천 name 에서 브랜드명 접두를 뗀 값)';
COMMENT ON COLUMN tb_photo_booth_search.address IS '주소';
COMMENT ON COLUMN tb_photo_booth_search.location IS '위치 좌표 (SRID 4326, 경도/위도 순서)';
COMMENT ON COLUMN tb_photo_booth_search.normalized_brand_name IS '정규화한 브랜드명 (소문자, 공백/구분자 제거)';
COMMENT ON COLUMN tb_photo_booth_search.normalized_branch_name IS '정규화한 지점명 (소문자, 공백/구분자 제거)';
COMMENT ON COLUMN tb_photo_booth_search.search_text IS '정규화한 브랜드명+지점명+주소 (부분 일치 검색용)';
COMMENT ON COLUMN tb_photo_booth_search.region_ids IS '법정동코드 10자리에서 뽑은 시도/시군구/읍면동/법정동 코드 목록';
COMMENT ON COLUMN tb_photo_booth_search.site_key IS '같은 자리 지점 묶음 키 (<법정동코드>:<경도 5자리>,<위도 5자리>)';
COMMENT ON COLUMN tb_photo_booth_search.source_dt IS '원천 데이터 수집일';
COMMENT ON COLUMN tb_photo_booth_search.business_date IS '색인 잡의 기준일 (businessDate 파라미터)';
COMMENT ON COLUMN tb_photo_booth_search.indexed_at IS '색인 생성 시각';

-- 카드와 1km 안 지하철역 연결 테이블. 카드가 지워지면 함께 지워진다
CREATE TABLE tb_photo_booth_search_station
(
    search_id    BIGINT      NOT NULL REFERENCES tb_photo_booth_search (id) ON DELETE CASCADE,
    station_name VARCHAR(60) NOT NULL,
    line_name    VARCHAR(40) NOT NULL,
    distance_m   INTEGER     NOT NULL,
    PRIMARY KEY (search_id, station_name, line_name)
);

CREATE INDEX ix_photo_booth_search_station_station ON tb_photo_booth_search_station (station_name, line_name);

COMMENT ON TABLE tb_photo_booth_search_station IS '검색 카드와 1km 안 지하철역 연결 테이블';
COMMENT ON COLUMN tb_photo_booth_search_station.search_id IS '검색 카드 ID (tb_photo_booth_search 참조)';
COMMENT ON COLUMN tb_photo_booth_search_station.station_name IS '역명 (tb_subway_station.name, 역 접미사 없음)';
COMMENT ON COLUMN tb_photo_booth_search_station.line_name IS '노선명 (tb_subway_station.line_name)';
COMMENT ON COLUMN tb_photo_booth_search_station.distance_m IS '지점과 역 사이 거리 (m, haversine)';
