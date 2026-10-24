-- Workflow가 소유하는 조회 테이블의 서버 계약 7열만 테스트 DB에 만든다.
CREATE TABLE tb_photo_booth_enriched
(
    platform  VARCHAR(32) NOT NULL,
    idx       VARCHAR(64) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    address   VARCHAR(255),
    location  geometry(Point, 4326),
    source_dt DATE NOT NULL,
    b_code    CHAR(10),
    PRIMARY KEY (platform, idx)
);
