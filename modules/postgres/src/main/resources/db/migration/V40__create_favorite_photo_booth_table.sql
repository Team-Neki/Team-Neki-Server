-- 포토부스 즐겨찾기를 지점 마스터 id(TB_FAVORITE_MAP.location_id) 대신 tb_photo_booth_enriched 의 원천 키 (platform, idx) 로 남긴다.
-- enriched 는 Workflow 가 매일 이름 교체로 새 세대를 만들어 행 id 가 없으므로, 세대를 넘어 안정적인 원천 키로 가리킨다.
-- platform 은 브랜드 코드(tb_brand.code, V38)이고, 앱에는 지점 마스터 map_id 와 같은 source:<platform>:<idx> 로 내린다.
--
-- 기존 TB_FAVORITE_MAP 은 옮기지 않고 남긴다. 서버가 두 테이블을 합쳐 읽고, 해제할 때 양쪽에서 함께 지운다.
-- 지점 마스터 위치와는 (source_platform, source_idx) = (platform, idx) 로 짝짓는다.
-- enriched 는 Workflow 소유이고 세대마다 테이블이 바뀌므로 FK 를 걸지 않는다. 다른 아그리게이트인 user_id 도 V27 원칙대로 FK 없이 둔다.
CREATE TABLE TB_FAVORITE_PHOTO_BOOTH
(
    user_id    BIGINT      NOT NULL,
    platform   VARCHAR(32) NOT NULL,
    idx        VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL,

    PRIMARY KEY (user_id, platform, idx)
);

COMMENT ON TABLE TB_FAVORITE_PHOTO_BOOTH IS '포토부스 즐겨찾기 (원천 키 기준). 기존 TB_FAVORITE_MAP 과 합쳐서 읽는다';
COMMENT ON COLUMN TB_FAVORITE_PHOTO_BOOTH.user_id IS '사용자 ID';
COMMENT ON COLUMN TB_FAVORITE_PHOTO_BOOTH.platform IS '브랜드 코드 (tb_brand.code, tb_photo_booth_enriched.platform)';
COMMENT ON COLUMN TB_FAVORITE_PHOTO_BOOTH.idx IS '지점 ID (tb_photo_booth_enriched.idx)';
COMMENT ON COLUMN TB_FAVORITE_PHOTO_BOOTH.created_at IS '즐겨찾기한 일시';

COMMENT ON TABLE TB_FAVORITE_MAP IS '지도 즐겨찾기 테이블 (지점 마스터 id 기준). 새 즐겨찾기는 TB_FAVORITE_PHOTO_BOOTH 에 쌓고, 원천 키가 없는 지점만 여기에 쌓는다';
