-- V35의 운영 초기 데이터는 테스트 간 상태를 오염시키므로 스키마만 남긴다.
DO $$
BEGIN
    IF to_regclass('tb_photo_booth_manual') IS NOT NULL THEN
        DELETE FROM TB_PHOTO_BOOTH_MANUAL;
    END IF;
END $$;

DELETE FROM TB_BRAND WHERE code IN ('DONT_LXXK_UP', 'MONO_MANSION');
