-- =====================================================================
-- 누락된 외래키(FK) 제약 추가 - 같은 아그리게이트(도메인 모듈) 내부에 한함
--
-- 원칙:
--   * 같은 아그리게이트(도메인 모듈) 내부 참조만 FK 로 강제한다.
--   * 다른 아그리게이트를 가리키는 참조(user_id, media_id 등)는
--     모듈 격리를 위해 ID 참조로만 두고 FK 를 걸지 않는다.
--     (참조 무결성/고아 정리는 애플리케이션 레벨에서 처리)
--
-- 삭제 정책(아그리게이트 내부):
--   * 소유/종속 관계 (부모 없이 존재 의미 없음)   -> ON DELETE CASCADE
--   * 마스터/참조 데이터 (brand, term)            -> RESTRICT(기본값)
--
-- 주의: 기존 환경에 참조 무결성을 위반하는 고아 데이터가 있으면
--       ADD CONSTRAINT 가 실패한다. 실패 시 해당 고아 행을 먼저 정리해야 한다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- map 아그리게이트
-- ---------------------------------------------------------------------
-- 위치는 브랜드(마스터)를 참조 -> RESTRICT
ALTER TABLE TB_PHOTO_BOOTH_LOCATION
    ADD CONSTRAINT fk_photo_booth_location_brand FOREIGN KEY (brand_id) REFERENCES TB_BRAND (id);

-- 즐겨찾기는 위치에 종속 -> 위치 삭제 시 정리
ALTER TABLE TB_FAVORITE_MAP
    ADD CONSTRAINT fk_favorite_map_location FOREIGN KEY (location_id) REFERENCES TB_PHOTO_BOOTH_LOCATION (id) ON DELETE CASCADE;

-- 브랜드 정렬 순서는 브랜드에 종속 -> 브랜드 삭제 시 정리
ALTER TABLE TB_USER_BRAND_ORDER
    ADD CONSTRAINT fk_user_brand_order_brand FOREIGN KEY (brand_id) REFERENCES TB_BRAND (id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------
-- photo 아그리게이트
-- ---------------------------------------------------------------------
-- 즐겨찾기 이미지는 사진에 종속 -> 사진 삭제 시 정리
ALTER TABLE TB_FAVORITE_IMAGE
    ADD CONSTRAINT fk_favorite_image_photo_image FOREIGN KEY (image_id) REFERENCES TB_PHOTO_IMAGE (id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------
-- pose 아그리게이트
-- ---------------------------------------------------------------------
-- 스크랩은 포즈에 종속 -> 포즈 삭제 시 정리
ALTER TABLE TB_SCRAP_POSE
    ADD CONSTRAINT fk_scrap_pose_pose FOREIGN KEY (pose_id) REFERENCES TB_POSE (id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------
-- support 아그리게이트
-- ---------------------------------------------------------------------
-- 약관 동의/이력은 약관(마스터)을 참조 -> RESTRICT
ALTER TABLE TB_USER_TERM_AGREEMENT
    ADD CONSTRAINT fk_user_term_agreement_term FOREIGN KEY (term_id) REFERENCES TB_TERM (id);

ALTER TABLE TB_USER_TERM_AGREEMENT_HIST
    ADD CONSTRAINT fk_user_term_agreement_hist_term FOREIGN KEY (term_id) REFERENCES TB_TERM (id);

-- ---------------------------------------------------------------------
-- FK 컬럼 인덱스 (PostgreSQL 은 FK 에 인덱스를 자동 생성하지 않음)
--   CASCADE 삭제 및 조인/조회 성능용. 복합 PK 선두 컬럼은 PK 인덱스로 커버되어 제외.
-- ---------------------------------------------------------------------
CREATE INDEX idx_photo_booth_location_brand_id ON TB_PHOTO_BOOTH_LOCATION (brand_id);
CREATE INDEX idx_favorite_map_location_id ON TB_FAVORITE_MAP (location_id);
CREATE INDEX idx_user_brand_order_brand_id ON TB_USER_BRAND_ORDER (brand_id);
CREATE INDEX idx_favorite_image_image_id ON TB_FAVORITE_IMAGE (image_id);
CREATE INDEX idx_scrap_pose_pose_id ON TB_SCRAP_POSE (pose_id);
