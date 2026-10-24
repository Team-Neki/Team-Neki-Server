-- Workflow가 운영에서 적재하는 읽기 전용 참조 테이블을 빈 테스트 DB에만 준비한다.
CREATE TABLE tb_photo_booth_enriched
(
    platform  VARCHAR(32) NOT NULL,
    idx       VARCHAR(64) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    address   VARCHAR(255),
    location  geometry(Point, 4326),
    -- 이 PR 브랜치의 구형 매핑도 로컬에서 실행할 수 있도록 남긴다. main은 location을 사용한다.
    longitude DOUBLE PRECISION,
    latitude  DOUBLE PRECISION,
    source_dt DATE NOT NULL,
    b_code    CHAR(10),
    PRIMARY KEY (platform, idx)
);

CREATE TABLE tb_subway_station
(
    name      VARCHAR(60) NOT NULL,
    line_name VARCHAR(40) NOT NULL,
    location  geometry(Point, 4326) NOT NULL,
    PRIMARY KEY (name, line_name)
);

CREATE TABLE tb_legal_dong
(
    code      CHAR(10) PRIMARY KEY,
    level     SMALLINT NOT NULL,
    leaf_name VARCHAR(30) NOT NULL,
    full_name VARCHAR(60) NOT NULL
);
