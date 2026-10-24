-- V35의 수동 지점 초기 데이터가 전제하는 운영 브랜드를 빈 테스트 DB에만 준비한다.
INSERT INTO TB_BRAND (name, code, platform, created_at, updated_at)
VALUES ('돈룩업', 'DONT_LXXK_UP', 'DONT_LXXK_UP', now(), now()),
       ('모노맨션', 'MONO_MANSION', 'MONO_MANSION', now(), now());
