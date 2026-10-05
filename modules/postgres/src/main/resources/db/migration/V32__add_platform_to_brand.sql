-- 검색 색인이 tb_photo_booth_enriched.platform(Workflow 의 Platform 값)으로 브랜드를 찾을 수 있도록
-- TB_BRAND 에 platform 컬럼을 둔다. Workflow 의 Platform 값과 code 가 다르므로(LIFE_FOUR_CUT vs LIFEFOURCUTS) 별도 컬럼이다.
-- 수집하지 않는 브랜드는 NULL 이고, 색인 잡은 NULL 인 브랜드를 건너뛴다.
ALTER TABLE TB_BRAND ADD COLUMN platform VARCHAR(32) NULL;

COMMENT ON COLUMN TB_BRAND.platform IS 'Workflow Platform 값 (검색 색인 조인 키, 수집하지 않는 브랜드는 NULL)';

-- soft delete 된 브랜드가 platform 을 영구 점유하지 않도록 살아있는 행에만 unique 를 건다 (V29 와 같은 방식)
CREATE UNIQUE INDEX uk_brand_platform ON TB_BRAND (platform) WHERE deleted_at IS NULL;

-- V3 가 넣은 브랜드 6개의 platform 을 채운다
UPDATE TB_BRAND SET platform = 'PHOTOISM'        WHERE deleted_at IS NULL AND code = 'PHOTOISM';
UPDATE TB_BRAND SET platform = 'PLANB_STUDIO'    WHERE deleted_at IS NULL AND code = 'PLANB_STUDIO';
UPDATE TB_BRAND SET platform = 'LIFE_FOUR_CUT'   WHERE deleted_at IS NULL AND code = 'LIFEFOURCUTS';
UPDATE TB_BRAND SET platform = 'PHOTO_GRAY'      WHERE deleted_at IS NULL AND code = 'PHOTOGRAY';
UPDATE TB_BRAND SET platform = 'PHOTO_SIGNATURE' WHERE deleted_at IS NULL AND code = 'PHOTOSIGNATURE';
UPDATE TB_BRAND SET platform = 'HARU_FILM'       WHERE deleted_at IS NULL AND code = 'HARUFILM';

-- 나머지 5개는 마이그레이션이 아니라 운영 데이터로 들어온 브랜드라 code 가 환경마다 다를 수 있다.
-- 아래 code 는 staging tb_brand 의 값(2026-09-25)이다. 맞는 행이 없으면 0건 갱신이고 잡이 경고 후 건너뛴다.
UPDATE TB_BRAND SET platform = 'BROOM_STUDIO'    WHERE deleted_at IS NULL AND code = 'BROOM_STUDIO';
UPDATE TB_BRAND SET platform = 'DONT_LXXK_UP'    WHERE deleted_at IS NULL AND code = 'DONT_LXXK_UP';
UPDATE TB_BRAND SET platform = 'MONO_MANSION'    WHERE deleted_at IS NULL AND code = 'MONO_MANSION';
UPDATE TB_BRAND SET platform = 'PHOTO_LAB_PLUS'  WHERE deleted_at IS NULL AND code = 'PHOTOLAB_PLUS';
UPDATE TB_BRAND SET platform = 'PICDOT'          WHERE deleted_at IS NULL AND code = 'PIC_DOT';
