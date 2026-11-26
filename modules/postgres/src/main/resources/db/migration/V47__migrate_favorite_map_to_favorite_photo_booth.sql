-- 지도 V1 API 제거 (앱 강제 업데이트로 V2 만 쓴다). 서버는 이제 TB_FAVORITE_PHOTO_BOOTH 만 읽고 쓴다.
-- 기존 TB_FAVORITE_MAP(지점 마스터 id) 즐겨찾기를 원천 키 (source_platform, source_idx) 로 바꿔 TB_FAVORITE_PHOTO_BOOTH 로 옮긴다.
--
-- * 원천 키가 없는 위치(source_type LEGACY/MANUAL)는 포토부스 키로 가리킬 수 없어 옮기지 못한다. 이 즐겨찾기는 V2 에서도 원래 보이지 않았다
-- * 이미 V2 에서 같은 부스를 즐겨찾기했으면 더 최근 시각을 남긴다 (서버가 두 테이블을 합칠 때와 같은 규칙)
-- * 지금 세대(enriched)에 없는 부스도 그대로 옮긴다. 조회에서 빠지는 것은 V2 와 같다
-- * 다시 돌려도 결과가 같다 (롤링 배포 중 기존 파드가 V1 으로 쓴 행은 같은 SQL 을 수동으로 다시 돌려 옮긴다)
--
-- TB_FAVORITE_MAP 은 지우지 않고 백업으로 남긴다. 옮긴 결과를 운영에서 확인한 뒤 다음 마이그레이션에서 DROP 한다.
INSERT INTO TB_FAVORITE_PHOTO_BOOTH (user_id, platform, idx, created_at)
SELECT fm.user_id, l.source_platform, l.source_idx, MAX(fm.created_at)
FROM TB_FAVORITE_MAP fm
         JOIN TB_PHOTO_BOOTH_LOCATION l ON l.id = fm.location_id
WHERE l.source_platform IS NOT NULL
  AND l.source_idx IS NOT NULL
GROUP BY fm.user_id, l.source_platform, l.source_idx
ON CONFLICT (user_id, platform, idx) DO UPDATE
    SET created_at = GREATEST(TB_FAVORITE_PHOTO_BOOTH.created_at, EXCLUDED.created_at);

COMMENT ON TABLE TB_FAVORITE_PHOTO_BOOTH IS '포토부스 즐겨찾기 (원천 키 기준). V47 에서 TB_FAVORITE_MAP 을 옮겨 왔다';
COMMENT ON TABLE TB_FAVORITE_MAP IS '[사용 중단] 지도 V1 즐겨찾기 (지점 마스터 id 기준). V47 에서 TB_FAVORITE_PHOTO_BOOTH 로 옮겼고 서버는 더 이상 읽고 쓰지 않는다. 확인 후 DROP 예정';
